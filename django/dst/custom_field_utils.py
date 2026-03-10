"""Entity-agnostic utilities for reading and writing CustomFieldValue rows.

This module is designed to work with any entity type (person, tag, family, etc.)
that uses the CustomField/CustomFieldValue system.
"""

from __future__ import annotations

from decimal import Decimal, InvalidOperation
from typing import Any

from rest_framework import serializers

from dst.models import CustomField, CustomFieldValue

# Maps field_type -> the column name on CustomFieldValue that stores the value.
FIELD_TYPE_VALUE_MAP: dict[str, str] = {
    CustomField.FieldType.TEXT: "value_text",
    CustomField.FieldType.INTEGER: "value_integer",
    CustomField.FieldType.NUMBER: "value_number",
    CustomField.FieldType.CONTROLLED_NUMBER: "value_number",
    CustomField.FieldType.CURRENCY: "value_currency",
    CustomField.FieldType.DATE: "value_date",
    CustomField.FieldType.DATETIME: "value_datetime",
    CustomField.FieldType.TOGGLE: "value_boolean",
    CustomField.FieldType.RADIO_GROUP: "value_option_ids",
    CustomField.FieldType.CHECKBOX_GROUP: "value_option_ids",
    CustomField.FieldType.SELECT: "value_option_ids",
    CustomField.FieldType.PEOPLE_SELECT: "value_person_ids",
}

ALL_VALUE_COLUMNS = [
    "value_text",
    "value_integer",
    "value_number",
    "value_currency",
    "value_boolean",
    "value_date",
    "value_datetime",
    "value_option_ids",
    "value_person_ids",
]


def _unwrap_single_value(custom_field: CustomField, raw: Any) -> Any:
    """Unwrap legacy single-element arrays for non-multi select/radio/people fields.

    Before this fix, single-value select/radio/people fields were stored as
    ``[value]`` arrays. This normalises them back to scalar values on read so
    the frontend receives the shape it expects.
    """
    if not isinstance(raw, list) or len(raw) != 1:
        return raw
    type_props = custom_field.type_props or {}
    field_type = custom_field.field_type
    is_multi = bool(type_props.get("multiSelect", False))
    if field_type == CustomField.FieldType.CHECKBOX_GROUP:
        # Checkbox groups are always multi — keep as array
        return raw
    if field_type in (
        CustomField.FieldType.SELECT,
        CustomField.FieldType.RADIO_GROUP,
        CustomField.FieldType.PEOPLE_SELECT,
    ) and not is_multi:
        return raw[0]
    return raw


def read_custom_field_values(
    entity_id: int,
    custom_fields: Any,
) -> dict[str, Any]:
    """Read custom field values for an entity, returning {cf_id_str: value}."""
    values_qs = (
        CustomFieldValue.objects.filter(
            entity_id=entity_id,
            custom_field__in=custom_fields,
        )
        .select_related("custom_field")
    )

    result: dict[str, Any] = {}
    for cfv in values_qs:
        cf = cfv.custom_field
        col_name = FIELD_TYPE_VALUE_MAP.get(cf.field_type)
        if col_name:
            raw = getattr(cfv, col_name)
            # Convert Decimal to float for JSON serialization
            if isinstance(raw, Decimal):
                raw = float(raw)
            # Unwrap legacy single-element arrays for non-multi fields
            raw = _unwrap_single_value(cf, raw)
            result[str(cfv.custom_field_id)] = raw
    return result


def write_custom_field_values(
    entity_id: int,
    custom_fields_by_id: dict[int, CustomField],
    submitted_values: dict[str, Any],
) -> None:
    """Upsert CustomFieldValue rows for an entity.

    submitted_values: {cf_id_str: value} — only fields present are touched.
    """
    for cf_id_str, value in submitted_values.items():
        try:
            cf_id = int(cf_id_str)
        except (ValueError, TypeError):
            continue

        custom_field = custom_fields_by_id.get(cf_id)
        if not custom_field:
            continue

        col_name = FIELD_TYPE_VALUE_MAP.get(custom_field.field_type)
        if not col_name:
            continue

        # Build defaults: clear all value columns, then set the correct one
        defaults: dict[str, Any] = {col: None for col in ALL_VALUE_COLUMNS}

        # Skip null/empty values — delete the row instead
        if value is None or value == "" or value == []:
            CustomFieldValue.objects.filter(
                custom_field_id=cf_id, entity_id=entity_id
            ).delete()
            continue

        defaults[col_name] = value

        CustomFieldValue.objects.update_or_create(
            custom_field_id=cf_id,
            entity_id=entity_id,
            defaults=defaults,
        )


