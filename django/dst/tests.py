from django.test import Client, TestCase
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APIClient

from dst.models import (
    CustomField,
    CustomFieldValue,
    Organization,
    User,
    UserRole,
)


class CustomFieldApiTests(TestCase):
    def setUp(self) -> None:
        self.org = Organization.objects.create(name="Admin School")
        self.user = User.objects.create_user(
            username="admin",
            password="pass",
            email="admin@example.com",
            name="Admin User",
            organization=self.org,
        )
        UserRole.objects.create(user=self.user, role=UserRole.ALL_ACCESS)
        self.client = APIClient()
        self.client.force_authenticate(user=self.user)
        self.list_url = reverse("custom-field-list")

    def test_list_fields_scoped_to_org_and_entity(self) -> None:
        other_org = Organization.objects.create(name="Other")
        CustomField.objects.create(
            organization=other_org,
            entity_type=CustomField.EntityType.PERSON,
            field_type=CustomField.FieldType.TEXT,
            label="Other Org Field",
        )
        first = self._create_field(label="Alpha", display_order=2)
        second = self._create_field(label="Beta", display_order=1)

        response = self.client.get(self.list_url)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual([field["id"] for field in response.data], [second.id, first.id])

    def test_create_field_success(self) -> None:
        payload = {
            "entity_type": CustomField.EntityType.PERSON,
            "field_type": CustomField.FieldType.SELECT,
            "label": "Enrollment status",
            "type_props": {
                "options": [
                    {"id": "pending", "label": "Pending"},
                    {"id": "active", "label": "Active"},
                ],
                "multiSelect": False,
            },
            "visible_to_role_ids": ["all-access"],
            "default_value": "pending",
        }

        response = self.client.post(self.list_url, payload, format="json")

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data["label"], "Enrollment status")
        self.assertEqual(response.data["organization"], self.org.id)

    def test_create_tag_field_supported(self) -> None:
        payload = {
            "entity_type": CustomField.EntityType.TAG,
            "field_type": CustomField.FieldType.TEXT,
            "label": "Tag note",
        }
        response = self.client.post(self.list_url, payload, format="json")

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data["entity_type"], CustomField.EntityType.TAG)

    def test_create_field_duplicate_label(self) -> None:
        self._create_field(label="Nickname")

        response = self.client.post(
            self.list_url,
            {
                "entity_type": CustomField.EntityType.PERSON,
                "field_type": CustomField.FieldType.TEXT,
                "label": "Nickname",
            },
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("label", response.data)

    def test_update_field_metadata(self) -> None:
        field = self._create_field(label="Graduation", help_text=None)
        detail_url = reverse("custom-field-detail", args=[field.id])

        response = self.client.patch(
            detail_url,
            {"label": "Graduation year", "display_order": 5},
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        field.refresh_from_db()
        self.assertEqual(field.label, "Graduation year")
        self.assertEqual(field.display_order, 5)

    def test_prevent_field_type_change_with_values(self) -> None:
        field = self._create_field(field_type=CustomField.FieldType.TEXT)
        CustomFieldValue.objects.create(custom_field=field, entity_id=1, value_text="Hi")
        detail_url = reverse("custom-field-detail", args=[field.id])

        response = self.client.patch(
            detail_url,
            {"field_type": CustomField.FieldType.NUMBER},
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_delete_field_soft_disables_when_values_exist(self) -> None:
        field = self._create_field()
        CustomFieldValue.objects.create(custom_field=field, entity_id=2, value_text="X")
        detail_url = reverse("custom-field-detail", args=[field.id])

        response = self.client.delete(detail_url)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        field.refresh_from_db()
        self.assertFalse(field.enabled)
        self.assertTrue(field.disabled)

    def test_delete_field_without_values_removes_record(self) -> None:
        field = self._create_field()
        detail_url = reverse("custom-field-detail", args=[field.id])

        response = self.client.delete(detail_url)

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(CustomField.objects.filter(id=field.id).exists())

    def _create_field(
        self,
        *,
        label: str = "Favorite snack",
        display_order: int | None = None,
        help_text: str | None = "Help text",
        field_type: str = CustomField.FieldType.TEXT,
    ) -> CustomField:
        return CustomField.objects.create(
            organization=self.org,
            entity_type=CustomField.EntityType.PERSON,
            field_type=field_type,
            label=label,
            help_text=help_text,
            display_order=display_order,
        )


class RoleKeyApiTests(TestCase):
    def setUp(self) -> None:
        self.org = Organization.objects.create(name="Admin School")
        self.user = User.objects.create_user(
            username="admin",
            password="pass",
            email="admin@example.com",
            name="Admin User",
            organization=self.org,
        )
        UserRole.objects.create(user=self.user, role=UserRole.ALL_ACCESS)
        self.client = APIClient()
        self.client.force_authenticate(user=self.user)
        self.url = reverse("role-key-list")

    def test_list_role_keys(self) -> None:
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        payload = response.data["roles"]
        self.assertTrue(any(role["id"] == UserRole.ALL_ACCESS for role in payload))


class CustomFieldDefaultValueTests(TestCase):
    def setUp(self) -> None:
        self.org = Organization.objects.create(name="Defaults School")
        self.user = User.objects.create_user(
            username="admin",
            password="pass",
            email="admin@example.com",
            name="Admin User",
            organization=self.org,
        )
        UserRole.objects.create(user=self.user, role=UserRole.ALL_ACCESS)
        self.client = APIClient()
        self.client.force_authenticate(user=self.user)
        self.list_url = reverse("custom-field-list")

    def test_create_with_default_toggle(self) -> None:
        payload = {
            "entity_type": CustomField.EntityType.PERSON,
            "field_type": CustomField.FieldType.TOGGLE,
            "label": "Has consent",
            "default_value": True,
        }

        response = self.client.post(self.list_url, payload, format="json")
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertTrue(response.data["default_value"])

    def test_select_default_must_match_options(self) -> None:
        payload = {
            "entity_type": CustomField.EntityType.PERSON,
            "field_type": CustomField.FieldType.SELECT,
            "label": "Meal choice",
            "type_props": {
                "options": [
                    {"id": "veg", "label": "Vegetarian"},
                    {"id": "omnivore", "label": "Omnivore"},
                ],
                "multiSelect": False,
            },
            "default_value": "unknown",
        }

        response = self.client.post(self.list_url, payload, format="json")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("default_value", response.data)


class CustomFieldSettingsViewTests(TestCase):
    def setUp(self) -> None:
        self.org = Organization.objects.create(name="Admin School")
        self.user = User.objects.create_user(
            username="admin",
            password="pass",
            email="admin@example.com",
            name="Admin User",
            organization=self.org,
        )
        UserRole.objects.create(user=self.user, role=UserRole.ALL_ACCESS)
        self.client = Client()
        self.client.force_login(self.user)
        self.url = reverse("custom-field-settings")

    def test_requires_admin_role(self) -> None:
        non_admin = User.objects.create_user(
            username="staff",
            password="pass",
            email="staff@example.com",
            name="Staff User",
            organization=self.org,
        )
        other_client = Client()
        other_client.force_login(non_admin)
        response = other_client.get(self.url)
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_renders_react_shell(self) -> None:
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("customFieldsApiBase", response.content.decode())
