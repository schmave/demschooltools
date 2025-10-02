from datetime import date
from decimal import Decimal

from django.contrib.auth.models import AbstractUser
from django.db import models


class Organization(models.Model):
    class Meta:
        db_table = "organization"

    id: int
    name = models.TextField()
    short_name = models.TextField(default="", blank=True)
    timezone = models.TextField(default="America/New_York")
    late_time = models.TimeField(null=True, blank=True)

    # Email and printing settings
    printer_email = models.TextField(default="", blank=True)
    mailchimp_api_key = models.TextField(default="", blank=True)
    mailchimp_last_sync_person_changes = models.DateTimeField(null=True, blank=True)
    mailchimp_updates_email = models.TextField(default="", blank=True)

    # JC settings
    jc_reset_day = models.IntegerField(default=3)
    show_last_modified_in_print = models.BooleanField(default=True)
    show_history_in_print = models.BooleanField(default=True)

    # Feature flags
    show_custodia = models.BooleanField(default=False)
    show_attendance = models.BooleanField(default=True)
    show_electronic_signin = models.BooleanField(default=False)
    show_accounting = models.BooleanField(default=False)
    show_roles = models.BooleanField(default=False)
    enable_case_references = models.BooleanField(default=False)

    # Attendance settings
    attendance_enable_off_campus = models.BooleanField(default=False)
    attendance_show_reports = models.BooleanField(default=False)
    attendance_report_latest_departure_time = models.TimeField(null=True, blank=True)
    attendance_report_latest_departure_time_2 = models.TimeField(null=True, blank=True)
    attendance_day_earliest_departure_time = models.TimeField(null=True, blank=True)
    attendance_report_late_fee = models.IntegerField(null=True, blank=True)
    attendance_report_late_fee_2 = models.IntegerField(null=True, blank=True)
    attendance_report_late_fee_interval = models.IntegerField(null=True, blank=True)
    attendance_report_late_fee_interval_2 = models.IntegerField(null=True, blank=True)
    attendance_show_percent = models.BooleanField(default=False)
    attendance_show_weighted_percent = models.BooleanField(default=False)
    attendance_show_rate_in_checkin = models.BooleanField(default=False)
    attendance_enable_partial_days = models.BooleanField(default=False)
    attendance_day_latest_start_time = models.TimeField(null=True, blank=True)
    attendance_day_min_hours = models.FloatField(null=True, blank=True)
    attendance_partial_day_value = models.DecimalField(
        max_digits=10, decimal_places=2, null=True, blank=True
    )
    attendance_admin_pin = models.CharField(max_length=10, default="", blank=True)
    attendance_default_absence_code = models.TextField(null=True, blank=True)
    attendance_default_absence_code_time = models.TimeField(null=True, blank=True)

    # Roles settings
    roles_individual_term = models.TextField(default="Clerk", null=True, blank=True)
    roles_committee_term = models.TextField(default="Committee", null=True, blank=True)
    roles_group_term = models.TextField(default="Group", null=True, blank=True)

    # Custodia settings
    custodia_password = models.TextField(default="", blank=True)

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
        constraints = [
            models.UniqueConstraint(
                fields=["title", "organization"], name="unique_title_org"
            )
        ]

    organization = models.ForeignKey("Organization", on_delete=models.PROTECT)

    title = models.TextField()
    use_student_display = models.BooleanField(default=False)

    show_in_jc = models.BooleanField(default=False)
    show_in_attendance = models.BooleanField(default=False)
    show_in_menu = models.BooleanField(default=True)
    show_in_account_balances = models.BooleanField(default=False)
    show_in_roles = models.BooleanField(default=True)


