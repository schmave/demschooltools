from django.contrib.auth.models import AbstractUser
from django.db import models


class Organization(models.Model):
    class Meta:
        db_table = "organization"

    id: int
    name = models.TextField()
    short_name = models.TextField()
    timezone = models.TextField()
    late_time = models.TimeField()

    def __str__(self):
        return f"{self.id}-{self.name}"


class OrganizationHost(models.Model):
    class Meta:
        db_table = "organization_hosts"

    host = models.TextField(primary_key=True)
    organization = models.ForeignKey(
        "Organization", on_delete=models.PROTECT, related_name="hosts"
    )


class Tag(models.Model):
    class Meta:
        db_table = "tag"

    organization = models.ForeignKey("Organization", on_delete=models.PROTECT)

    title = models.TextField()
    use_student_display = models.BooleanField()

    show_in_jc = models.BooleanField()
    show_in_attendance = models.BooleanField()
    show_in_menu = models.BooleanField()
    show_in_account_balances = models.BooleanField()
    show_in_roles = models.BooleanField()


class Person(models.Model):
    class Meta:
        db_table = "person"

    first_name = models.CharField()
    last_name = models.CharField()
    display_name = models.CharField()
    family_person = models.ForeignKey(
        "self", on_delete=models.PROTECT, null=True, blank=True
    )

    def get_name(self):
        return self.display_name or self.first_name

    email = models.CharField(blank=True)

    gender = models.CharField(blank=True)
    address = models.CharField(blank=True)
    city = models.CharField(blank=True)
    state = models.CharField(blank=True)
    zip = models.CharField(blank=True)
    neighborhood = models.CharField(blank=True)

    id = models.IntegerField(primary_key=True, db_column="person_id")
    organization_id: int
    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)
    tags = models.ManyToManyField(Tag, db_table="person_tag")

    custodia_show_as_absent = models.DateField()
    custodia_start_date = models.DateField()

    def __str__(self):
        return f'{self.first_name} {self.last_name} "{self.display_name}" [Org {self.organization_id}]'


class Role(models.Model):
    class Meta:
        db_table = "role"

    organization_id: int
    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)


class RoleRecord(models.Model):
    class Meta:
        db_table = "role_record"

    role = models.ForeignKey(Role, on_delete=models.PROTECT)


class RoleRecordMember(models.Model):
    class Meta:
        db_table = "role_record_member"

    role_record = models.ForeignKey(
        RoleRecord, on_delete=models.PROTECT, db_column="record_id"
    )
    person = models.ForeignKey(Person, on_delete=models.PROTECT)


class AttendanceRule(models.Model):
    class Meta:
        db_table = "attendance_rule"

    organization_id: int
    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)


class Comment(models.Model):
    class Meta:
        db_table = "comments"

    created = models.DateTimeField()
    person = models.ForeignKey(Person, on_delete=models.PROTECT)


class TaskList(models.Model):
    class Meta:
        db_table = "task_list"

    title = models.CharField(max_length=255)
    tag = models.ForeignKey(Tag, on_delete=models.PROTECT)
    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)


class Task(models.Model):
    class Meta:
        db_table = "task"

    task_list = models.ForeignKey(TaskList, on_delete=models.PROTECT)


class CompletedTask(models.Model):
    class Meta:
        db_table = "completed_task"

    comment = models.ForeignKey(Comment, on_delete=models.PROTECT)
    person = models.ForeignKey(Person, on_delete=models.PROTECT)
    task = models.ForeignKey(Task, on_delete=models.PROTECT)


class NotificationRule(models.Model):
    class Meta:
        db_table = "notification_rule"

    tag = models.ForeignKey(Tag, on_delete=models.PROTECT)
    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)


class MailchimpSync(models.Model):
    class Meta:
        db_table = "mailchimp_sync"

    tag = models.ForeignKey(Tag, on_delete=models.PROTECT)


class AttendanceDay(models.Model):
    class Meta:
        db_table = "attendance_day"
        constraints = [
            models.UniqueConstraint("person", "day", name="u_attendance_day")
        ]
        indexes = [models.Index("day", name="attendance_day_day_idx")]

    person = models.ForeignKey(Person, on_delete=models.PROTECT)
    day = models.DateField()
    code = models.CharField(max_length=64, null=True, blank=True)

    start_time = models.TimeField(null=True, blank=True)
    end_time = models.TimeField(null=True, blank=True)

    off_campus_departure_time = models.TimeField(null=True, blank=True)
    off_campus_return_time = models.TimeField(null=True, blank=True)
    off_campus_minutes_exempted = models.IntegerField(null=True, blank=True)


class AttendanceWeek(models.Model):
    class Meta:
        db_table = "attendance_week"
        constraints = [
            models.UniqueConstraint("person", "monday", name="u_attendance_week")
        ]
        indexes = [models.Index("monday", name="attendance_week_monday_idx")]

    person = models.ForeignKey(Person, on_delete=models.PROTECT)
    monday = models.DateField()
    extra_hours = models.FloatField(default=0)


class PersonTagChange(models.Model):
    class Meta:
        db_table = "person_tag_change"

    person = models.ForeignKey(Person, on_delete=models.PROTECT)
    time = models.DateTimeField()
    was_add = models.BooleanField()
    tag = models.ForeignKey(Tag, on_delete=models.PROTECT)


