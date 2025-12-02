from __future__ import annotations

from __future__ import annotations

import json
from typing import Any, List, Tuple

from django.contrib.auth.mixins import LoginRequiredMixin
from django.core.exceptions import PermissionDenied as DjangoPermissionDenied
from django.db import IntegrityError
from django.db.models import QuerySet
from django.template.loader import render_to_string
from django.urls import reverse
from django.views import View
from rest_framework import status, viewsets
from rest_framework.exceptions import PermissionDenied, ValidationError
from rest_framework.permissions import BasePermission
from rest_framework.request import Request
from rest_framework.response import Response
from rest_framework.views import APIView

from dst.models import CustomField, Tag, User, UserRole
from dst.serializers import CustomFieldSerializer
from dst.utils import DstHttpRequest, render_main_template


class RequireOrgAdmin(BasePermission):
    """Allows access only to users with org-level admin privileges."""

    def has_permission(self, request, view):  # type: ignore[override]
        user = request.user
        if not isinstance(user, User) or not user.is_authenticated:
            return False
        return user.hasRole(UserRole.ALL_ACCESS) or user.is_superuser


class CustomFieldViewSet(viewsets.ModelViewSet):
    """CRUD operations for organization-specific custom field definitions."""

    serializer_class = CustomFieldSerializer
    permission_classes = [RequireOrgAdmin]

    def get_queryset(self) -> QuerySet[CustomField]:
        org = self._get_request_org()
        queryset = CustomField.objects.filter(organization=org)
        entity_type = self.request.query_params.get(
            "entity_type", CustomField.EntityType.PERSON
        )
        queryset = queryset.filter(entity_type=entity_type)
        return queryset.order_by("display_order", "label")

    def perform_create(self, serializer: CustomFieldSerializer) -> None:
        try:
            serializer.save(organization=self._get_request_org())
        except IntegrityError as exc:  # pragma: no cover - path asserted in tests
            raise ValidationError(
                {"label": "A field with this label already exists for the entity."}
            ) from exc

    def perform_update(self, serializer: CustomFieldSerializer) -> None:
        instance = self.get_object()
        try:
            serializer.save(organization=instance.organization)
        except IntegrityError as exc:
            raise ValidationError(
                {"label": "A field with this label already exists for the entity."}
            ) from exc

    def destroy(self, request: Request, *args: Any, **kwargs: Any) -> Response:
        instance = self.get_object()
        if instance.values.exists():
            instance.enabled = False
            instance.disabled = True
            instance.save(update_fields=["enabled", "disabled", "date_updated"])
            serializer = self.get_serializer(instance)
            return Response(serializer.data, status=status.HTTP_200_OK)

        return super().destroy(request, *args, **kwargs)

    def _get_request_org(self):
        org = getattr(self.request, "org", None)
        if org:
            return org
        user = self.request.user
        if isinstance(user, User) and user.organization:
            return user.organization
        raise PermissionDenied("Unable to determine organization context.")


class RoleKeyListView(APIView):
    """Expose the available UserRole keys for client-side filtering."""

    permission_classes = [RequireOrgAdmin]

    ROLE_DEFINITIONS: List[Tuple[str, str]] = [
        (UserRole.ALL_ACCESS, "All Access"),
        (UserRole.ACCOUNTING, "Accounting"),
        (UserRole.ATTENDANCE, "Attendance"),
        (UserRole.ROLES, "Manage Roles"),
        (UserRole.VIEW_JC, "View JC"),
        (UserRole.EDIT_MANUAL, "Edit Manual"),
        (UserRole.EDIT_RESOLUTION_PLANS, "Edit Resolution Plans"),
        (UserRole.EDIT_7_DAY_JC, "Edit JC (7 Day)"),
        (UserRole.EDIT_31_DAY_JC, "Edit JC (31 Day)"),
        (UserRole.EDIT_ALL_JC, "Edit JC (All Records)"),
        (UserRole.CHECKIN_APP, "Check-in App"),
    ]

    def get(self, request: Request) -> Response:
        roles = [
            {"id": key, "label": label}
            for key, label in self.ROLE_DEFINITIONS
        ]
        return Response({"roles": roles})


class CustomFieldSettingsView(LoginRequiredMixin, View):
    """Render the React settings shell for managing custom fields."""

    login_url = "/login"

    def get(self, request: DstHttpRequest):
        if not request.user.hasRole(UserRole.ALL_ACCESS):
            raise DjangoPermissionDenied(
                "You don't have privileges to access this item"
            )

        content = render_to_string(
            "custom_fields_settings.html",
            {
                "custom_fields_api_url": reverse("custom-field-list"),
                "role_keys_api_url": reverse("role-key-list"),
                "tags_json": json.dumps(
                    [
                        {"id": tag.id, "label": tag.title}
                        for tag in Tag.objects.filter(
                            organization=request.org
                        ).order_by("title")
                    ]
                ),
                "request": request,
            },
        )
        return render_main_template(
            request,
            "settings",
            content,
            "Custom Fields",
            "settings_custom_fields",
        )
