from __future__ import annotations

import json
from typing import Any

from django.contrib.auth.mixins import LoginRequiredMixin
from django.core.exceptions import PermissionDenied as DjangoPermissionDenied
from django.template.loader import render_to_string
from django.urls import reverse
from django.views import View
from django.db import models as db_models
from rest_framework import status, viewsets
from rest_framework.exceptions import PermissionDenied
from rest_framework.request import Request
from rest_framework.response import Response

from dst.custom_field_utils import read_custom_field_values, write_custom_field_values
from dst.custom_field_views import RequireOrgAdmin
from dst.models import (
    CustomField,
    CustomFieldGroup,
    Person,
    PhoneNumber,
    SavedGridView,
    Tag,
    UserRole,
)
from dst.person_serializers import (
    PersonReadSerializer,
    PersonWriteSerializer,
    SavedGridViewSerializer,
)
from dst.serializers import CustomFieldGroupSerializer, CustomFieldSerializer
from dst.utils import DstHttpRequest, render_main_template

# ---------------------------------------------------------------------------
# DRF API ViewSet
# ---------------------------------------------------------------------------


class PersonViewSet(viewsets.ModelViewSet):
    """CRUD API for organization-scoped person records."""

    permission_classes = [RequireOrgAdmin]

    def get_queryset(self):
        org = self._get_org()
        return (
            Person.objects.filter(organization=org, is_family=False)
            .select_related("family_person")
            .prefetch_related("phonenumber_set", "tags")
            .order_by("last_name", "first_name")
        )

    def get_serializer_class(self):
        if self.action in ("list", "retrieve"):
            return PersonReadSerializer
        return PersonWriteSerializer

    def get_serializer_context(self):
        ctx = super().get_serializer_context()
        org = self._get_org()
        ctx["custom_fields"] = CustomField.objects.filter(
            organization=org,
            entity_type=CustomField.EntityType.PERSON,
            enabled=True,
        )
        return ctx

    def perform_create(self, serializer: PersonWriteSerializer) -> None:
        org = self._get_org()
        phone_data = serializer.validated_data.pop("phone_numbers", [])
        tag_ids = serializer.validated_data.pop("tag_ids", [])
        cf_values = serializer.validated_data.pop("custom_field_values", {})
        family_person_id = serializer.validated_data.pop("family_person_id", None)

        person = serializer.save(
            organization=org,
            family_person_id=family_person_id,
        )
        self._sync_phone_numbers(person, phone_data)
        if tag_ids:
            person.tags.set(tag_ids)
        self._write_cf_values(org, person.id, cf_values)

    def perform_update(self, serializer: PersonWriteSerializer) -> None:
        phone_data = serializer.validated_data.pop("phone_numbers", [])
        tag_ids = serializer.validated_data.pop("tag_ids", [])
        cf_values = serializer.validated_data.pop("custom_field_values", {})
        family_person_id = serializer.validated_data.pop("family_person_id", None)

        person = serializer.save(family_person_id=family_person_id)
        self._sync_phone_numbers(person, phone_data)
        person.tags.set(tag_ids)
        self._write_cf_values(person.organization, person.id, cf_values)

    def retrieve(self, request: Request, *args: Any, **kwargs: Any) -> Response:
        instance = self.get_object()
        serializer = self.get_serializer(instance)
        return Response(serializer.data)

    def create(self, request: Request, *args: Any, **kwargs: Any) -> Response:
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        self.perform_create(serializer)
        # Re-read with the read serializer for the response
        read_serializer = PersonReadSerializer(
            serializer.instance,
            context=self.get_serializer_context(),
        )
        return Response(read_serializer.data, status=status.HTTP_201_CREATED)

    def update(self, request: Request, *args: Any, **kwargs: Any) -> Response:
        partial = kwargs.pop("partial", False)
        instance = self.get_object()
        serializer = self.get_serializer(
            instance, data=request.data, partial=partial
        )
        serializer.is_valid(raise_exception=True)
        self.perform_update(serializer)
        read_serializer = PersonReadSerializer(
            serializer.instance,
            context=self.get_serializer_context(),
        )
        return Response(read_serializer.data)

    def _sync_phone_numbers(
        self, person: Person, phone_data: list[dict]
    ) -> None:
        """Replace all phone numbers with the submitted list."""
        person.phonenumber_set.all().delete()
        for entry in phone_data[:3]:
            number = entry.get("number", "").strip()
            comment = entry.get("comment", "").strip()
            if number or comment:
                PhoneNumber.objects.create(
                    person=person, number=number, comment=comment
                )

    def _write_cf_values(self, org, person_id: int, cf_values: dict) -> None:
        if not cf_values:
            return
        custom_fields = CustomField.objects.filter(
            organization=org,
            entity_type=CustomField.EntityType.PERSON,
            enabled=True,
        )
        cf_by_id = {cf.id: cf for cf in custom_fields}
        write_custom_field_values(person_id, cf_by_id, cf_values)

    def _get_org(self):
        org = getattr(self.request, "org", None)
        if org:
            return org
        user = self.request.user
        if hasattr(user, "organization") and user.organization:
            return user.organization
        from rest_framework.exceptions import PermissionDenied

        raise PermissionDenied("Unable to determine organization context.")


