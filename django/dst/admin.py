from django.contrib import admin
from django.contrib.sessions.models import Session

from dst.models import (
    AttendanceDay,
    AttendanceWeek,
    Case,
    Chapter,
    Charge,
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
    list_filter = ["organization_id", "is_superuser", "is_staff"]
    search_fields = ["name", "email"]


@admin.register(Person)
class PersonAdmin(admin.ModelAdmin):
    readonly_fields = ["id", "organization", "tags"]

    list_display = ["first_name", "last_name", "email", "organization"]
    list_filter = ["organization_id"]
    search_fields = ["first_name", "last_name", "email"]


@admin.register(Tag)
class TagAdmin(admin.ModelAdmin):
    readonly_fields = ["id", "organization"]

    list_display = [
        "title",
        "use_student_display",
        "show_in_jc",
        "show_in_attendance",
        "show_in_menu",
        "show_in_account_balances",
        "show_in_roles",
    ]
    list_filter = ["organization_id"]
    search_fields = ["title"]


@admin.register(Case)
class CaseAdmin(admin.ModelAdmin):
    readonly_fields = ["meeting"]

    list_display = [
        "meeting",
    ]


@admin.register(Charge)
class ChargeAdmin(admin.ModelAdmin):
    readonly_fields = ["person", "case"]

    list_display = ["person", "case"]


@admin.register(AttendanceDay)
class AttendanceDayAdmin(admin.ModelAdmin):
    readonly_fields = ["person", "day"]

    list_filter = ["day"]
    list_display = ["person", "day"]


@admin.register(AttendanceWeek)
class AttendanceWeekAdmin(admin.ModelAdmin):
    readonly_fields = ["person", "monday"]

    list_filter = ["monday"]
    list_display = ["person", "monday"]


admin.site.register(Chapter)
admin.site.register(Comment)
admin.site.register(CompletedTask)
admin.site.register(Entry)
admin.site.register(ManualChange)
admin.site.register(Meeting)
admin.site.register(Organization)
admin.site.register(PersonTagChange)
admin.site.register(Section)


@admin.register(Session)
class SessionAdmin(admin.ModelAdmin):
    def _session_data(self, obj: Session):
        return obj.get_decoded()

    list_display = ["session_key", "_session_data", "expire_date"]