class Chapter(models.Model):
    class Meta:
        db_table = "chapter"

    title = models.TextField()
    num = models.TextField()
    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)
    deleted = models.BooleanField()


class Section(models.Model):
    class Meta:
        db_table = "section"

    title = models.TextField()
    num = models.TextField()
    chapter = models.ForeignKey(Chapter, on_delete=models.PROTECT)
    deleted = models.BooleanField()


class Entry(models.Model):
    class Meta:
        db_table = "entry"

    title = models.TextField()
    num = models.TextField()
    section = models.ForeignKey(Section, on_delete=models.PROTECT)
    deleted = models.BooleanField()
    content = models.TextField()


class ManualChange(models.Model):
    class Meta:
        db_table = "manual_change"

    entry = models.ForeignKey(Entry, on_delete=models.PROTECT)
    date_entered = models.DateTimeField()


class Account(models.Model):
    class Meta:
        db_table = "account"

    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)


class Transaction(models.Model):
    class Meta:
        db_table = "transactions"

    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)


class Meeting(models.Model):
    class Meta:
        db_table = "meeting"

    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)
    date = models.DateField()


class Case(models.Model):
    class Meta:
        db_table = "case"

    meeting = models.ForeignKey(Meeting, on_delete=models.PROTECT)


class Charge(models.Model):
    class Meta:
        db_table = "charge"

    case = models.ForeignKey(Case, on_delete=models.PROTECT)
    person = models.ForeignKey(Person, on_delete=models.PROTECT)


class ChargeReference(models.Model):
    class Meta:
        constraints = [
            models.UniqueConstraint("charge", "the_case", name="uk_charge_case")
        ]
        db_table = "charge_reference"

    charge = models.ForeignKey(
        Charge, on_delete=models.PROTECT, db_column="referenced_charge"
    )
    the_case = models.ForeignKey(
        Case, on_delete=models.PROTECT, db_column="referencing_case"
    )


class CaseReference(models.Model):
    class Meta:
        constraints = [
            models.UniqueConstraint(
                "referenced_case", "referencing_case", name="uk_case_case"
            )
        ]
        db_table = "case_reference"

    referenced_case = models.ForeignKey(
        Case, on_delete=models.PROTECT, db_column="referenced_case"
    )
    referencing_case = models.ForeignKey(
        Case,
        on_delete=models.PROTECT,
        db_column="referencing_case",
        related_name="case_reference_cases",
    )


class PersonAtMeeting(models.Model):
    class Meta:
        db_table = "person_at_meeting"

    person = models.ForeignKey(Person, on_delete=models.PROTECT)


class PersonAtCase(models.Model):
    class Meta:
        db_table = "person_at_case"

    person = models.ForeignKey(Person, on_delete=models.PROTECT)


class PersonChange(models.Model):
    class Meta:
        db_table = "person_change"

    person = models.ForeignKey(Person, on_delete=models.PROTECT)


CHECKIN_USERNAME = "Check-in app user"


class User(AbstractUser):
    password = models.TextField(db_column="hashed_password")

    class Meta:
        db_table = 'public"."users'

    is_active = models.BooleanField(
        default=True,
        db_column="active",
    )
    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)
    name = models.TextField()
    email = models.TextField()  # override this so that emails aren't validated
    email_validated = models.BooleanField(default=False)

    roles: "models.Manager[UserRole]"

    # Disable Django's normal group & permission setup for admin access.
    groups = None
    user_permissions = None

    def hasRole(self, desired_role: str) -> bool:
        for role in self.roles.values_list("role", flat=True):
            if role_includes(role, desired_role):
                return True
        return False


def role_includes(greater_role: str, lesser_role: str) -> bool:
    if greater_role == UserRole.ALL_ACCESS:
        return True
    elif greater_role == UserRole.EDIT_ALL_JC:
        return lesser_role in {
            UserRole.EDIT_31_DAY_JC,
            UserRole.EDIT_7_DAY_JC,
            UserRole.VIEW_JC,
            UserRole.EDIT_RESOLUTION_PLANS,
        }
    elif greater_role == UserRole.EDIT_31_DAY_JC:
        return lesser_role in {
            UserRole.EDIT_7_DAY_JC,
            UserRole.VIEW_JC,
            UserRole.EDIT_RESOLUTION_PLANS,
        }
    elif greater_role == UserRole.EDIT_7_DAY_JC:
        return lesser_role in {UserRole.VIEW_JC, UserRole.EDIT_RESOLUTION_PLANS}

    return False


class UserRole(models.Model):
    ACCOUNTING = "accounting"
    ROLES = "edit-roles"
    ATTENDANCE = "attendance"
    VIEW_JC = "view-jc"
    EDIT_MANUAL = "edit-manual"
    EDIT_RESOLUTION_PLANS = "edit-rps"
    EDIT_7_DAY_JC = "edit-recent-jc"
    EDIT_31_DAY_JC = "edit-recent-31-jc"
    EDIT_ALL_JC = "edit-all-jc"
    ALL_ACCESS = "all-access"
    CHECKIN_APP = "checkin-app"

    class Meta:
        db_table = 'public"."user_role'

    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name="roles")
    role = models.TextField()


class LinkedAccount(models.Model):
    class Meta:
        db_table = 'public"."linked_account'

    user = models.ForeignKey(
        User, on_delete=models.CASCADE, related_name="linked_accounts"
    )
    provider_user_id = models.TextField()
    provider_key = models.TextField()
