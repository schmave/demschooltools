from django.db import migrations


def merge_groups(apps, schema_editor):
    CustomFieldGroup = apps.get_model("dst", "CustomFieldGroup")

    for group in CustomFieldGroup.objects.filter(entity_type="person", label="Basic Info"):
        org = group.organization
        keys = list(group.core_field_keys or [])

        # Merge Tags group
        tags_group = CustomFieldGroup.objects.filter(
            organization=org, entity_type="person", label="Tags"
        ).first()
        if tags_group:
            if "tags" not in keys:
                keys.append("tags")
            tags_group.delete()

        # Merge Family group
        family_group = CustomFieldGroup.objects.filter(
            organization=org, entity_type="person", label="Family"
        ).first()
        if family_group:
            if "family_person_id" not in keys:
                keys.append("family_person_id")
            family_group.delete()

        group.core_field_keys = keys
        group.save()


def reverse_merge(apps, schema_editor):
    pass  # Not worth reversing


class Migration(migrations.Migration):
    dependencies = [
        ("dst", "0001_initial"),
    ]
    operations = [
        migrations.RunPython(merge_groups, reverse_merge),
    ]
