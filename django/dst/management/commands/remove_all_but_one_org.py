from typing import Type

from django.core.management.base import BaseCommand
from django.db.models import Model, Q

from custodia.models import Excuse, Override, StudentRequiredMinutes, Swipe
from dst.models import (
    Account,
    AttendanceDay,
    AttendanceRule,
    AttendanceWeek,
    Case,
    CaseReference,
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
    Role,
    RoleRecord,
    RoleRecordMember,
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

        def org_filter(model: Type[Model]):
            return model.objects.exclude(organization_id=org_id)

        people = org_filter(Person)

        def delete_for_people(model: Type[Model]):
            model.objects.filter(person__in=people).delete()

        delete_for_people(PersonTagChange)

        task_lists = org_filter(TaskList)
        tasks = Task.objects.filter(task_list__in=task_lists)
        CompletedTask.objects.filter(task__in=tasks).delete()
        tasks.delete()
        task_lists.delete()

        org_filter(Transaction).delete()
        org_filter(Account).delete()
        org_filter(NotificationRule).delete()

        tags = org_filter(Tag)

        MailchimpSync.objects.filter(tag__in=tags).delete()

        delete_for_people(AttendanceDay)
        delete_for_people(AttendanceWeek)
        delete_for_people(Swipe)
        delete_for_people(Excuse)
        delete_for_people(Override)
        delete_for_people(StudentRequiredMinutes)

        delete_for_people(PersonAtMeeting)
        delete_for_people(PersonAtCase)

        meetings = org_filter(Meeting)
        charges = Charge.objects.filter(case__meeting__in=meetings)
        ChargeReference.objects.filter(charge__in=charges).delete()
        charges.delete()

        cases = Case.objects.filter(meeting__in=meetings)
        CaseReference.objects.filter(
            Q(referenced_case__in=cases) | Q(referencing_case__in=cases)
        ).delete()
        cases.delete()
        meetings.delete()

        delete_for_people(Comment)
        delete_for_people(PersonChange)

        tags.delete()

        roles = org_filter(Role)
        role_records = RoleRecord.objects.filter(role__in=roles)
        RoleRecordMember.objects.filter(role_record__in=role_records).delete()
        role_records.delete()
        roles.delete()

        org_filter(AttendanceRule).delete()

        people.exclude(family_person=None).delete()
        people.delete()
