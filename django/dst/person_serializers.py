from __future__ import annotations

from typing import Any

from rest_framework import serializers

from dst.custom_field_utils import validate_custom_field_value
from dst.models import CustomField, Person, SavedGridView, Tag


class PhoneNumberSerializer(serializers.Serializer):
    id = serializers.IntegerField(required=False)
    number = serializers.CharField(max_length=255, allow_blank=True, required=False)
    comment = serializers.CharField(max_length=255, allow_blank=True, required=False)


class TagReadSerializer(serializers.ModelSerializer):
    class Meta:
        model = Tag
        fields = ["id", "title"]


class PersonReadSerializer(serializers.ModelSerializer):
    """Serializer for GET responses — includes nested phone numbers, tags,
    and custom field values."""

    phone_numbers = serializers.SerializerMethodField()
    tags = TagReadSerializer(many=True, read_only=True)
    custom_field_values = serializers.SerializerMethodField()
    family_person_id = serializers.IntegerField(
        source="family_person.id", allow_null=True, default=None
    )
    family_person_name = serializers.SerializerMethodField()

    class Meta:
        model = Person
        fields = [
            "id",
            "first_name",
            "last_name",
            "display_name",
            "gender",
            "email",
            "dob",
            "approximate_dob",
            "notes",
            "address",
            "city",
            "state",
            "zip",
            "neighborhood",
            "grade",
            "previous_school",
            "school_district",
            "pin",
            "is_family",
            "family_person_id",
            "family_person_name",
            "phone_numbers",
            "tags",
            "custom_field_values",
            "created",
        ]

    def get_family_person_name(self, obj: Person) -> str | None:
        fp = obj.family_person
        if fp is None:
            return None
        name = (fp.display_name or fp.first_name or "").strip()
        last = (fp.last_name or "").strip()
        return f"{name} {last}".strip() or None

    def get_phone_numbers(self, obj: Person) -> list[dict]:
        return [
            {"id": pn.id, "number": pn.number or "", "comment": pn.comment or ""}
            for pn in obj.phonenumber_set.all()
        ]

    def get_custom_field_values(self, obj: Person) -> dict[str, Any]:
        from dst.custom_field_utils import read_custom_field_values

        custom_fields = self.context.get("custom_fields")
        if custom_fields is None:
            return {}
        return read_custom_field_values(obj.id, custom_fields)


class PersonWriteSerializer(serializers.ModelSerializer):
    """Serializer for POST/PUT/PATCH — accepts phone_numbers, tag_ids,
    custom_field_values alongside core fields."""

    dob = serializers.DateField(required=False, allow_null=True, default=None)
    phone_numbers = PhoneNumberSerializer(many=True, required=False, default=list)
    tag_ids = serializers.ListField(
        child=serializers.IntegerField(), required=False, default=list
    )
    custom_field_values = serializers.DictField(required=False, default=dict)
    family_person_id = serializers.IntegerField(
        required=False, allow_null=True, default=None
    )

    class Meta:
        model = Person
        fields = [
            "first_name",
            "last_name",
            "display_name",
            "gender",
            "email",
            "dob",
            "notes",
            "address",
            "city",
            "state",
            "zip",
            "neighborhood",
            "grade",
            "previous_school",
            "school_district",
            "pin",
            "phone_numbers",
            "tag_ids",
            "custom_field_values",
            "family_person_id",
        ]
        extra_kwargs = {
            "display_name": {"required": False, "allow_blank": True},
            "gender": {"required": False, "allow_blank": True},
            "email": {"required": False, "allow_blank": True},
            "dob": {"required": False, "allow_null": True},
            "notes": {"required": False, "allow_blank": True},
            "address": {"required": False, "allow_blank": True},
            "city": {"required": False, "allow_blank": True},
            "state": {"required": False, "allow_blank": True},
            "zip": {"required": False, "allow_blank": True},
            "neighborhood": {"required": False, "allow_blank": True},
            "grade": {"required": False, "allow_blank": True},
            "previous_school": {"required": False, "allow_blank": True},
            "school_district": {"required": False, "allow_blank": True},
            "pin": {"required": False, "allow_blank": True},
        }

    def validate_dob(self, value):
        if value == "":
            return None
        return value

    def validate_phone_numbers(self, value: list[dict]) -> list[dict]:
        if len(value) > 3:
            raise serializers.ValidationError("Maximum 3 phone numbers allowed.")
        return value

    def validate_tag_ids(self, value: list[int]) -> list[int]:
        org = self._get_org()
        if not org:
            return value
        valid_ids = set(
            Tag.objects.filter(organization=org).values_list("id", flat=True)
        )
        invalid = [tid for tid in value if tid not in valid_ids]
        if invalid:
            raise serializers.ValidationError(
                f"Invalid tag IDs for this organization: {invalid}"
            )
        return value

    def validate_family_person_id(self, value: int | None) -> int | None:
        if value is None:
            return None
        org = self._get_org()
        if not org:
            return value
        if not Person.objects.filter(id=value, organization=org).exists():
            raise serializers.ValidationError(
                "Selected family member does not exist in this organization."
            )
        # Don't allow linking to self
        if self.instance and self.instance.id == value:
            raise serializers.ValidationError(
                "A person cannot be linked to themselves as family."
            )
        return value

    def validate_custom_field_values(
        self, value: dict[str, Any]
    ) -> dict[str, Any]:
        if not value:
            return value
        org = self._get_org()
        if not org:
            return value

        custom_fields = CustomField.objects.filter(
            organization=org,
            entity_type=CustomField.EntityType.PERSON,
            enabled=True,
        )
        cf_by_id = {cf.id: cf for cf in custom_fields}

        validated: dict[str, Any] = {}
        for cf_id_str, raw_value in value.items():
            try:
                cf_id = int(cf_id_str)
            except (ValueError, TypeError):
                continue
            cf = cf_by_id.get(cf_id)
            if not cf:
                continue
            coerced = validate_custom_field_value(cf, raw_value)
            validated[cf_id_str] = coerced

        # Check required custom fields that were not submitted
        for cf in custom_fields:
            cf_key = str(cf.id)
            if cf.required and cf_key not in validated:
                submitted = value.get(cf_key)
                if submitted is None or submitted == "" or submitted == []:
                    raise serializers.ValidationError(
                        {f"cf_{cf.id}": f"'{cf.label}' is required."}
                    )

        return validated

    def _get_org(self):
        request = self.context.get("request")
        if request:
            return getattr(request, "org", None)
        return None


class SavedGridViewSerializer(serializers.ModelSerializer):
    created_by_name = serializers.SerializerMethodField()

    class Meta:
        model = SavedGridView
        fields = [
            "id",
            "name",
            "entity_type",
            "state",
            "is_private",
            "created_by",
            "created_by_name",
            "created",
            "updated",
        ]
        read_only_fields = [
            "id",
            "created_by",
            "created_by_name",
            "created",
            "updated",
        ]

    def get_created_by_name(self, obj):
        if obj.created_by:
            return obj.created_by.get_full_name() or obj.created_by.username
        return None
