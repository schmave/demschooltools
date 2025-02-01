from datetime import datetime

from django.core.management.base import BaseCommand
from django.db.models import Q
from tabulate import tabulate

from custodia.models import Swipe
from dst.models import (
    AttendanceDay,
    CompletedTask,
    ManualChange,
    Meeting,
    Organization,
    Person,
    PersonTagChange,
    Tag,
)


class Command(BaseCommand):
    help = "Prints usage information per school"

    def add_arguments(self, parser):
        parser.add_argument("--year", type=int, required=True)

    def handle(self, *args, year=0, **kwargs):
        assert year > 2010, year
        print(f"Data for school year {year}-{year+1}")

        start_date = datetime(year, 8, 1)
        oct_first = start_date.replace(month=10)
        end_date = datetime(year + 1, 7, 31)

        data = []

        for org in Organization.objects.all():
            student_tags = Tag.objects.exclude(
                title__icontains="staff",
            ).filter(
                Q(show_in_jc=True) | Q(show_in_attendance=True),
                organization=org,
            )
            print(org.name, [x.title for x in student_tags])
            num_people = (
                Person.objects.filter(organization=org, tags__in=student_tags)
                .distinct()
                .count()
            )
            num_tag_changes = PersonTagChange.objects.filter(
                person__organization=org,
                time__gte=start_date,
                time__lte=end_date,
            ).count()

            net_change_since_oct_1 = sum(
                1 if x else -1
                for x in PersonTagChange.objects.filter(
                    time__gte=oct_first,
                    tag__in=student_tags,
                ).values_list("was_add", flat=True)
            )

            data.append(
                [
                    org.name,
                    num_people,
                    net_change_since_oct_1,
                    num_tag_changes,
                    Meeting.objects.filter(
                        organization=org, date__gte=start_date, date__lte=end_date
                    ).count(),
                    ManualChange.objects.filter(
                        entry__section__chapter__organization=org,
                        date_entered__gte=start_date,
                        date_entered__lte=end_date,
                    ).count(),
                    AttendanceDay.objects.filter(
                        person__organization=org, day__gte=start_date, day__lte=end_date
                    ).count(),
                    Swipe.objects.filter(
                        student__person__organization=org,
                        swipe_day__gte=start_date,
                        swipe_day__lte=end_date,
                    ).count(),
                    CompletedTask.objects.filter(
                        person__organization=org,
                        comment__created__gte=start_date,
                        comment__created__lte=end_date,
                    ).count(),
                ]
            )

        data.sort(reverse=True, key=lambda x: x[3])

        print(
            tabulate(
                data,
                tablefmt="fancy_grid",
                intfmt=",",
                headers=[
                    "School name",
                    "Billable # right now",
                    f"Change in billable since {oct_first.date()}",
                    "Tag changes",
                    "JC meetings",
                    "Manual chgs",
                    "Attendance",
                    "Custodia",
                    "Cklist completes",
                ],
            )
        )