def validate_custom_field_value(
    custom_field: CustomField, value: Any
) -> Any:
    """Validate and coerce a single custom field value.

    Raises serializers.ValidationError on invalid input.
    Returns the coerced value.
    """
    field_type = custom_field.field_type
    validation = custom_field.type_validation or {}
    type_props = custom_field.type_props or {}

    if value is None or value == "" or value == []:
        if custom_field.required:
            raise serializers.ValidationError(
                {f"cf_{custom_field.id}": f"'{custom_field.label}' is required."}
            )
        return value

    if field_type == CustomField.FieldType.TEXT:
        if not isinstance(value, str):
            raise serializers.ValidationError(
                {f"cf_{custom_field.id}": "Must be a string."}
            )
        min_len = validation.get("minLength")
        max_len = validation.get("maxLength")
        if min_len is not None and len(value) < int(min_len):
            raise serializers.ValidationError(
                {f"cf_{custom_field.id}": f"Must be at least {min_len} characters."}
            )
        if max_len is not None and len(value) > int(max_len):
            raise serializers.ValidationError(
                {f"cf_{custom_field.id}": f"Must be at most {max_len} characters."}
            )
        return value

    if field_type in (
        CustomField.FieldType.INTEGER,
        CustomField.FieldType.NUMBER,
        CustomField.FieldType.CONTROLLED_NUMBER,
        CustomField.FieldType.CURRENCY,
    ):
        try:
            numeric = (
                int(value)
                if field_type == CustomField.FieldType.INTEGER
                else float(value)
            )
        except (ValueError, TypeError):
            raise serializers.ValidationError(
                {f"cf_{custom_field.id}": "Must be a valid number."}
            )
        min_val = validation.get("min")
        max_val = validation.get("max")
        if min_val is not None and numeric < float(min_val):
            raise serializers.ValidationError(
                {f"cf_{custom_field.id}": f"Must be at least {min_val}."}
            )
        if max_val is not None and numeric > float(max_val):
            raise serializers.ValidationError(
                {f"cf_{custom_field.id}": f"Must be at most {max_val}."}
            )
        if field_type == CustomField.FieldType.CURRENCY:
            try:
                return Decimal(str(value)).quantize(Decimal("0.01"))
            except InvalidOperation:
                raise serializers.ValidationError(
                    {f"cf_{custom_field.id}": "Invalid currency value."}
                )
        return numeric

    if field_type == CustomField.FieldType.TOGGLE:
        if not isinstance(value, bool):
            raise serializers.ValidationError(
                {f"cf_{custom_field.id}": "Must be true or false."}
            )
        return value

    if field_type in (
        CustomField.FieldType.SELECT,
        CustomField.FieldType.RADIO_GROUP,
        CustomField.FieldType.CHECKBOX_GROUP,
    ):
        options = type_props.get("options", [])
        valid_ids = {str(o.get("id")) for o in options}
        multi = type_props.get("multiSelect", False)

        if field_type == CustomField.FieldType.CHECKBOX_GROUP:
            multi = True

        if multi:
            if not isinstance(value, list):
                raise serializers.ValidationError(
                    {f"cf_{custom_field.id}": "Must be an array."}
                )
            invalid = [v for v in value if str(v) not in valid_ids]
            if invalid:
                raise serializers.ValidationError(
                    {f"cf_{custom_field.id}": f"Invalid option(s): {invalid}"}
                )
            return value
        else:
            if isinstance(value, list):
                raise serializers.ValidationError(
                    {f"cf_{custom_field.id}": "Must be a single value, not an array."}
                )
            if str(value) not in valid_ids:
                raise serializers.ValidationError(
                    {f"cf_{custom_field.id}": f"Invalid option: {value}"}
                )
            return value

    if field_type == CustomField.FieldType.PEOPLE_SELECT:
        multi = type_props.get("multiSelect", False)
        if multi:
            if not isinstance(value, list):
                raise serializers.ValidationError(
                    {f"cf_{custom_field.id}": "Must be an array of person IDs."}
                )
        else:
            if isinstance(value, list):
                raise serializers.ValidationError(
                    {f"cf_{custom_field.id}": "Must be a single person ID."}
                )
            return value
        return value

    # date, datetime — pass through as strings for now
    return value