class SavedGridViewViewSet(viewsets.ModelViewSet):
    """CRUD API for saved grid view configurations."""

    serializer_class = SavedGridViewSerializer
    permission_classes = [RequireOrgAdmin]

    def get_queryset(self):
        org = self._get_org()
        entity_type = self.request.query_params.get("entity_type", "person")
        return SavedGridView.objects.filter(
            organization=org,
            entity_type=entity_type,
        ).filter(
            db_models.Q(is_private=False)
            | db_models.Q(created_by=self.request.user)
        )

    def perform_create(self, serializer):
        serializer.save(
            organization=self._get_org(),
            created_by=self.request.user,
        )

    def perform_update(self, serializer):
        if serializer.instance.created_by != self.request.user:
            raise PermissionDenied("You can only edit your own views.")
        serializer.save()

    def perform_destroy(self, instance):
        if instance.created_by != self.request.user:
            raise PermissionDenied("You can only delete your own views.")
        instance.delete()

    def _get_org(self):
        org = getattr(self.request, "org", None)
        if org:
            return org
        user = self.request.user
        if hasattr(user, "organization") and user.organization:
            return user.organization
        raise PermissionDenied("Unable to determine organization context.")


# ---------------------------------------------------------------------------
# Django page views (serve React shells)
# ---------------------------------------------------------------------------


def _get_common_context(request: DstHttpRequest) -> dict[str, Any]:
    """Build shared context data for all person page templates."""
    org = request.org

    custom_fields = CustomField.objects.filter(
        organization=org,
        entity_type=CustomField.EntityType.PERSON,
        enabled=True,
    ).order_by("display_order", "label")

    tag_options = [
        {"id": t.id, "label": t.title}
        for t in Tag.objects.filter(organization=org).order_by("title")
    ]

    people_options = [
        {
            "id": p.id,
            "label": (
                (p.display_name or p.first_name) + " " + p.last_name
            ).strip(),
        }
        for p in Person.objects.filter(organization=org, is_family=False)
        .exclude(first_name="", last_name="", display_name="")
        .only("id", "first_name", "last_name", "display_name")
        .order_by("last_name", "first_name")
    ]

    groups = CustomFieldGroup.objects.filter(
        organization=org,
        entity_type=CustomFieldGroup.EntityType.PERSON,
    ).order_by("display_order", "label")

    return {
        "person_api_url": reverse("person-list"),
        "custom_fields_json": json.dumps(
            CustomFieldSerializer(custom_fields, many=True).data
        ),
        "groups_json": json.dumps(
            CustomFieldGroupSerializer(groups, many=True).data
        ),
        "tags_json": json.dumps(tag_options),
        "people_json": json.dumps(people_options),
        "show_electronic_signin": org.show_electronic_signin,
        "request": request,
    }


