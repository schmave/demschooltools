from django.contrib import admin

from custodia.models import (
    Excuse,
    Override,
    StudentRequiredMinutes,
    Swipe,
    Year,
)


@admin.register(Swipe)
class SwipeAdmin(admin.ModelAdmin):
    list_display = ["id", "person", "swipe_day", "in_time", "out_time"]
    list_filter = ["swipe_day", "person__organization"]
    search_fields = ["person__first_name", "person__last_name", "person__display_name"]

    autocomplete_fields = ["person"]


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
    list_display = [
        "id",
        "name",
        "organization",
        "from_time",
        "to_time",
        "inserted_date",
    ]
    list_filter = ["inserted_date", "organization"]
    readonly_fields = ["organization"]
