from django.db import models

class Organization(models.Model):
    class Meta:
        db_table = 'organization'
    name = models.TextField()
    short_name = models.TextField()


class Tag(models.Model):
    class Meta:
        db_table = 'tag'

    organization = models.ForeignKey('Organization', on_delete=models.PROTECT)

    title = models.TextField()
    show_in_jc = models.BooleanField()
    show_in_attendance = models.BooleanField()

class Person(models.Model):
    class Meta:
        db_table = 'person'

    id = models.IntegerField(primary_key=True, db_column='person_id')
    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)
    tags = models.ManyToManyField(Tag, db_table='person_tag')



class AttendanceDay(models.Model):
    class Meta:
        db_table = 'attendance_day'

    person = models.ForeignKey(Person, on_delete=models.PROTECT)
    day = models.DateField()


class PersonTagChange(models.Model):
    class Meta:
        db_table = 'person_tag_change'

    person = models.ForeignKey(Person, on_delete=models.PROTECT)
    time = models.DateTimeField()


class Chapter(models.Model):
    class Meta:
        db_table = 'chapter'
    title = models.TextField()
    num = models.TextField()
    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)
    deleted = models.BooleanField()

class Section(models.Model):
    class Meta:
        db_table = 'section'
    title = models.TextField()
    num = models.TextField()
    chapter = models.ForeignKey(Chapter, on_delete=models.PROTECT)
    deleted = models.BooleanField()

class Entry(models.Model):
    class Meta:
        db_table = 'entry'
    title = models.TextField()
    num = models.TextField()
    section = models.ForeignKey(Section, on_delete=models.PROTECT)
    deleted = models.BooleanField()
    content = models.TextField()

class ManualChange(models.Model):
    class Meta:
        db_table = 'manual_change'

    entry = models.ForeignKey(Entry, on_delete=models.PROTECT)
    date_entered = models.DateTimeField()

class Meeting(models.Model):
    class Meta:
        db_table = 'meeting'

    organization = models.ForeignKey(Organization, on_delete=models.PROTECT)
    date = models.DateField()
