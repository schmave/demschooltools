import django

django.setup()

from datetime import datetime
from tabulate import tabulate

from django.db.models import Q

from manual.models import *
from custodia.models import Swipe


def main():
    start_date = datetime(2023, 8, 1)
    end_date = datetime(2024, 7, 31)

    data = []

    for org in Organization.objects.all():
        num_people = Person.objects.filter(organization=org).filter(
            Q(tags__show_in_jc=True)
            | Q(tags__show_in_attendance=True)).distinct().count()
        num_tag_changes = PersonTagChange.objects.filter(
            person__organization=org, time__gte=start_date,
            time__lte=end_date).count()

        data.append([
            org.name,
            num_people,
            num_tag_changes,
            Meeting.objects.filter(organization=org,
                                   date__gte=start_date,
                                   date__lte=end_date).count(),
            ManualChange.objects.filter(
                entry__section__chapter__organization=org,
                date_entered__gte=start_date,
                date_entered__lte=end_date).count(),
            AttendanceDay.objects.filter(person__organization=org,
                                         day__gte=start_date,
                                         day__lte=end_date).count(),
            Swipe.objects.filter(student__person__organization=org,
                                 swipe_day__gte=start_date,
                                 swipe_day__lte=end_date).count(),
        ])

    data.sort(reverse=True, key=lambda x: x[2])

    print(
        tabulate(data,
                 tablefmt="fancy_grid",
                 intfmt=",",
                 headers=[
                     'School name',
                     'Billable #',
                     'Tag changes',
                     'JC meetings',
                     'Manual chgs',
                     'Attendance',
                     'Custodia',
                 ]))


if __name__ == '__main__':
    main()