class Person(models.Model):
    class Meta:
        db_table = "person"

    # Names and display
    first_name = models.CharField(max_length=255, default="")
    last_name = models.CharField(max_length=255, default="")
    display_name = models.CharField(max_length=255, default="")

    # Family relationship
    family_person = models.ForeignKey(
        "self",
        on_delete=models.PROTECT,
        null=True,
        blank=True,
        db_column="family_person_id",
    )
    is_family = models.BooleanField(default=False)

    # Contact info
    email = models.CharField(max_length=255, default="", blank=True)
    address = models.CharField(max_length=255, default="", blank=True)
    city = models.CharField(max_length=255, default="", blank=True)
    state = models.CharField(max_length=255, default="", blank=True)
    zip = models.CharField(max_length=255, default="", blank=True)
    neighborhood = models.CharField(max_length=255, default="", blank=True)

    # Personal info
    gender = models.TextField(default="Unknown")
    dob = models.DateField(null=True, blank=True)
    approximate_dob = models.DateField(null=True, blank=True)
    grade = models.TextField(default="", blank=True)
    notes = models.TextField(default="", blank=True)

    # School info
    previous_school = models.CharField(max_length=255, default="")
    school_district = models.CharField(max_length=255, default="")

    # System fields
    id = models.IntegerField(primary_key=True, db_column="person_id")
    organization_id: int
    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)
    tags = models.ManyToManyField(Tag, db_table="person_tag", blank=True)
    created = models.DateTimeField(auto_now_add=True)
    pin = models.CharField(max_length=10, default="", blank=True)

    # Custodia fields
    custodia_show_as_absent = models.DateField(null=True, blank=True)
    custodia_start_date = models.DateField(null=True, blank=True)

    def get_name(self):
        return self.display_name or self.first_name

    def __str__(self):
        return f'{self.first_name} {self.last_name} "{self.display_name}" [Org {self.organization_id}]'


class PhoneNumber(models.Model):
    class Meta:
        db_table = "phone_numbers"

    person = models.ForeignKey(Person, on_delete=models.CASCADE)
    comment = models.CharField(max_length=255, null=True, blank=True)
    number = models.CharField(max_length=255, null=True, blank=True)


class Donation(models.Model):
    class Meta:
        db_table = "donation"

    dollar_value = models.FloatField(null=True, blank=True)
    is_cash = models.BooleanField()
    description = models.TextField()
    person = models.ForeignKey(Person, on_delete=models.CASCADE)
    date = models.DateTimeField(auto_now_add=True)

    thanked = models.BooleanField()
    thanked_by_user = models.ForeignKey(
        "User",
        on_delete=models.PROTECT,
        null=True,
        blank=True,
        related_name="donations_thanked",
        db_column="thanked_by_user_id",
    )
    thanked_time = models.DateTimeField(null=True, blank=True)

    indiegogo_reward_given = models.BooleanField()
    indiegogo_reward_by_user = models.ForeignKey(
        "User",
        on_delete=models.PROTECT,
        null=True,
        blank=True,
        related_name="donations_indiegogo_rewarded",
        db_column="indiegogo_reward_by_user_id",
    )
    indiegogo_reward_given_time = models.DateTimeField(null=True, blank=True)


class Email(models.Model):
    class Meta:
        db_table = "email"

    message = models.TextField(null=True, blank=True)
    sent = models.BooleanField()
    deleted = models.BooleanField()
    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)


class Role(models.Model):
    class Meta:
        db_table = "role"

    organization_id: int
    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)
    is_active = models.BooleanField(default=True)
    type = models.IntegerField()
    eligibility = models.IntegerField()
    name = models.TextField()
    notes = models.TextField()
    description = models.TextField()


class RoleRecord(models.Model):
    class Meta:
        db_table = "role_record"

    role = models.ForeignKey(Role, on_delete=models.PROTECT)
    role_name = models.TextField()
    date_created = models.DateTimeField()


class RoleRecordMember(models.Model):
    class Meta:
        db_table = "role_record_member"

    role_record = models.ForeignKey(
        RoleRecord, on_delete=models.PROTECT, db_column="record_id"
    )
    person = models.ForeignKey(Person, on_delete=models.PROTECT, null=True, blank=True)
    person_name = models.TextField(null=True, blank=True)
    type = models.IntegerField()


class AttendanceRule(models.Model):
    class Meta:
        db_table = "attendance_rule"

    organization_id: int
    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)
    category = models.TextField(null=True, blank=True)
    person = models.ForeignKey(Person, on_delete=models.PROTECT, null=True, blank=True)
    start_date = models.DateField(null=True, blank=True)
    end_date = models.DateField(null=True, blank=True)
    monday = models.BooleanField()
    tuesday = models.BooleanField()
    wednesday = models.BooleanField()
    thursday = models.BooleanField()
    friday = models.BooleanField()
    absence_code = models.CharField(max_length=64, null=True, blank=True)
    min_hours = models.FloatField(null=True, blank=True)
    latest_start_time = models.TimeField(null=True, blank=True)
    earliest_departure_time = models.TimeField(null=True, blank=True)
    exempt_from_fees = models.BooleanField()


