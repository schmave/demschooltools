from django.core.management.base import BaseCommand
from django.db.transaction import atomic

from custodia.models import Excuse, Override, StudentRequiredMinutes, Swipe
from dst.models import (
    Account,
    AttendanceDay,
    AttendanceWeek,
    Case,
    Charge,
    ChargeReference,
    Comment,
    CompletedTask,
    MailchimpSync,
    Meeting,
    NotificationRule,
    Organization,
    Person,
    PersonAtCase,
    PersonAtMeeting,
    PersonChange,
    PersonTagChange,
    Tag,
    Task,
    TaskList,
    Transaction,
)


class Command(BaseCommand):
    help = "Remove all data from the database except for one org"

    def add_arguments(self, parser):
        parser.add_argument("--org_id", type=int, required=True)

    def handle(self, *args, org_id=0, **kwargs):
        assert Organization.objects.filter(id=org_id).exists()

        def org_filter(query_set):
            return query_set.exclude(organization_id=org_id)

        people = org_filter(Person.objects)

        def delete_for_people(model):
            model.objects.filter(person__in=people).delete()

        with atomic():
            delete_for_people(PersonTagChange)

            task_lists = org_filter(TaskList.objects)
            tasks = Task.objects.filter(task_list__in=task_lists)
            CompletedTask.objects.filter(task__in=tasks).delete()
            tasks.delete()
            task_lists.delete()

            org_filter(Transaction.objects).delete()
            org_filter(Account.objects).delete()
            org_filter(NotificationRule.objects).delete()

            tags = org_filter(Tag.objects)

            MailchimpSync.objects.filter(tag__in=tags).delete()

            delete_for_people(AttendanceDay)
            delete_for_people(AttendanceWeek)
            delete_for_people(Swipe)
            delete_for_people(Excuse)
            delete_for_people(Override)
            delete_for_people(StudentRequiredMinutes)

            delete_for_people(PersonAtMeeting)
            delete_for_people(PersonAtCase)

            charges = Charge.objects.filter(person__in=people)
            # TODO: Add unique constraint and/or primary key to ChargeReference table
            ChargeReference(charge__in=charges).delete()
            charges.delete()

            meetings = org_filter(Meeting.objects)
            Case.objects.filter(meeting__in=meetings).delete()
            meetings.delete()

            delete_for_people(Comment)
            delete_for_people(PersonChange)

            tags.delete()
            people.delete()
