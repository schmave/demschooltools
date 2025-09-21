"""
$ psql
    CREATE DATABASE for_export;

$ pg_restore -O -d for_export ~/Downloads/backup_2025_06_30/db.dump

$ psql for_export
   truncate allowed_ips;
   truncate django_admin_log;
   truncate django_session;
   truncate donation;
   truncate linked_account;
   truncate organization_hosts;

$ DST_DB_NAME=for_export uv run manage.py remove_all_but_one_org --org_id <N>
$ pg_dump -O for_export -f <school-name>.sql
"""

from typing import Type

from django.core.management.base import BaseCommand
from django.db.models import Model, Q

from custodia.models import Excuse, Override, StudentRequiredMinutes, Swipe, Year
from dst.models import (
    Account,
    AttendanceCode,
    AttendanceDay,
    AttendanceRule,
    AttendanceWeek,
    Case,
    CaseReference,
    Chapter,
    Charge,
    ChargeReference,
    Comment,
    CompletedTask,
    Entry,
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
    Section,
    Tag,
    Task,
    TaskList,
    Transaction,
    User,
)


class Command(BaseCommand):
    help = "Remove all data from the database except for one org"

    def add_arguments(self, parser):
        parser.add_argument("--org_id", type=int, required=True)

    def handle(self, *args, org_id=0, **kwargs):
        assert Organization.objects.filter(id=org_id).exists()

        def org_filter(model: Type[Model]):
            return model.objects.filter(
                ~Q(organization_id=org_id) | Q(organization_id=None)
            )

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

        org_filter(AttendanceCode).delete()
        delete_for_people(AttendanceDay)
        delete_for_people(AttendanceWeek)
        org_filter(Year).delete()
        delete_for_people(Swipe)
        delete_for_people(Excuse)
        delete_for_people(Override)
        delete_for_people(StudentRequiredMinutes)

        delete_for_people(PersonAtMeeting)
        delete_for_people(PersonAtCase)

        meetings = org_filter(Meeting)
        charges = Charge.objects.filter(case__meeting__in=meetings)
        ChargeReference.objects.filter(charge__in=charges).delete()
        charges._raw_delete(charges.db)

        cases = Case.objects.filter(meeting__in=meetings)
        CaseReference.objects.filter(
            Q(referenced_case__in=cases) | Q(referencing_case__in=cases)
        ).delete()
        cases._raw_delete(cases.db)
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

        chapters = org_filter(Chapter)
        sections = Section.objects.filter(chapter__in=chapters)
        entries = Entry.objects.filter(section__in=sections)
        entries.delete()
        sections.delete()
        chapters.delete()

        users = org_filter(User)
        Comment.objects.filter(user__in=users).delete()
        users.delete()
        Organization.objects.exclude(id=org_id).delete()
