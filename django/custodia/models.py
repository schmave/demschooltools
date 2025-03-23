from django.db import models

from dst.models import Organization, Person

# class OldStudent(models.Model):
#     class Meta:
#         db_table = 'overseer"."old_students'

#     id = models.AutoField(db_column="_id", primary_key=True)
#     name = models.TextField()
#     person = models.ForeignKey(Person, on_delete=models.PROTECT, db_column="dst_id")


# class OldUsers(models.Model):
#     class Meta:
#         db_table = 'overseer"."users'

#     id = models.AutoField(db_column="user_id", primary_key=True)
#     username = models.TextField()
#     roles = models.TextField()
#     password = models.TextField()
#     school_id = models.IntegerField()


class StudentRequiredMinutes(models.Model):
    person_id: int
    person = models.ForeignKey(
        Person,
        on_delete=models.PROTECT,
        related_name="required_minutes",
    )
    fromdate = models.DateField()
    required_minutes = models.IntegerField()


class Swipe(models.Model):
    id = models.AutoField(primary_key=True)
    person = models.ForeignKey(Person, on_delete=models.PROTECT)
    person_id: int
    swipe_day = models.DateField()

    in_time = models.DateTimeField()
    out_time = models.DateTimeField(null=True, blank=True)

    def __str__(self):
        return f"ID-{self.id} {self.in_time}-{self.out_time}"

    class Meta:
        indexes = [
            models.Index("person", "swipe_day", name="swipes_person_id_swipe_day_idx"),
            models.Index("swipe_day", "person", name="swipes_swipe_day_person_id_idx"),
        ]


class Override(models.Model):
    id = models.AutoField(primary_key=True)
    person = models.ForeignKey(Person, on_delete=models.PROTECT)
    person_id: int
    inserted_date = models.DateTimeField(auto_now_add=True)

    date = models.DateField()

    class Meta:
        constraints = [
            models.UniqueConstraint("person", "date", name="uniq_overrides_person_date")
        ]


class Excuse(models.Model):
    id = models.AutoField(primary_key=True)
    person = models.ForeignKey(Person, on_delete=models.PROTECT)
    person_id: int
    inserted_date = models.DateTimeField(auto_now_add=True)

    date = models.DateField()

    class Meta:
        constraints = [
            models.UniqueConstraint("person", "date", name="uniq_excuses_person_date")
        ]


class Year(models.Model):
    id = models.AutoField(primary_key=True)

    from_time = models.DateTimeField()
    to_time = models.DateTimeField()

    inserted_date = models.DateTimeField(auto_now_add=True)
    name = models.TextField()
    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)
