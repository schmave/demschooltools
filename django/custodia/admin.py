from django.contrib import admin

from custodia.models import (
    Excuse,
    Override,
    School,
    Student,
    StudentRequiredMinutes,
    Swipe,
    Year,
)


@admin.register(School)
class SchoolAdmin(admin.ModelAdmin):
    list_display = ["id", "name", "timezone", "late_time"]


@admin.register(Swipe)
class SwipeAdmin(admin.ModelAdmin):
    list_display = ["id", "student", "swipe_day", "in_time", "out_time"]
    list_filter = ["swipe_day", "student__school"]

    readonly_fields = ["student"]


@admin.register(Student)
class StudentAdmin(admin.ModelAdmin):
    list_display = ["id", "name", "school", "start_date", "is_teacher"]
    list_filter = ["is_teacher", "school"]
    readonly_fields = ["school", "person"]


@admin.register(StudentRequiredMinutes)
class StudentRequiredMinutesAdmin(admin.ModelAdmin):
    list_display = ["student", "fromdate", "required_minutes"]
    list_filter = ["student__school"]
    readonly_fields = ["student"]


@admin.register(Override)
class OverrideAdmin(admin.ModelAdmin):
    list_display = ["id", "student", "inserted_date", "date"]
    readonly_fields = ["student"]
    list_filter = ("date", "inserted_date", "student__school")


@admin.register(Excuse)
class ExcuseAdmin(admin.ModelAdmin):
    list_display = ["id", "student", "inserted_date", "date"]
    readonly_fields = ["student"]
    list_filter = ("date", "inserted_date", "student__school")


@admin.register(Year)
class YearAdmin(admin.ModelAdmin):
    list_display = ["id", "name", "school", "inserted_date"]
    list_filter = ["inserted_date", "school"]
    readonly_fields = ["school"]
