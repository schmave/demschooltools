import csv
import io
import math
import zipfile
from collections import defaultdict
from datetime import date, datetime
from typing import Dict, List, Optional

from django.contrib.auth.decorators import login_required as django_login_required
from django.http import HttpRequest, HttpResponse
from django.shortcuts import render
from django.template.loader import render_to_string
from django.utils.safestring import mark_safe

from custodia.views import get_start_of_school_year
from dst.models import (
    AttendanceCode,
    AttendanceDay,
    AttendanceWeek,
    Organization,
    Person,
    UserRole,
)
from dst.org_config import get_org_config


def login_required():
    return django_login_required(login_url="/login")


class DstHttpRequest(HttpRequest):
    org: Organization


def render_main_template(
    request: DstHttpRequest,
    content: str,
    title: str,
    selected_button: str | None = None,
):
    return render(
        request,
        "main.html",
        {
            "title": title,
            "menu": "attendance",
            "selectedBtn": selected_button,
            "content": mark_safe(content),
            "current_username": request.user.email
            if request.user.is_authenticated
            else None,
            "is_user_logged_in": request.user.is_authenticated,
            "org_config": get_org_config(request.org),
        },
    )


class AttendanceStats:
    def __init__(self, org: Organization):
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

    def process_day(
        self, day: AttendanceDay, day_index: int, codes_map: Dict[str, AttendanceCode]
    ):
        """Process a single attendance day and update statistics"""
        if day is None:
            return

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
        if not self.org.attendance_day_min_hours:
            return False
        return hours < self.org.attendance_day_min_hours

    def _calculate_hours(self, day: AttendanceDay) -> float:
        if not day.start_time or not day.end_time:
            return 0.0

        start_seconds = (
            day.start_time.hour * 3600
            + day.start_time.minute * 60
            + day.start_time.second
        )
        end_seconds = (
            day.end_time.hour * 3600 + day.end_time.minute * 60 + day.end_time.second
        )

        if end_seconds <= start_seconds:
            return 0.0

        hours = (end_seconds - start_seconds) / 3600.0

        # Subtract off-campus time if applicable
        if day.off_campus_departure_time and day.off_campus_return_time:
            off_start = (
                day.off_campus_departure_time.hour * 3600
                + day.off_campus_departure_time.minute * 60
                + day.off_campus_departure_time.second
            )
            off_end = (
                day.off_campus_return_time.hour * 3600
                + day.off_campus_return_time.minute * 60
                + day.off_campus_return_time.second
            )

            if off_end > off_start:
                off_hours = (off_end - off_start) / 3600.0
                hours -= off_hours

            # Add back exempted minutes
            if day.off_campus_minutes_exempted:
                hours += day.off_campus_minutes_exempted / 60.0

        return max(0.0, hours)

    def average_hours_per_day(self) -> float:
        if self.days_present == 0:
            return 0.0
        return self.total_hours / self.days_present

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
    """Get mapping of attendance codes"""
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
) -> Dict[Person, AttendanceStats]:
    """Create mapping of people to their attendance statistics"""
    person_to_stats = {}
    codes_map = get_codes_map(False, org)

    # Get all school days in the date range
    school_days = list_school_days(start_date, end_date, org)

    for day_index, school_day in enumerate(school_days):
        for day in school_day:
            if day.person not in person_to_stats:
                person_to_stats[day.person] = AttendanceStats(org)

            stats = person_to_stats[day.person]
            stats.process_day(day, day_index, codes_map)

    # Add extra hours from attendance weeks
    weeks = AttendanceWeek.objects.filter(
        person__organization=org, monday__gte=start_date, monday__lte=end_date
    )

    for week in weeks:
        if week.person not in person_to_stats:
            person_to_stats[week.person] = AttendanceStats(org)

        person_to_stats[week.person].total_hours += week.extra_hours

    return person_to_stats


def list_school_days(
    start_date: date, end_date: date, org: Organization
) -> List[List[AttendanceDay]]:
    """Get list of school days grouped by date"""
    days = (
        AttendanceDay.objects.filter(
            person__organization=org, day__gte=start_date, day__lte=end_date
        )
        .select_related("person")
        .order_by("-day")
    )

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
    """Get list of people who have attendance records"""
    return list(
        Person.objects.filter(organization=org, attendanceday__isnull=False)
        .distinct()
        .order_by("first_name", "last_name")
    )


@login_required()
def attendance_index(request: DstHttpRequest):
    """Main attendance index view"""
    if not request.user.hasRole(UserRole.ATTENDANCE):
        return HttpResponse("Access denied", status=403)

    # Parse parameters
    start_date_str = request.GET.get("start_date", "")
    end_date_str = request.GET.get("end_date", "")
    is_custom_date = request.GET.get("is_custom_date") == "true"

    if not start_date_str:
        start_date = get_start_of_school_year()
        end_date = None
        is_custom_date = False
    else:
        start_date = parse_date_or_now(start_date_str)
        end_date = parse_date_or_now(end_date_str) if end_date_str else None

    if end_date is None:
        # Default to one year from start date
        end_date = date(start_date.year + 1, start_date.month, start_date.day)

    # Get data
    person_to_stats = map_people_to_stats(start_date, end_date, request.org)
    codes_map = get_codes_map(False, request.org)

    all_people = list(person_to_stats.keys())
    all_people.sort(key=lambda p: p.first_name)

    all_codes = list(codes_map.keys())
    current_people = get_attendance_people(request.org)

    # Calculate navigation dates
    prev_date = date(start_date.year - 1, start_date.month, start_date.day)
    next_date = date(start_date.year + 1, start_date.month, start_date.day)

    # Don't show next year if we're already at current year
    current_year_start = get_start_of_school_year()
    if start_date >= current_year_start:
        next_date = None

    org_config = get_org_config(request.org)

    return render_main_template(
        request,
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
    all_dates: List[date],
    person_date_attendance: Dict[str, Dict[date, AttendanceDay]],
    org_config,
) -> bytes:
    """Generate daily hours CSV file matching Java implementation"""
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
    """Calculate hours for a single day (helper function)"""
    if not day.start_time or not day.end_time:
        return 0.0

    start_seconds = (
        day.start_time.hour * 3600 + day.start_time.minute * 60 + day.start_time.second
    )
    end_seconds = (
        day.end_time.hour * 3600 + day.end_time.minute * 60 + day.end_time.second
    )

    if end_seconds <= start_seconds:
        return 0.0

    hours = (end_seconds - start_seconds) / 3600.0

    # Subtract off-campus time if applicable
    if day.off_campus_departure_time and day.off_campus_return_time:
        off_start = (
            day.off_campus_departure_time.hour * 3600
            + day.off_campus_departure_time.minute * 60
            + day.off_campus_departure_time.second
        )
        off_end = (
            day.off_campus_return_time.hour * 3600
            + day.off_campus_return_time.minute * 60
            + day.off_campus_return_time.second
        )

        if off_end > off_start:
            off_hours = (off_end - off_start) / 3600.0
            hours -= off_hours

        # Add back exempted minutes
        if day.off_campus_minutes_exempted:
            hours += day.off_campus_minutes_exempted / 60.0

    return max(0.0, hours)


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
        daily_hours_content = get_daily_hours_file(
            all_dates, person_date_attendance, org_config
        )
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
