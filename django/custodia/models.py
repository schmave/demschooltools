from django.db import models

from dst.models import Person


class School(models.Model):
    class Meta:
        db_table = "schools"

    id = models.AutoField(db_column="_id", primary_key=True)
    name = models.TextField()
    timezone = models.TextField()
    inserted_date = models.DateTimeField()
    use_display_name = models.BooleanField()
    late_time = models.TimeField()

    def __str__(self):
        return f"{self.id}-{self.name}"


# class Student(models.Model):
#     class Meta:
#         db_table = "students"

#     id = models.AutoField(db_column="_id", primary_key=True)
#     person = models.ForeignKey(
#         "dst.Person", db_column="dst_id", on_delete=models.PROTECT
#     )
#     start_date = models.DateField()
#     is_teacher = models.BooleanField()
#     name = models.TextField()
#     show_as_absent = models.DateField()
#     school = models.ForeignKey(School, on_delete=models.PROTECT)

#     def __str__(self):
#         return self.name


class StudentRequiredMinutes(models.Model):
    class Meta:
        db_table = "students_required_minutes"

    person_id: int
    person = models.ForeignKey(
        Person,
        on_delete=models.PROTECT,
        related_name="required_minutes",
    )
    fromdate = models.DateField()
    required_minutes = models.IntegerField()


class Swipe(models.Model):
    class Meta:
        db_table = "swipes"

    id = models.AutoField(db_column="_id", primary_key=True)
    person = models.ForeignKey(Person, on_delete=models.PROTECT)
    person_id: int
    swipe_day = models.DateField()

    in_time = models.DateTimeField(null=True)
    out_time = models.DateTimeField(null=True)


class Override(models.Model):
    class Meta:
        db_table = "overrides"

    id = models.AutoField(db_column="_id", primary_key=True)
    person = models.ForeignKey(Person, on_delete=models.PROTECT)
    person_id: int
    inserted_date = models.DateTimeField(auto_now_add=True)

    date = models.DateField()


class Excuse(models.Model):
    class Meta:
        db_table = "excuses"

    id = models.AutoField(db_column="_id", primary_key=True)
    person = models.ForeignKey(Person, on_delete=models.PROTECT)
    person_id: int
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
