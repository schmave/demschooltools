import csv
import io
import json
import math
import zipfile
from collections import defaultdict
from datetime import date, datetime, time
from typing import Dict, List, Optional

from django import forms
from django.contrib.auth.mixins import LoginRequiredMixin
from django.http import HttpResponse
from django.template.loader import render_to_string
from django.views import View

from custodia.views import get_start_of_school_year
from dst.models import (
    AttendanceCode,
    AttendanceDay,
    AttendanceRule,  # noqa: F401
    AttendanceWeek,
    Organization,
    Person,
    Tag,
    UserRole,
)
from dst.org_config import get_org_config
from dst.utils import (
    DateToDatetimeField,
    DstHttpRequest,
    login_required,
    render_main_template,
)


class AttendanceView(LoginRequiredMixin, View):
    login_url = "/login"


class SignInSheetView(AttendanceView):
    def get(self, request: DstHttpRequest):
        if not request.user.hasRole(UserRole.ATTENDANCE):
            raise PermissionError("You don't have privileges to access this item")

        org = request.org

        # Get people with attendance tags (with their tags)
        people_with_tags = []
        for person in (
            Person.objects.filter(
                tags__show_in_attendance=True,
                tags__organization=org,
            )
            .distinct()
            .prefetch_related("tags")
            .order_by("display_name", "first_name")
        ):
            people_with_tags.append(
                {
                    "id": person.id,
                    "label": person.get_name() + " " + person.last_name,
                    "tags": [tag.id for tag in person.tags.all()],
                }
            )

        # Get attendance tags
        tags = []
        for tag in Tag.objects.filter(
            organization=org, show_in_attendance=True
        ).order_by("title"):
            tags.append(
                {
                    "id": tag.id,
                    "label": tag.title,
                }
            )

        # Get all people (for guest selection)
        all_people = []
        for person in (
            Person.objects.filter(organization=org, is_family=False)
            .exclude(first_name="", last_name="", display_name="")
            .only("display_name", "first_name", "last_name")
            .distinct()
        ):
            all_people.append(
                {
                    "id": person.id,
                    "label": person.get_name() + " " + person.last_name,
                }
            )
        all_people.sort(key=lambda x: x["label"])

        return render_main_template(
            request,
            "attendance",
            render_to_string(
                "sign_in_sheet.html",
                {
                    "people_json": json.dumps(people_with_tags),
                    "tags_json": json.dumps(tags),
                    "all_people_json": json.dumps(all_people),
                    "request": request,
                },
            ),
            "Sign in sheet",
            "sign_in_sheet",
        )


