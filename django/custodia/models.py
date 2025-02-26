from django.db import models

from dst.models import Organization, Person

# class Student(models.Model):
#     class Meta:
#         db_table = "old_students"

#     id = models.AutoField(db_column="_id", primary_key=True)
#     name = models.TextField()
#     person = models.ForeignKey(Person, on_delete=models.PROTECT, db_column="dst_id")


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

    in_time = models.DateTimeField()
    out_time = models.DateTimeField(null=True, blank=True)

    def __str__(self):
        return f"ID-{self.id} {self.in_time}-{self.out_time}"


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
    class Meta:
        db_table = "years"

    id = models.AutoField(db_column="_id", primary_key=True)

    from_time = models.DateTimeField(db_column="from_date")
    to_time = models.DateTimeField(db_column="to_date")

    inserted_date = models.DateTimeField(auto_now_add=True)
    name = models.TextField()
    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)