class Comment(models.Model):
    class Meta:
        db_table = "comments"

    message = models.TextField(null=True, blank=True)
    created = models.DateTimeField(auto_now_add=True)
    person = models.ForeignKey(Person, on_delete=models.CASCADE)
    user = models.ForeignKey("User", on_delete=models.PROTECT, db_column="user_id")


class TaskList(models.Model):
    class Meta:
        db_table = "task_list"

    title = models.CharField(max_length=255, null=True, blank=True)
    tag = models.ForeignKey(Tag, on_delete=models.PROTECT, null=True, blank=True)
    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)


class Task(models.Model):
    class Meta:
        db_table = "task"

    title = models.CharField(max_length=255, null=True, blank=True)
    task_list = models.ForeignKey(
        TaskList, on_delete=models.PROTECT, null=True, blank=True
    )
    sort_order = models.IntegerField(null=True, blank=True)
    enabled = models.BooleanField(null=True, blank=True)


class CompletedTask(models.Model):
    class Meta:
        db_table = "completed_task"
        constraints = [
            models.UniqueConstraint(
                fields=["task", "person"], name="unique_completed_task_1"
            )
        ]

    comment = models.ForeignKey(
        Comment, on_delete=models.CASCADE, null=True, blank=True
    )
    person = models.ForeignKey(Person, on_delete=models.CASCADE)
    task = models.ForeignKey(Task, on_delete=models.PROTECT)


class NotificationRule(models.Model):
    class Meta:
        db_table = "notification_rule"

    tag = models.ForeignKey(Tag, on_delete=models.PROTECT, null=True, blank=True)
    the_type = models.IntegerField()
    email = models.TextField()
    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)


class MailchimpSync(models.Model):
    class Meta:
        db_table = "mailchimp_sync"

    tag = models.ForeignKey(Tag, on_delete=models.PROTECT)
    mailchimp_list_id = models.CharField(max_length=255)
    sync_local_adds = models.BooleanField()
    sync_local_removes = models.BooleanField()
    last_sync = models.DateTimeField(null=True, blank=True)


class AttendanceCode(models.Model):
    class Meta:
        db_table = "attendance_code"

    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)
    code = models.CharField(max_length=64)
    description = models.TextField()
    color = models.CharField(max_length=16)
    counts_toward_attendance = models.BooleanField(default=False)
    not_counted = models.BooleanField(default=False)


class AttendanceDay(models.Model):
    class Meta:
        db_table = "attendance_day"
        constraints = [
            models.UniqueConstraint(fields=["person", "day"], name="u_attendance_day")
        ]
        indexes = [models.Index(fields=["day"], name="attendance_day_day_idx")]

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
            models.UniqueConstraint(
                fields=["person", "monday"], name="u_attendance_week"
            )
        ]
        indexes = [models.Index(fields=["monday"], name="attendance_week_monday_idx")]

    person = models.ForeignKey(Person, on_delete=models.PROTECT)
    monday = models.DateField()
    extra_hours = models.FloatField(default=0)


class PersonTagChange(models.Model):
    class Meta:
        db_table = "person_tag_change"

    person = models.ForeignKey(Person, on_delete=models.PROTECT)
    tag = models.ForeignKey(Tag, on_delete=models.PROTECT)
    creator = models.ForeignKey(
        "User", on_delete=models.PROTECT, db_column="creator_id"
    )
    time = models.DateTimeField(auto_now_add=True)
    was_add = models.BooleanField()


class PersonChange(models.Model):
    class Meta:
        db_table = "person_change"

    person = models.ForeignKey(Person, on_delete=models.PROTECT)
    old_email = models.CharField(max_length=255)
    new_email = models.CharField(max_length=255)
    time = models.DateTimeField(auto_now_add=True)


class ManualManager(models.Manager):
    def get_queryset(self):
        return super().get_queryset().filter(deleted=False).order_by("num")