class AttendanceStats:
    def __init__(self, org: Organization, rules: List[AttendanceRule]):
        self.org = org
        self.absence_counts: Dict[Optional[AttendanceCode], int] = defaultdict(int)
        self.days_present = 0
        self.partial_days_present = 0
        self.approved_absences = 0
        self.unapproved_absences = 0
        self.total_hours = 0.0
        self.values: Dict[int, float] = {}  # Maps day index to attendance value
        self.partial_day_value = 0.0

        if org.attendance_partial_day_value:
            self.partial_day_value = float(org.attendance_partial_day_value)

        # Index the rules by person_id so that _get_current_attendance_rules is fast.
        self.person_to_rules = defaultdict(list)
        for rule in rules:
            self.person_to_rules[rule.person_id].append(rule)

    def process_day(
        self, day: AttendanceDay, day_index: int, codes_map: Dict[str, AttendanceCode]
    ):
        if day.code or not day.start_time or not day.end_time:
            self._increment_code_count(codes_map.get(day.code), day_index)
        else:
            self._increment_attendance(day, day_index)

    def _increment_code_count(self, code: Optional[AttendanceCode], index: int):
        if code is None:
            return

        if not code.not_counted:
            if code.counts_toward_attendance:
                self.approved_absences += 1
                self.values[index] = 1.0
            else:
                self.unapproved_absences += 1
                self.values[index] = 0.0

        self.absence_counts[code] += 1

    def _increment_attendance(self, day: AttendanceDay, index: int):
        hours = self._calculate_hours(day)
        self.total_hours += hours

        if self._is_partial_day(day, hours):
            self.partial_days_present += 1
            self.values[index] = self.partial_day_value
        else:
            self.days_present += 1
            self.values[index] = 1.0

    def _is_partial_day(self, day: AttendanceDay, hours: float) -> bool:
        """
        Determine if a day is partial based on Java AttendanceDay.isPartial() logic.
        Checks hours, start time, and end time against organization and rule-based thresholds.
        """
        if not self.org.attendance_enable_partial_days:
            return False

        # Get default values from organization
        min_hours = self.org.attendance_day_min_hours
        latest_start_time = self.org.attendance_day_latest_start_time
        earliest_departure_time = self.org.attendance_day_earliest_departure_time

        # Get current attendance rules for this person and day
        rules = self._get_current_attendance_rules(day)

        # Apply rule overrides (person-specific rules override org defaults)
        for rule in rules:
            if self._rule_matches_day(rule, day.day):
                if rule.min_hours is not None:
                    min_hours = rule.min_hours
                if rule.latest_start_time is not None:
                    latest_start_time = rule.latest_start_time
                if rule.earliest_departure_time is not None:
                    earliest_departure_time = rule.earliest_departure_time

        # Check the three partial day conditions
        if min_hours is not None and hours < min_hours:
            return True

        if latest_start_time is not None and day.start_time:
            if self._time_to_seconds(day.start_time) > self._time_to_seconds(
                latest_start_time
            ):
                return True

        if earliest_departure_time is not None and day.end_time:
            if self._time_to_seconds(day.end_time) < self._time_to_seconds(
                earliest_departure_time
            ):
                return True

        return False

    def _get_current_attendance_rules(self, day: AttendanceDay) -> List[AttendanceRule]:
        return [
            rule
            for rule in (
                self.person_to_rules[day.person_id] + self.person_to_rules[None]
            )
            if (
                (rule.start_date is None or rule.start_date <= day.day)
                and (rule.end_date is None or rule.end_date >= day.day)
            )
        ]

    def _rule_matches_day(self, rule: AttendanceRule, day_date: date) -> bool:
        """Check if an attendance rule matches the given day of week"""
        weekday = day_date.weekday()  # Monday=0, Sunday=6

        # Map Python weekday to rule fields
        day_fields = [
            rule.monday,  # 0 = Monday
            rule.tuesday,  # 1 = Tuesday
            rule.wednesday,  # 2 = Wednesday
            rule.thursday,  # 3 = Thursday
            rule.friday,  # 4 = Friday
            False,  # 5 = Saturday (not supported)
            False,  # 6 = Sunday (not supported)
        ]

        return weekday < 5 and day_fields[weekday]

    def _time_to_seconds(self, time_obj: time) -> int:
        """Convert time object to seconds since midnight"""
        return time_obj.hour * 3600 + time_obj.minute * 60 + time_obj.second

    def _calculate_hours(self, day: AttendanceDay) -> float:
        if not day.start_time or not day.end_time:
            return 0.0

        start_seconds = self._time_to_seconds(day.start_time)
        end_seconds = self._time_to_seconds(day.end_time)

        if end_seconds <= start_seconds:
            return 0.0

        hours = (end_seconds - start_seconds) / 3600.0
        off_hours = 0

        # Subtract off-campus time if applicable
        if day.off_campus_departure_time and day.off_campus_return_time:
            off_start = self._time_to_seconds(day.off_campus_departure_time)
            off_end = self._time_to_seconds(day.off_campus_return_time)

            off_hours = (off_end - off_start) / 3600.0

            if day.off_campus_minutes_exempted:
                off_hours -= day.off_campus_minutes_exempted / 60.0

            if off_hours < 0:
                off_hours = 0

        return max(0.0, hours - off_hours)

    def average_hours_per_day(self) -> float:
        total_days_attended = self.days_present + self.partial_days_present
        if total_days_attended == 0:
            return 0.0
        return self.total_hours / total_days_attended

    def attendance_rate(self) -> float:
        if not self.values:
            return 0.0
        total = sum(self.values.values())
        return total / len(self.values)

    def weighted_attendance_rate(self) -> float:
        total = 0.0
        reference_total = 0.0

        for index, value in self.values.items():
            total += self._weight_function(index, value)
            reference_total += self._weight_function(index, 1.0)

        return total / reference_total if reference_total > 0 else 0.0

    def _weight_function(self, index: int, value: float) -> float:
        # The way the weighted attendance rate works is that the present day is worth 100%,
        # and a long time in the past (i.e. several months ago) is worth close to 0%,
        # with a smooth transition in between.
        # reference_days is the number of school days in the past when the weight reaches 20%,
        # so you can adjust this to stretch or compress the curve. I've chosen 60 because
        # this is equivalent to 3 months.
        reference_days = 60.0
        curve_constant = (5.0 / (4.0 * reference_days)) ** 2

        # The curve is a Gaussian function, i.e. a bell curve, so near present day (index = 0)
        # the weight decreases slowly at first, then faster, then reaches an inflection point
        # and starts slowing down again, and finally has a long tail that goes out to infinity.
        return value * math.exp(-curve_constant * (index**2))


