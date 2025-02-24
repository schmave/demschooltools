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

    def get_name(self):
        return self.display_name or f"{self.first_name} {self.last_name}"

    email = models.CharField()

    gender = models.CharField()
    address = models.CharField()
    city = models.CharField()
    state = models.CharField()
    zip = models.CharField()
    neighborhood = models.CharField()

    id = models.IntegerField(primary_key=True, db_column="person_id")
    organization_id: int
    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)
    tags = models.ManyToManyField(Tag, db_table="person_tag")

    custodia_show_as_absent = models.DateField()
    custodia_start_date = models.DateField()

    def __str__(self):
        return f'{self.first_name} {self.last_name} "{self.display_name}" [Org {self.organization_id}]'


class Comment(models.Model):
    class Meta:
        db_table = "comments"

    created = models.DateTimeField()


class CompletedTask(models.Model):
    class Meta:
        db_table = "completed_task"

    comment = models.ForeignKey(Comment, on_delete=models.PROTECT)
    person = models.ForeignKey(Person, on_delete=models.PROTECT)


class AttendanceDay(models.Model):
    class Meta:
        db_table = "attendance_day"

    person = models.ForeignKey(Person, on_delete=models.PROTECT)
    day = models.DateField()


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


class Meeting(models.Model):
    class Meta:
        db_table = "meeting"

    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)
    date = models.DateField()


class User(AbstractUser):
    password = models.TextField(db_column="hashed_password")

    class Meta:
        db_table = 'public"."users'

    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)
    name = models.TextField()
    email = models.TextField()  # override this so that emails aren't validated

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