class Chapter(models.Model):
    class Meta:
        db_table = "chapter"

    objects = ManualManager()
    all_objects = models.Manager()

    id: int
    title = models.TextField()
    num = models.TextField()
    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)
    organization_id: int
    deleted = models.BooleanField(default=False)

    def __str__(self):
        return f"{self.num} {self.title}"


class Section(models.Model):
    class Meta:
        db_table = "section"

    objects = ManualManager()
    all_objects = models.Manager()

    id: int
    title = models.TextField()
    num = models.TextField()
    chapter = models.ForeignKey(
        Chapter, on_delete=models.PROTECT, related_name="sections"
    )
    chapter_id: int
    deleted = models.BooleanField(default=False)

    def number(self):
        return self.chapter.num + self.num

    def __str__(self):
        return f"{self.number()} {self.title}"


class Entry(models.Model):
    class Meta:
        db_table = "entry"

    objects = ManualManager()
    all_objects = models.Manager()

    id: int
    title = models.TextField()
    num = models.TextField()
    section_id: int
    section = models.ForeignKey(
        Section, on_delete=models.PROTECT, related_name="entries"
    )
    deleted = models.BooleanField(default=False)
    content = models.TextField()
    is_breaking_res_plan = models.BooleanField(default=False)

    def number(self):
        return self.section.number() + "." + self.num

    # TODO: get rid of this. use a template filter instead? this is ugly
    def changes_for_render(self) -> list["ManualChange"]:
        if self.id:
            return list(self.changes.all())
        return []

    def __str__(self):
        return f"{self.num} {self.title}"


class ManualChange(models.Model):
    class Meta:
        db_table = "manual_change"

    chapter = models.ForeignKey(
        Chapter, on_delete=models.PROTECT, related_name="changes", null=True, blank=True
    )
    section = models.ForeignKey(
        Section, on_delete=models.PROTECT, related_name="changes", null=True, blank=True
    )
    entry = models.ForeignKey(
        Entry, on_delete=models.PROTECT, related_name="changes", null=True, blank=True
    )
    date_entered = models.DateTimeField(auto_now_add=True)
    effective_date = models.DateField(null=True, blank=True)
    show_date_in_history = models.BooleanField(default=True)
    user = models.ForeignKey("User", on_delete=models.PROTECT, null=True, blank=True)

    was_deleted = models.BooleanField(default=False)
    was_created = models.BooleanField(default=False)
    old_content = models.TextField(null=True, blank=True)
    new_content = models.TextField(null=True, blank=True)
    old_title = models.CharField(max_length=255, null=True, blank=True)
    new_title = models.CharField(max_length=255, null=True, blank=True)
    old_num = models.TextField(null=True, blank=True)
    new_num = models.TextField(null=True, blank=True)

    def effective_date_with_fallback(self) -> date:
        return self.effective_date or self.date_entered.date()


class Account(models.Model):
    class Meta:
        db_table = "account"

    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)
    person = models.ForeignKey(Person, on_delete=models.PROTECT, null=True, blank=True)
    type = models.IntegerField()
    initial_balance = models.DecimalField(max_digits=10, decimal_places=2)
    name = models.TextField(default="")
    monthly_credit = models.DecimalField(
        max_digits=10, decimal_places=2, default=Decimal(0)
    )
    date_last_monthly_credit = models.DateTimeField(null=True, blank=True)
    is_active = models.BooleanField(default=True)


class Transaction(models.Model):
    class Meta:
        db_table = "transactions"

    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)
    from_account = models.ForeignKey(
        Account,
        on_delete=models.PROTECT,
        null=True,
        blank=True,
        related_name="transactions_from",
        db_column="from_account_id",
    )
    to_account = models.ForeignKey(
        Account,
        on_delete=models.PROTECT,
        null=True,
        blank=True,
        related_name="transactions_to",
        db_column="to_account_id",
    )
    from_name = models.TextField()
    to_name = models.TextField()
    description = models.TextField()
    type = models.IntegerField()
    amount = models.DecimalField(max_digits=10, decimal_places=2)
    date_created = models.DateTimeField()
    created_by_user = models.ForeignKey(
        "User",
        on_delete=models.PROTECT,
        null=True,
        blank=True,
        db_column="created_by_user_id",
    )
    archived = models.BooleanField(default=False)


