from django.contrib import admin

from dst.models import (
    AttendanceDay,
    Chapter,
    Comment,
    CompletedTask,
    Entry,
    ManualChange,
    Meeting,
    Organization,
    Person,
    PersonTagChange,
    Section,
    Tag,
)

admin.site.register(AttendanceDay)
admin.site.register(Chapter)
admin.site.register(Comment)
admin.site.register(CompletedTask)
admin.site.register(Entry)
admin.site.register(ManualChange)
admin.site.register(Meeting)
admin.site.register(Organization)
admin.site.register(Person)
admin.site.register(PersonTagChange)
admin.site.register(Section)
admin.site.register(Tag)
