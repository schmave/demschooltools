import django.db.models.deletion
from django.conf import settings
from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [
        ("dst", "0007_merge_tags_family_into_basic_info"),
    ]

    operations = [
        migrations.CreateModel(
            name="SavedGridView",
            fields=[
                (
                    "id",
                    models.AutoField(
                        auto_created=True,
                        primary_key=True,
                        serialize=False,
                        verbose_name="ID",
                    ),
                ),
                ("name", models.CharField(max_length=255)),
                ("entity_type", models.CharField(default="person", max_length=50)),
                ("state", models.JSONField()),
                ("is_private", models.BooleanField(default=False)),
                ("created", models.DateTimeField(auto_now_add=True)),
                ("updated", models.DateTimeField(auto_now=True)),
                (
                    "organization",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="saved_grid_views",
                        to="dst.organization",
                    ),
                ),
                (
                    "created_by",
                    models.ForeignKey(
                        db_column="created_by_id",
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="saved_grid_views",
                        to=settings.AUTH_USER_MODEL,
                    ),
                ),
            ],
            options={
                "db_table": "saved_grid_views",
                "ordering": ["name"],
            },
        ),
    ]