def _require_admin(request: DstHttpRequest) -> None:
    if not request.user.hasRole(UserRole.ALL_ACCESS):
        raise DjangoPermissionDenied(
            "You don't have privileges to access this item"
        )


class PersonCreateView(LoginRequiredMixin, View):
    login_url = "/login"

    def get(self, request: DstHttpRequest):
        _require_admin(request)
        ctx = _get_common_context(request)
        ctx["mode"] = "create"
        content = render_to_string("person_create.html", ctx)
        return render_main_template(
            request, "crm", content, "Add a Person", "new_person"
        )


class PersonEditView(LoginRequiredMixin, View):
    login_url = "/login"

    def get(self, request: DstHttpRequest, person_id: int):
        _require_admin(request)
        person = Person.objects.filter(
            id=person_id, organization=request.org
        ).prefetch_related("phonenumber_set", "tags").first()
        if not person:
            raise DjangoPermissionDenied("Person not found.")

        ctx = _get_common_context(request)
        ctx["mode"] = "edit"

        custom_fields = CustomField.objects.filter(
            organization=request.org,
            entity_type=CustomField.EntityType.PERSON,
            enabled=True,
        )
        cf_values = read_custom_field_values(person.id, custom_fields)

        person_data = PersonReadSerializer(
            person, context={"custom_fields": custom_fields}
        ).data
        ctx["person_json"] = json.dumps(person_data)
        ctx["custom_field_values_json"] = json.dumps(cf_values)

        name = person.display_name or person.first_name
        content = render_to_string("person_edit.html", ctx)
        return render_main_template(
            request,
            "crm",
            content,
            f"Editing {name} {person.last_name}",
            None,
        )


class PeopleListView(LoginRequiredMixin, View):
    login_url = "/login"

    def get(self, request: DstHttpRequest):
        _require_admin(request)
        ctx = _get_common_context(request)
        ctx["mode"] = "list"

        # Inject saved grid views (shared + user's private)
        saved_views = SavedGridView.objects.filter(
            organization=request.org,
            entity_type="person",
        ).filter(
            db_models.Q(is_private=False)
            | db_models.Q(created_by=request.user)
        )
        ctx["saved_views_json"] = json.dumps(
            SavedGridViewSerializer(saved_views, many=True).data
        )
        ctx["user_id"] = request.user.id

        content = render_to_string("people_list.html", ctx)
        return render_main_template(
            request, "crm", content, "People", "people_list"
        )


class PersonDetailView(LoginRequiredMixin, View):
    login_url = "/login"

    def get(self, request: DstHttpRequest, person_id: int):
        person = Person.objects.filter(
            id=person_id, organization=request.org
        ).prefetch_related("phonenumber_set", "tags").first()
        if not person:
            raise DjangoPermissionDenied("Person not found.")

        ctx = _get_common_context(request)
        ctx["mode"] = "view"

        custom_fields = CustomField.objects.filter(
            organization=request.org,
            entity_type=CustomField.EntityType.PERSON,
            enabled=True,
        )
        cf_values = read_custom_field_values(person.id, custom_fields)

        person_data = PersonReadSerializer(
            person, context={"custom_fields": custom_fields}
        ).data
        ctx["person_json"] = json.dumps(person_data)
        ctx["custom_field_values_json"] = json.dumps(cf_values)

        # Family members
        family_members = []
        if person.family_person_id:
            siblings = Person.objects.filter(
                organization=request.org,
                family_person_id=person.family_person_id,
                is_family=False,
            ).exclude(id=person.id).only("id", "first_name", "last_name", "display_name")
            family_members = [
                {
                    "id": s.id,
                    "label": (
                        (s.display_name or s.first_name) + " " + s.last_name
                    ).strip(),
                }
                for s in siblings
            ]
        ctx["family_members_json"] = json.dumps(family_members)

        name = person.display_name or person.first_name
        content = render_to_string("person_view.html", ctx)
        return render_main_template(
            request, "people", content, f"{name} {person.last_name}", None
        )
