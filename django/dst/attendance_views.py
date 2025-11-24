import json

from django.contrib.auth.mixins import LoginRequiredMixin
from django.template.loader import render_to_string
from django.views import View

from dst.models import Person, Tag, UserRole
from dst.utils import DstHttpRequest, render_main_template


class SignInSheetView(LoginRequiredMixin, View):
    def get(self, request: DstHttpRequest):
        if not request.user.hasRole(UserRole.ATTENDANCE):
            raise PermissionError("You don't have privileges to access this item")

        org = request.org

        # Get people with attendance tags (with their tags)
        people_with_tags = []
        for person in (
            Person.objects.filter(
                tags__show_in_attendance=True,
                tags__organization=org,
            )
            .distinct()
            .prefetch_related("tags")
            .order_by("display_name", "first_name")
        ):
            people_with_tags.append(
                {
                    "id": person.id,
                    "label": person.get_name() + " " + person.last_name,
                    "tags": [tag.id for tag in person.tags.all()],
                }
            )

        # Get attendance tags
        tags = []
        for tag in Tag.objects.filter(
            organization=org, show_in_attendance=True
        ).order_by("title"):
            tags.append(
                {
                    "id": tag.id,
                    "label": tag.title,
                }
            )

        # Get all people (for guest selection)
        all_people = []
        for person in (
            Person.objects.filter(organization=org, is_family=False)
            .exclude(first_name="", last_name="", display_name="")
            .only("display_name", "first_name", "last_name")
            .distinct()
        ):
            all_people.append(
                {
                    "id": person.id,
                    "label": person.get_name() + " " + person.last_name,
                }
            )
        all_people.sort(key=lambda x: x["label"])

        return render_main_template(
            request,
            "attendance",
            render_to_string(
                "sign_in_sheet.html",
                {
                    "people_json": json.dumps(people_with_tags),
                    "tags_json": json.dumps(tags),
                    "all_people_json": json.dumps(all_people),
                    "request": request,
                },
            ),
            "Sign in sheet",
            "sign_in_sheet",
        )