def parse_date_or_now(date_str: str) -> date:
    """Parse a date string or return today's date"""
    if not date_str:
        return date.today()

    try:
        return datetime.strptime(date_str, "%Y-%m-%d").date()
    except ValueError:
        return date.today()


def get_codes_map(
    include_no_school: bool, org: Organization
) -> Dict[str, AttendanceCode]:
    codes = {}

    for code in AttendanceCode.objects.filter(organization=org):
        codes[code.code] = code

    if include_no_school:
        # Create a virtual "No school" code
        no_school = AttendanceCode()
        no_school.description = "No school"
        no_school.color = "#cc9"
        no_school.code = "_NS_"
        codes["_NS_"] = no_school

    return codes


def map_people_to_stats(
    start_date: date, end_date: date, org: Organization
) -> Dict[int, AttendanceStats]:
    rules = list(AttendanceRule.objects.filter(organization=org))
    person_to_stats = defaultdict(lambda: AttendanceStats(org, rules))
    codes_map = get_codes_map(False, org)

    # Get all school days in the date range
    school_days = list_school_days(start_date, end_date, org)

    for day_index, school_day in enumerate(school_days):
        for day in school_day:
            person_to_stats[day.person_id].process_day(day, day_index, codes_map)

    # Add extra hours from attendance weeks
    weeks = AttendanceWeek.objects.filter(
        person__organization=org, monday__gte=start_date, monday__lte=end_date
    )

    for week in weeks:
        person_to_stats[week.person_id].total_hours += week.extra_hours

    return person_to_stats


def list_school_days(
    start_date: date, end_date: date, org: Organization
) -> List[List[AttendanceDay]]:
    """Get list of school days grouped by date"""
    days = AttendanceDay.objects.filter(
        person__organization=org, day__gte=start_date, day__lte=end_date
    ).order_by("-day")

    # Group by date
    groups = defaultdict(list)
    for day in days:
        groups[day.day].append(day)

    # Filter to only include actual school days
    school_days = []
    for day_date, day_list in groups.items():
        # Check if any attendance record indicates this was a school day
        for day in day_list:
            is_absence = day.code and day.code != "_NS_"
            is_attendance = day.start_time and day.end_time
            if is_absence or is_attendance:
                school_days.append(day_list)
                break

    return school_days


def get_attendance_people(org: Organization) -> List[Person]:
    """Get people tagged as being in Attendance"""
    return list(
        Person.objects.filter(
            organization=org, tags__show_in_attendance=True
        ).distinct()
    )


class AttendanceIndexForm(forms.Form):
    start_date = DateToDatetimeField(required=False)
    end_date = DateToDatetimeField(required=False)
    is_custom_date = forms.BooleanField(required=False)


@login_required()
def attendance_index(request: DstHttpRequest):
    """Main attendance index view"""
    if not request.user.hasRole(UserRole.ATTENDANCE):
        return HttpResponse("Access denied", status=403)

    form = AttendanceIndexForm(request.GET)
    assert form.is_valid()

    is_custom_date = form.cleaned_data["is_custom_date"]

    if form.cleaned_data["start_date"]:
        start_date = form.cleaned_data["start_date"]
        end_date = form.cleaned_data["end_date"]
    else:
        start_date = get_start_of_school_year()
        end_date = None

    if end_date is None:
        # Default to one year from start date
        end_date = date(start_date.year + 1, start_date.month, start_date.day)

    person_to_stats = map_people_to_stats(start_date, end_date, request.org)
    codes_map = get_codes_map(False, request.org)

    all_people_ids = list(person_to_stats.keys())
    all_people = Person.objects.filter(id__in=all_people_ids).order_by(
        "first_name", "last_name"
    )

    all_codes = list(codes_map.keys())
    current_people = get_attendance_people(request.org)

    prev_date = date(start_date.year - 1, start_date.month, start_date.day)
    next_date = date(start_date.year + 1, start_date.month, start_date.day)

    # Don't show next year if we're already at current year
    current_year_start = get_start_of_school_year()
    if start_date >= current_year_start:
        next_date = None

    org_config = get_org_config(request.org)

    return render_main_template(
        request,
        "attendance",
        render_to_string(
            "attendance_index.html",
            {
                "people": all_people,
                "person_to_stats": person_to_stats,
                "codes": all_codes,
                "codes_map": codes_map,
                "current_people": current_people,
                "start_date": start_date,
                "end_date": end_date,
                "is_custom_date": is_custom_date,
                "prev_date": prev_date,
                "next_date": next_date,
                "org_config": org_config,
            },
        ),
        "Attendance",
        "attendance_home",
    )


