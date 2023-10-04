from django.db import models

class Organization(models.Model):
    class Meta:
        db_table = 'organization'
    name = models.TextField()
    short_name = models.TextField()


class Chapter(models.Model):
    class Meta:
        db_table = 'chapter'
    title = models.TextField()
    num = models.TextField()
    organization = models.ForeignKey('Organization', on_delete=models.PROTECT)
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