class Meeting(models.Model):
    class Meta:
        db_table = "meeting"
        constraints = [
            models.UniqueConstraint(
                fields=["organization", "date"], name="unq_org_date"
            )
        ]

    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)
    date = models.DateField()


class Case(models.Model):
    class Meta:
        db_table = "case"
        constraints = [
            models.UniqueConstraint(
                fields=["case_number", "meeting"], name="u_case_number_meeting"
            )
        ]

    case_number = models.CharField(max_length=255, default="")
    findings = models.TextField(default="")
    date = models.DateField(null=True, blank=True)
    location = models.CharField(max_length=255, default="")
    time = models.CharField(max_length=255, default="")
    date_closed = models.DateField(null=True, blank=True)
    meeting = models.ForeignKey(Meeting, on_delete=models.PROTECT)
    meetings = models.ManyToManyField(
        Meeting, through="CaseMeeting", related_name="cases_many"
    )


class CaseMeeting(models.Model):
    class Meta:
        db_table = "case_meeting"

    case = models.ForeignKey(Case, on_delete=models.CASCADE)
    meeting = models.ForeignKey(Meeting, on_delete=models.CASCADE)


class Charge(models.Model):
    class Meta:
        db_table = "charge"

    case = models.ForeignKey(Case, on_delete=models.PROTECT)
    person = models.ForeignKey(Person, on_delete=models.PROTECT, null=True, blank=True)
    rule = models.ForeignKey(
        Entry, on_delete=models.PROTECT, null=True, blank=True, db_column="rule_id"
    )
    plea = models.CharField(max_length=255, default="")
    resolution_plan = models.TextField(default="")
    referred_to_sm = models.BooleanField(default=False)
    sm_decision = models.TextField(null=True, blank=True)
    sm_decision_date = models.DateField(null=True, blank=True)
    minor_referral_destination = models.CharField(max_length=255, default="")
    severity = models.CharField(max_length=255, default="")
    referenced_charge = models.ForeignKey(
        "self",
        on_delete=models.PROTECT,
        null=True,
        blank=True,
        db_column="referenced_charge_id",
    )
    rp_complete = models.BooleanField(default=False)
    rp_complete_date = models.DateTimeField(null=True, blank=True)


class ChargeReference(models.Model):
    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=["referenced_charge", "referencing_case"], name="uk_charge_case"
            )
        ]
        db_table = "charge_reference"

    referenced_charge = models.ForeignKey(
        Charge, on_delete=models.PROTECT, db_column="referenced_charge"
    )
    referencing_case = models.ForeignKey(
        Case, on_delete=models.PROTECT, db_column="referencing_case"
    )


class CaseReference(models.Model):
    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=["referenced_case", "referencing_case"], name="uk_case_case"
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

    meeting = models.ForeignKey(
        Meeting, on_delete=models.PROTECT, null=True, blank=True
    )
    person = models.ForeignKey(Person, on_delete=models.PROTECT)
    role = models.IntegerField(null=True, blank=True)


class PersonAtCase(models.Model):
    class Meta:
        db_table = "person_at_case"

    case = models.ForeignKey(Case, on_delete=models.PROTECT, null=True, blank=True)
    person = models.ForeignKey(Person, on_delete=models.PROTECT)
    role = models.IntegerField(default=0)


CHECKIN_USERNAME = "Check-in app user"


class User(AbstractUser):
    password = models.TextField(db_column="hashed_password")

    class Meta:
        db_table = 'public"."users'
        constraints = [
            models.UniqueConstraint(fields=["email"], name="users_unique_email_1")
        ]

    is_active = models.BooleanField(
        default=True,
        db_column="active",
    )
    organization = models.ForeignKey(
        Organization, on_delete=models.PROTECT, null=True, blank=True
    )
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

    def __str__(self):
        return self.name


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
        constraints = [
            models.UniqueConstraint(
                fields=["provider_key", "provider_user_id"], name="u_linked_account"
            )
        ]

    user = models.ForeignKey(
        User, on_delete=models.CASCADE, related_name="linked_accounts"
    )
    provider_user_id = models.TextField()
    provider_key = models.TextField()


class AllowedIp(models.Model):
    class Meta:
        db_table = "allowed_ips"

    ip = models.TextField(primary_key=True)
    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)