def get_daily_hours_file(
    all_dates: List[date], person_date_attendance: Dict[str, Dict[date, AttendanceDay]]
) -> bytes:
    csv_buffer = io.StringIO()
    writer = csv.writer(csv_buffer)

    # Write header
    header = ["Name"]
    for d in all_dates:
        header.append(d.strftime("%Y-%m-%d"))
    writer.writerow(header)

    # Write data for each person
    for name in sorted(person_date_attendance.keys()):
        row = [name]
        for date_val in all_dates:
            day = person_date_attendance[name].get(date_val)
            if day is None or not day.start_time or not day.end_time:
                # Show absence code or empty string
                row.append(day.code if day and day.code else "")
            else:
                # Calculate and show hours
                hours = _calculate_day_hours(day)
                row.append(f"{hours:.2f}")
        writer.writerow(row)

    # Add BOM for Excel compatibility
    csv_content = "\ufeff" + csv_buffer.getvalue()
    return csv_content.encode("utf-8")


def get_daily_signins_file(
    all_dates: List[date],
    person_date_attendance: Dict[str, Dict[date, AttendanceDay]],
    org_config,
) -> bytes:
    """Generate daily sign-ins CSV file matching Java implementation"""
    csv_buffer = io.StringIO()
    writer = csv.writer(csv_buffer)

    # Write header
    header = ["Name"]
    for d in all_dates:
        header.append(d.strftime("%Y-%m-%d"))
    writer.writerow(header)

    # Write data for each person (2 rows per person: sign-in times, then sign-out times)
    for name in sorted(person_date_attendance.keys()):
        # Row 1: Sign-in times
        row = [name]
        for date_val in all_dates:
            day = person_date_attendance[name].get(date_val)
            if day is None or not day.start_time or not day.end_time:
                # Show absence code or empty string
                row.append(day.code if day and day.code else "")
            else:
                # Show start time in HH:mm format
                row.append(day.start_time.strftime("%H:%M"))
        writer.writerow(row)

        # Row 2: Sign-out times (empty name)
        row = [""]
        for date_val in all_dates:
            day = person_date_attendance[name].get(date_val)
            if day is None or not day.start_time or not day.end_time:
                # Show absence code or empty string
                row.append(day.code if day and day.code else "")
            else:
                # Show end time in HH:mm format
                row.append(day.end_time.strftime("%H:%M"))
        writer.writerow(row)

    # Add BOM for Excel compatibility
    csv_content = "\ufeff" + csv_buffer.getvalue()
    return csv_content.encode("utf-8")


