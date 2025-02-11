from django.db import models


class School(models.Model):
    """
    Changes from the overseer.schools table currently being used in production:

    alter table overseer.schools add column late_time time DEFAULT '10:15:00'::time without time zone NULL;
    """

    class Meta:
        db_table = "schools"

    id = models.AutoField(db_column="_id", primary_key=True)
    name = models.TextField()
    timezone = models.TextField()
    inserted_date = models.DateTimeField()
    use_display_name = models.BooleanField()
    late_time = models.TimeField()

    def __str__(self):
        return self.name


class Student(models.Model):
    class Meta:
        db_table = "students"

    id = models.AutoField(db_column="_id", primary_key=True)
    person = models.ForeignKey(
        "dst.Person", db_column="dst_id", on_delete=models.PROTECT
    )
    start_date = models.DateField()
    is_teacher = models.BooleanField()
    name = models.TextField()
    show_as_absent = models.DateField()
    school = models.ForeignKey(School, on_delete=models.PROTECT)

    def __str__(self):
        return self.name


class StudentRequiredMinutes(models.Model):
    """
    Add unique ID to students_required_minutes table
        ALTER TABLE overseer.students_required_minutes ADD COLUMN id SERIAL PRIMARY KEY;
    """

    class Meta:
        db_table = "students_required_minutes"

    student = models.ForeignKey(
        Student,
        on_delete=models.PROTECT,
        related_name="required_minutes",
    )
    fromdate = models.DateField()
    required_minutes = models.IntegerField()


class Swipe(models.Model):
    """
    Changes from the overseer.users table currently being used in production:

    * all time fields currently use a time zone. migrate the DB to use UTC instead
    * drop rounded_in_time and rounded_out_time columns
    * create index on overseer.swipes (student_id, swipe_day)
    """

    class Meta:
        db_table = "swipes"

    id = models.AutoField(db_column="_id", primary_key=True)
    student = models.ForeignKey(Student, on_delete=models.PROTECT)
    student_id: int
    swipe_day = models.DateField()

    in_time = models.DateTimeField(null=True)
    out_time = models.DateTimeField(null=True)


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

      * add unique(school, name) constraint
    """

    class Meta:
        db_table = "years"

    id = models.AutoField(db_column="_id", primary_key=True)

    from_time = models.DateTimeField(db_column="from_date")
    to_time = models.DateTimeField(db_column="to_date")

    inserted_date = models.DateTimeField(auto_now_add=True)
    name = models.TextField()
    school = models.ForeignKey(School, on_delete=models.PROTECT)
