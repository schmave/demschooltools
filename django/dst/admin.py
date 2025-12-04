from auditlog.mixins import AuditlogHistoryAdminMixin
from django.contrib import admin
from django.contrib.sessions.models import Session

from dst.models import (
    Account,
    AllowedIp,
    AttendanceCode,
    AttendanceDay,
    AttendanceRule,
    AttendanceWeek,
    Case,
    CaseMeeting,
    CaseReference,
    Chapter,
    Charge,
    ChargeReference,
    Comment,
    CompletedTask,
    Donation,
    Email,
    Entry,
    LinkedAccount,
    MailchimpSync,
    ManualChange,
    Meeting,
    NotificationRule,
    Organization,
    OrganizationHost,
    Person,
    PersonAtCase,
    PersonAtMeeting,
    PersonChange,
    PersonTagChange,
    PhoneNumber,
    Role,
    RoleRecord,
    RoleRecordMember,
    Section,
    Tag,
    Task,
    TaskList,
    Transaction,
    User,
    UserRole,
)


class DstAuditlogModelAdmin(AuditlogHistoryAdminMixin, admin.ModelAdmin):
    auditlog_history_per_page = 100
    change_form_template = "admin/change_form_with_auditlog.html"


def has_password(user: User) -> bool:
    return bool(user.password)


@admin.register(User)
class UserAdmin(DstAuditlogModelAdmin):
    list_display = ["id", "organization_id", "name", "email", has_password]
    list_filter = ["organization_id", "is_superuser", "is_staff"]
    search_fields = ["name", "email"]


@admin.register(Person)
class PersonAdmin(DstAuditlogModelAdmin):
    readonly_fields = ["id", "organization", "tags"]

    list_display = ["first_name", "last_name", "email", "organization"]
    list_filter = ["organization_id"]
    search_fields = ["first_name", "last_name", "display_name", "email"]

    autocomplete_fields = ["family_person"]


@admin.register(Tag)
class TagAdmin(DstAuditlogModelAdmin):
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


@admin.register(ManualChange)
class ManualChangeAdmin(admin.ModelAdmin):
    readonly_fields = ["entry", "chapter", "section", "date_entered"]
    list_display = ["id", "entry_id", "date_entered", "was_created", "was_deleted"]


@admin.register(AttendanceCode)
class AttendanceCodeAdmin(DstAuditlogModelAdmin):
    list_display = [
        "code",
        "description",
        "organization",
        "counts_toward_attendance",
        "not_counted",
    ]
    list_filter = ["organization_id"]
    search_fields = ["code", "description"]


@admin.register(AttendanceRule)
class AttendanceRuleAdmin(DstAuditlogModelAdmin):
    list_display = ["person", "organization", "category", "start_date", "end_date"]
    list_filter = ["organization_id", "category"]
    search_fields = ["person__first_name", "person__last_name"]


@admin.register(NotificationRule)
class NotificationRuleAdmin(DstAuditlogModelAdmin):
    list_display = ["email", "the_type", "tag", "organization"]
    list_filter = ["organization_id", "the_type"]
    search_fields = ["email"]


@admin.register(RoleRecord)
class RoleRecordAdmin(DstAuditlogModelAdmin):
    list_display = ["role_name", "date_created", "role"]
    search_fields = ["role_name"]


# Simple registrations for models without auditlog
admin.site.register(Account)
admin.site.register(AllowedIp)
admin.site.register(CaseMeeting)
admin.site.register(CaseReference)
admin.site.register(Chapter)
admin.site.register(ChargeReference)
admin.site.register(Comment)
admin.site.register(CompletedTask)
admin.site.register(Donation)
admin.site.register(Email)
admin.site.register(Entry)
admin.site.register(LinkedAccount)
admin.site.register(MailchimpSync)
admin.site.register(Meeting)
admin.site.register(Organization)
admin.site.register(OrganizationHost)
admin.site.register(PersonAtCase)
admin.site.register(PersonAtMeeting)
admin.site.register(PersonChange)
admin.site.register(PersonTagChange)
admin.site.register(PhoneNumber)
admin.site.register(Role)
admin.site.register(RoleRecordMember)
admin.site.register(Section)
admin.site.register(Task)
admin.site.register(TaskList)
admin.site.register(Transaction)
admin.site.register(UserRole)


@admin.register(Session)
class SessionAdmin(admin.ModelAdmin):
    def _session_data(self, obj: Session):
        return obj.get_decoded()

    list_display = ["session_key", "_session_data", "expire_date"]
