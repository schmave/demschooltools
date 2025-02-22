from django.contrib import admin

from custodia.models import (
    Excuse,
    Override,
    School,
    StudentRequiredMinutes,
    Swipe,
    Year,
)


@admin.register(School)
class SchoolAdmin(admin.ModelAdmin):
    list_display = ["id", "name", "timezone", "late_time", "use_display_name"]


@admin.register(Swipe)
class SwipeAdmin(admin.ModelAdmin):
    list_display = ["id", "person", "swipe_day", "in_time", "out_time"]
    list_filter = ["swipe_day", "person__organization"]

    readonly_fields = ["person"]


# @admin.register(Student)
# class StudentAdmin(admin.ModelAdmin):
#     list_display = ["id", "name", "school", "start_date", "is_teacher"]
#     list_filter = ["is_teacher", "school"]
#     readonly_fields = ["school", "person"]


@admin.register(StudentRequiredMinutes)
class StudentRequiredMinutesAdmin(admin.ModelAdmin):
    list_display = ["person", "fromdate", "required_minutes"]
    list_filter = ["person__organization"]
    readonly_fields = ["person"]


@admin.register(Override)
class OverrideAdmin(admin.ModelAdmin):
    list_display = ["id", "person", "inserted_date", "date"]
    readonly_fields = ["person"]
    list_filter = ("date", "inserted_date", "person__organization")


@admin.register(Excuse)
class ExcuseAdmin(admin.ModelAdmin):
    list_display = ["id", "person", "inserted_date", "date"]
    readonly_fields = ["person"]
    list_filter = ("date", "inserted_date", "person__organization")


@admin.register(Year)
class YearAdmin(admin.ModelAdmin):
    list_display = ["id", "name", "school", "inserted_date"]
    list_filter = ["inserted_date", "school"]
    readonly_fields = ["school"]
