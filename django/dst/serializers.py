from __future__ import annotations

from typing import Any

from rest_framework import serializers

from dst.models import CustomField


class CustomFieldSerializer(serializers.ModelSerializer):
    """Serializer for CRUD operations on CustomField definitions."""

    ALLOWED_CONDITION_OPERATORS = {
        "equals",
        "notEquals",
        "contains",
        "notContains",
        "isEmpty",
        "isNotEmpty",
    }
    ALLOWED_ENTITY_TYPES = {
        CustomField.EntityType.PERSON,
        CustomField.EntityType.TAG,
    }

    class Meta:
        model = CustomField
        fields = [
            "id",
            "organization",
            "entity_type",
            "field_type",
            "label",
            "help_text",
            "enabled",
            "required",
            "disabled",
            "display_order",
            "type_props",
            "type_validation",
            "default_value",
            "required_if",
            "disabled_if",
            "visible_to_role_ids",
            "editable_by_role_ids",
            "date_created",
            "date_updated",
        ]
        read_only_fields = [
            "id",
            "organization",
            "date_created",
            "date_updated",
        ]

    def validate_visible_to_role_ids(self, value: Any) -> list[str]:
        return self._validate_string_list(value, "visible_to_role_ids")

    def validate_editable_by_role_ids(self, value: Any) -> list[str]:
        return self._validate_string_list(value, "editable_by_role_ids")

    def validate_entity_type(self, value: str) -> str:
        if value not in self.ALLOWED_ENTITY_TYPES:
            raise serializers.ValidationError(
                f"Entity type must be one of: {', '.join(sorted(self.ALLOWED_ENTITY_TYPES))}."
            )
        return value

    def validate_required_if(self, value: Any) -> list[dict[str, Any]]:
        return self._validate_conditions(value, "required_if")

    def validate_disabled_if(self, value: Any) -> list[dict[str, Any]]:
        return self._validate_conditions(value, "disabled_if")

    def validate(self, attrs: dict[str, Any]) -> dict[str, Any]:
        attrs = super().validate(attrs)
        field_type = attrs.get("field_type", getattr(self.instance, "field_type", None))
        type_props = attrs.get(
            "type_props", getattr(self.instance, "type_props", None) or {}
        )
        default_value = attrs.get(
            "default_value", getattr(self.instance, "default_value", None)
        )

        if not isinstance(type_props, dict):
            raise serializers.ValidationError(
                {"type_props": "Must be an object containing type-specific settings."}
            )

        if field_type in (
            CustomField.FieldType.SELECT,
            CustomField.FieldType.RADIO_GROUP,
            CustomField.FieldType.CHECKBOX_GROUP,
        ):
            options = type_props.get("options")
            if not isinstance(options, list) or not all(
                isinstance(option, dict) and "id" in option and "label" in option
                for option in options
            ):
                raise serializers.ValidationError(
                    {
                        "type_props": (
                            "Select/radio fields require an 'options' list with id/label entries."
                        )
                    }
                )
            multi_select = type_props.get("multiSelect")
            if multi_select is not None and not isinstance(multi_select, bool):
                raise serializers.ValidationError(
                    {"type_props": "multiSelect must be a boolean when provided."}
                )
        elif field_type == CustomField.FieldType.PEOPLE_SELECT:
            if "multiSelect" in type_props and not isinstance(
                type_props.get("multiSelect"), bool
            ):
                raise serializers.ValidationError(
                    {"type_props": "peopleSelect multiSelect flag must be boolean."}
                )
            filter_by_tags = type_props.get("filterByTags")
            if filter_by_tags is not None and not isinstance(filter_by_tags, list):
                raise serializers.ValidationError(
                    {"type_props": "filterByTags must be an array of tag identifiers."}
                )

        attrs["default_value"] = self._normalize_default_value(
            field_type, default_value, type_props
        )

        return attrs

    def validate_field_type(self, value: str) -> str:
        if (
            self.instance
            and self.instance.field_type != value
            and self.instance.values.exists()
        ):
            raise serializers.ValidationError(
                "Cannot change field_type while values exist for this field."
            )
        return value

    def _validate_string_list(self, value: Any, field_name: str) -> list[str]:
        if value in (None, []):
            return []
        if not isinstance(value, list) or not all(isinstance(item, str) for item in value):
            raise serializers.ValidationError(
                {field_name: "Must be an array of role keys (strings)."}
            )
        return value

    def _validate_conditions(
        self, value: Any, field_name: str
    ) -> list[dict[str, Any]]:
        if value in (None, []):
            return []

        if not isinstance(value, list):
            raise serializers.ValidationError({field_name: "Must be an array of objects."})

        normalized: list[dict[str, Any]] = []
        for condition in value:
            if not isinstance(condition, dict):
                raise serializers.ValidationError(
                    {field_name: "Each condition must be an object."}
                )
            field_key = condition.get("fieldKey")
            operator = condition.get("operator")
            if not isinstance(field_key, str) or not field_key:
                raise serializers.ValidationError(
                    {field_name: "Each condition requires a non-empty fieldKey."}
                )
            if operator not in self.ALLOWED_CONDITION_OPERATORS:
                raise serializers.ValidationError(
                    {
                        field_name: (
                            f"Operator must be one of {sorted(self.ALLOWED_CONDITION_OPERATORS)}."
                        )
                    }
                )
            normalized.append(
                {
                    "fieldKey": field_key,
                    "operator": operator,
                    **(
                        {"value": condition["value"]}
                        if "value" in condition
                        else {}
                    ),
                }
            )
        return normalized

    def _normalize_default_value(
        self, field_type: str | None, default_value: Any, type_props: dict[str, Any]
    ) -> Any:
        if default_value in (None, "", [], {}):
            return None

        multi_select = bool(type_props.get("multiSelect"))
        options = type_props.get("options") or []
        option_ids = {str(option.get("id")) for option in options if option.get("id") is not None}

        if field_type in (
            CustomField.FieldType.TEXT,
            CustomField.FieldType.DATE,
            CustomField.FieldType.DATETIME,
        ):
            if not isinstance(default_value, str):
                raise serializers.ValidationError(
                    {"default_value": "Default value must be a string."}
                )
            return default_value

        if field_type in (
            CustomField.FieldType.INTEGER,
            CustomField.FieldType.NUMBER,
            CustomField.FieldType.CONTROLLED_NUMBER,
            CustomField.FieldType.CURRENCY,
        ):
            if not isinstance(default_value, (int, float)):
                raise serializers.ValidationError(
                    {"default_value": "Default value must be numeric."}
                )
            return default_value

        if field_type == CustomField.FieldType.TOGGLE:
            if not isinstance(default_value, bool):
                raise serializers.ValidationError(
                    {"default_value": "Default value must be true or false."}
                )
            return default_value

        if field_type in (
            CustomField.FieldType.SELECT,
            CustomField.FieldType.RADIO_GROUP,
        ):
            if multi_select:
                if not isinstance(default_value, list) or not all(
                    isinstance(item, str) for item in default_value
                ):
                    raise serializers.ValidationError(
                        {"default_value": "Default must be a list of option identifiers."}
                    )
                missing = [item for item in default_value if item not in option_ids]
                if missing:
                    raise serializers.ValidationError(
                        {
                            "default_value": (
                                f"Unknown option identifiers: {', '.join(sorted(missing))}."
                            )
                        }
                    )
                return default_value

            if not isinstance(default_value, str):
                raise serializers.ValidationError(
                    {"default_value": "Default must be a single option identifier."}
                )
            if option_ids and default_value not in option_ids:
                raise serializers.ValidationError(
                    {"default_value": "Default value must reference a defined option."}
                )
            return default_value

        if field_type == CustomField.FieldType.CHECKBOX_GROUP:
            if not isinstance(default_value, list) or not all(
                isinstance(item, str) for item in default_value
            ):
                raise serializers.ValidationError(
                    {"default_value": "Default must be a list of option identifiers."}
                )
            missing = [item for item in default_value if item not in option_ids]
            if missing:
                raise serializers.ValidationError(
                    {
                        "default_value": (
                            f"Unknown option identifiers: {', '.join(sorted(missing))}."
                        )
                    }
                )
            return default_value

        if field_type == CustomField.FieldType.PEOPLE_SELECT:
            if multi_select:
                if not isinstance(default_value, list):
                    raise serializers.ValidationError(
                        {"default_value": "Default must be a list of person identifiers."}
                    )
                return default_value
            if not isinstance(default_value, (str, int)):
                raise serializers.ValidationError(
                    {"default_value": "Default must be a person identifier."}
                )
            return default_value

        return default_value
