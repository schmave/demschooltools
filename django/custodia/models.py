from django.contrib.auth.models import AbstractUser
from django.db import models


class School(models.Model):
    class Meta:
        db_table = "schools"

    id = models.AutoField(db_column="_id", primary_key=True)
    name = models.TextField()
    timezone = models.TextField()
    inserted_date = models.DateTimeField()
    use_display_name = models.BooleanField()


class CustodiaUser(AbstractUser):
    """
    Changes from the overseer.users table currently being used in production:

        ALTER TABLE overseer.users RENAME COLUMN user_id TO id;
        ALTER TABLE overseer.users RENAME COLUMN inserted_date TO date_joined;

        ALTER TABLE overseer.users ADD COLUMN last_login timestamptz NULL default now();
        ALTER TABLE overseer.users ADD COLUMN is_superuser bool NOT NULL default false;
        ALTER TABLE overseer.users ADD COLUMN first_name varchar(150) NOT NULL default '';
        ALTER TABLE overseer.users ADD COLUMN last_name varchar(150) NOT NULL default '';
        ALTER TABLE overseer.users ADD COLUMN email varchar(254) NOT NULL default '';
        ALTER TABLE overseer.users ADD COLUMN is_staff bool NOT NULL default false;
        ALTER TABLE overseer.users ADD COLUMN is_active bool NOT NULL default true;


    Also need to deal with the fact that passwords are not in the right format.
    Django bcrypt looks like:
       bcrypt$$2b$12$kHtLdeD00SoRqyuqgXZgfevdO0Gy7PAGRFqw0cX49FGLInWRwHZDS

    """

    class Meta:
        db_table = 'overseer"."users'

    school = models.ForeignKey(School, on_delete=models.PROTECT)
    roles = models.TextField()


class Student(models.Model):
    class Meta:
        db_table = "students"

    id = models.AutoField(db_column="_id", primary_key=True)
    person = models.ForeignKey(
        "dst.Person", db_column="dst_id", on_delete=models.PROTECT
    )
    is_teacher = models.BooleanField()
    name = models.TextField()
    show_as_absent = models.DateField()
    school = models.ForeignKey(School, on_delete=models.PROTECT)


class Swipe(models.Model):
    """
    Changes from the overseer.users table currently being used in production:

    * all time fields currently use a time zone. migrate the DB to use UTC instead
    * drop rounded_in_time and rounded_out_time columns
    """

    class Meta:
        db_table = "swipes"

    id = models.AutoField(db_column="_id", primary_key=True)
    student = models.ForeignKey(Student, on_delete=models.PROTECT)
    swipe_day = models.DateField()

    in_time = models.DateTimeField()
    out_time = models.DateTimeField()


class Override(models.Model):
    """
    Recommended changes from the overseer.overrides table currently being used in production:

    Add unique (student, date) constraint
    """

    class Meta:
        db_table = "overrides"

    id = models.AutoField(db_column="_id", primary_key=True)
    student = models.ForeignKey(Student, on_delete=models.PROTECT)
    inserted_date = models.DateTimeField(auto_now_add=True)

    date = models.DateField()


class Excuse(models.Model):
    """
    Recommended changes from the overseer.excuses table currently being used in production:

    Add unique (student, date) constraint
    """

    class Meta:
        db_table = "excuses"

    id = models.AutoField(db_column="_id", primary_key=True)
    student = models.ForeignKey(Student, on_delete=models.PROTECT)
    inserted_date = models.DateTimeField(auto_now_add=True)

    date = models.DateField()


class Year(models.Model):
    """
    Recommended changes from the overseer.years table currently being used in production:

    Figure out timezone of from_date and to_date columns
    """

    class Meta:
        db_table = "years"

    id = models.AutoField(db_column="_id", primary_key=True)

    from_date = models.DateTimeField()
    to_date = models.DateTimeField()

    inserted_date = models.DateTimeField(auto_now_add=True)
    name = models.TextField()
    school = models.ForeignKey(School, on_delete=models.PROTECT)
