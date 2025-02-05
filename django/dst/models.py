from django.contrib.auth.models import AbstractUser
from django.db import models


class Organization(models.Model):
    class Meta:
        db_table = "organization"

    name = models.TextField()
    short_name = models.TextField()


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
    show_in_jc = models.BooleanField()
    show_in_attendance = models.BooleanField()


class Person(models.Model):
    class Meta:
        db_table = "person"

    id = models.IntegerField(primary_key=True, db_column="person_id")
    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)
    tags = models.ManyToManyField(Tag, db_table="person_tag")


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
    """
    Changes from the public.users table currently being used in production:

        ALTER TABLE public.users ADD COLUMN date_joined timestamptz NULL default NULL;
        ALTER TABLE public.users ADD COLUMN last_login timestamptz NULL default NULL;
        ALTER TABLE public.users ADD COLUMN first_name varchar(150) NOT NULL default '';
        ALTER TABLE public.users ADD COLUMN last_name varchar(150) NOT NULL default '';
        ALTER TABLE public.users ADD COLUMN is_superuser bool NOT NULL default false;
        ALTER TABLE public.users ADD COLUMN is_staff bool NOT NULL default false;
        ALTER TABLE public.users ADD COLUMN is_active bool NOT NULL default true;
        ALTER TABLE public.users ADD COLUMN username varchar(150) NOT NULL default '';


    Also need to deal with the fact that passwords are not in the right format.
    Django bcrypt looks like:
       bcrypt$$2b$12$kHtLdeD00SoRqyuqgXZgfevdO0Gy7PAGRFqw0cX49FGLInWRwHZDS

    """

    password = models.TextField(db_column="hashed_password")

    class Meta:
        db_table = 'public"."users'

    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)


class LinkedAccount(models.Model):
    class Meta:
        db_table = 'public"."linked_account'

    user = models.ForeignKey(
        User, on_delete=models.CASCADE, related_name="linked_accounts"
    )
    provider_user_id = models.TextField()
    provider_key = models.TextField()
