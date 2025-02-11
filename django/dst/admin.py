from django.contrib import admin
from django.contrib.sessions.models import Session

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
    User,
)


@admin.register(User)
class UserAdmin(admin.ModelAdmin):
    list_display = ["id", "organization_id", "name", "email"]
    list_filter = ["organization_id"]


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


@admin.register(Session)
class SessionAdmin(admin.ModelAdmin):
    def _session_data(self, obj: Session):
        return obj.get_decoded()

    list_display = ["session_key", "_session_data", "expire_date"]