def _calculate_day_hours(day: AttendanceDay) -> float:
    """Calculate hours for a single day (helper function) - matches Java getHours() exactly"""
    if not day.start_time or not day.end_time:
        return 0.0

    # Convert to milliseconds to match Java Time.getTime() behavior
    start_millis = (
        day.start_time.hour * 3600000
        + day.start_time.minute * 60000
        + day.start_time.second * 1000
        + day.start_time.microsecond // 1000
    )
    end_millis = (
        day.end_time.hour * 3600000
        + day.end_time.minute * 60000
        + day.end_time.second * 1000
        + day.end_time.microsecond // 1000
    )

    if end_millis <= start_millis:
        return 0.0

    # Calculate off-campus time in milliseconds
    off_campus_time = 0
    if day.off_campus_departure_time and day.off_campus_return_time:
        off_start_millis = (
            day.off_campus_departure_time.hour * 3600000
            + day.off_campus_departure_time.minute * 60000
            + day.off_campus_departure_time.second * 1000
            + day.off_campus_departure_time.microsecond // 1000
        )
        off_end_millis = (
            day.off_campus_return_time.hour * 3600000
            + day.off_campus_return_time.minute * 60000
            + day.off_campus_return_time.second * 1000
            + day.off_campus_return_time.microsecond // 1000
        )

        off_campus_time = off_end_millis - off_start_millis

        # Subtract exempted minutes (convert to milliseconds)
        if day.off_campus_minutes_exempted:
            off_campus_time -= day.off_campus_minutes_exempted * 60 * 1000
            if off_campus_time < 0:
                off_campus_time = 0

    # Match Java calculation exactly: (endTime.getTime() - startTime.getTime() - off_campus_time) / (1000.0 * 60 * 60)
    return (end_millis - start_millis - off_campus_time) / (1000.0 * 60 * 60)


@login_required()
def attendance_download(request: DstHttpRequest):
    """Download attendance data as CSV/ZIP"""
    if not request.user.hasRole(UserRole.ATTENDANCE):
        return HttpResponse("Access denied", status=403)

    start_date_str = request.GET.get("start_date", "")
    start_date = (
        parse_date_or_now(start_date_str)
        if start_date_str
        else get_start_of_school_year()
    )
    end_date = date(start_date.year + 1, start_date.month, start_date.day)

    # Get all attendance days
    days = (
        AttendanceDay.objects.filter(
            person__organization=request.org, day__gte=start_date, day__lte=end_date
        )
        .select_related("person")
        .order_by("day", "person__first_name")
    )

    # Get all attendance weeks
    weeks = AttendanceWeek.objects.filter(
        person__organization=request.org,
        monday__gte=start_date,
        monday__lte=end_date,
    ).select_related("person")

    org_config = get_org_config(request.org)

    # Create ZIP file in memory
    zip_buffer = io.BytesIO()

    with zipfile.ZipFile(zip_buffer, "w", zipfile.ZIP_DEFLATED) as zip_file:
        # File 1: Main attendance data (all_data.csv)
        csv_buffer = io.StringIO()
        writer = csv.writer(csv_buffer)

        # Write header
        writer.writerow(
            [
                "Name",
                "Day",
                "Absence code",
                "Arrival time",
                "Departure time",
                "Extra hours",
            ]
        )

        # Write attendance days
        for day in days:
            name = f"{day.person.first_name} {day.person.last_name}"
            day_str = day.day.strftime("%Y-%m-%d")

            if day.code:
                writer.writerow([name, day_str, day.code, "", "", ""])
            else:
                start_time = (
                    day.start_time.strftime("%H:%M:%S") if day.start_time else ""
                )
                end_time = day.end_time.strftime("%H:%M:%S") if day.end_time else ""
                writer.writerow([name, day_str, "", start_time, end_time, ""])

        # Write attendance weeks (extra hours)
        for week in weeks:
            name = f"{week.person.first_name} {week.person.last_name}"
            monday_str = week.monday.strftime("%Y-%m-%d")
            writer.writerow([name, monday_str, "", "", "", str(week.extra_hours)])

        # Add CSV to ZIP with BOM for Excel compatibility
        csv_content = "\ufeff" + csv_buffer.getvalue()
        zip_file.writestr("attendance/all_data.csv", csv_content.encode("utf-8"))

        # Prepare data for the other two files
        all_dates = sorted(set(day.day for day in days))
        person_date_attendance = defaultdict(dict)

        for day in days:
            name = f"{day.person.first_name} {day.person.last_name}"
            person_date_attendance[name][day.day] = day

        # File 2: Daily hours matrix (daily_hours.csv)
        daily_hours_content = get_daily_hours_file(all_dates, person_date_attendance)
        zip_file.writestr("attendance/daily_hours.csv", daily_hours_content)

        # File 3: Daily sign-ins matrix (daily_signins.csv)
        daily_signins_content = get_daily_signins_file(
            all_dates, person_date_attendance, org_config
        )
        zip_file.writestr("attendance/daily_signins.csv", daily_signins_content)

    zip_buffer.seek(0)

    response = HttpResponse(zip_buffer.getvalue(), content_type="application/zip")
    response["Content-Disposition"] = "attachment; filename=attendance.zip"

    return response
