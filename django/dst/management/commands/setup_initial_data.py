"""
uv run manage.py setup_initial_data
"""

from django.core.management.base import BaseCommand
from django.db.transaction import atomic

from dst.models import (
    Organization,
    OrganizationHost,
    Tag,
    User,
    UserRole,
)


class Command(BaseCommand):
    help = "Create some initial data for local development"

    @atomic
    def handle(self, *args, **kwargs):
        org = Organization.objects.create(
            short_name="TRVS",
            name="Three Rivers Village School",
            show_custodia=True,
            show_accounting=True,
            enable_case_references=True,
            show_electronic_signin=True,
            show_roles=True,
        )

        OrganizationHost.objects.create(organization=org, host="localhost:9000")

        Tag.objects.create(
            title="Current Student",
            use_student_display=True,
            organization=org,
            show_in_jc=True,
            show_in_attendance=True,
            show_in_account_balances=True,
            show_in_roles=True,
        )
        Tag.objects.create(
            title="Staff",
            use_student_display=False,
            organization=org,
            show_in_jc=True,
            show_in_attendance=True,
            show_in_account_balances=True,
            show_in_roles=True,
        )

        user = User.objects.create(
            email="admin@asdf.com",
            name="Admin User",
            email_validated=True,
            is_staff=True,
            is_superuser=True,
            password="$2a$10$sHAtPc.yeZg2AWMr7EZZbuu.sYaOPgFsMZiAY62q/URbjMxU3jB.q",
        )
        user.save()

        UserRole.objects.create(user=user, role=UserRole.ALL_ACCESS)
