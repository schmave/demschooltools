from collections import defaultdict
from datetime import date, datetime, tzinfo

import pytz
from django.contrib.auth import authenticate, login, logout
from django.contrib.auth.views import redirect_to_login
from django.http import HttpResponse
from django.shortcuts import redirect, render
from django.views import View
from rest_framework.request import Request
from rest_framework.response import Response
from rest_framework.views import APIView

from custodia.models import Override, School, Student, Swipe

# TODO: Allow per-student overrides to this (if schools are using it) via
# the students_required_minutes table.

# TODO? This used to be configurable via the classes table, but
# even PFS hasn't configured it. Maybe make this a DST setting for Custodia.
DEFAULT_REQUIRED_MINUTES = 345


class IndexView(View):
    def get(self, request):
        if not request.user.is_authenticated:
            return redirect_to_login("")

        return HttpResponse(open("static/index.html").read())


class LoginView(View):
    def get(self, request):
        return render(request, "login.html")

    def post(self, request):
        user = authenticate(
            request,
            username=request.POST["username"],
            password=request.POST["password"],
        )
        if user is not None:
            login(request, user)
            return redirect("/")

        return redirect_to_login("")


def student_to_dict(student: Student, last_swipe: Swipe | None, local_now: datetime):
    return {
        "_id": student.id,
        "name": student.name,
        "last_swipe_type": "in"
        if last_swipe and last_swipe.out_time is None
        else "out",
        # TODO
        "swiped_today_late": False,
        "is_teacher": student.is_teacher,
        "in_today": last_swipe and last_swipe.swipe_day == local_now.date(),
        # TODO
        "late_time": None,
        "absent_today": student.show_as_absent == local_now.date(),
        "last_swipe_date": last_swipe and format_date(last_swipe.swipe_day),
    }


class SwipeView(APIView):
    def post(self, request: Request, student_id: int):
        student = Student.objects.get(id=student_id)

        direction = request.data["direction"]  # type: ignore

        school: School = request.user.school
        local_now = datetime.now(pytz.timezone(school.timezone))

        if direction == "in":
            swipe = Swipe.objects.create(
                student=student,
                swipe_day=local_now.date(),
                in_time=datetime.utcnow(),
            )
        elif direction == "out":
            swipe = Swipe.objects.get(
                student=student, swipe_day=local_now.date(), out_time=None
            )
            swipe.out_time = datetime.utcnow()
            swipe.save()
        else:
            assert False, "invalid direction"

        return Response(student_to_dict(student, swipe, local_now))


class DeleteSwipeView(APIView):
    def post(self, request: Request, student_id: int):
        swipe_id = request.data["swipe"]["_id"]  # type: ignore
        Swipe.objects.get(id=swipe_id).delete()
        return student_data_view(request, student_id)


class LogoutView(View):
    def get(self, request):
        if request.user.is_authenticated:
            logout(request)
        return redirect("/")


class IsAdminView(APIView):
    def get(self, request: Request):
        school: School = request.user.school
        return Response(
            {
                "admin": "overseer.roles/admin"
                if "overseer.roles/admin" in request.user.roles
                else None,
                "school": {
                    "_id": school.id,
                    "name": school.name,
                    "timezone": school.timezone,
                    "use_display_name": school.use_display_name,
                },
            }
        )


def format_date(dt: date) -> str:
    return dt.strftime("%Y-%m-%d")


def format_time(dt: datetime | None, timezone: tzinfo) -> str:
    if dt:
        return dt.astimezone(timezone).strftime("%I:%M")
    else:
        return ""


class StudentsTodayView(APIView):
    def get(self, request: Request):
        school = request.user.school
        tz = pytz.timezone(school.timezone)
        now = datetime.now(tz)

        student_infos = []

        for student in Student.objects.filter(person__tags__show_in_attendance=True):
            last_swipe = Swipe.objects.filter(student=student).order_by("-id").first()
            student_infos.append(student_to_dict(student, last_swipe, now))

        return Response(
            {
                "today": format_date(now),
                "students": student_infos,
            }
        )


def get_start_of_school_year(school_timezone: tzinfo) -> date:
    local_now = datetime.now(school_timezone)
    result = date(local_now.year, 8, 1)
    if result > local_now.date():
        return date(local_now.year - 1, 1, 1)
    return result


class StudentDataView(APIView):
    def get(self, request: Request, student_id: int):
        return student_data_view(request, student_id)


def student_data_view(request: Request, student_id: int) -> Response:
    student = Student.objects.get(id=student_id)
    school: School = request.user.school
    school_timezone = pytz.timezone(school.timezone)
    local_now = datetime.now(school_timezone)
    override_days = set(
        Override.objects.filter(student=student).values_list("date", flat=True)
    )

    year_start = get_start_of_school_year(school_timezone)
    day_to_swipes: dict[date, list[Swipe]] = defaultdict(list)
    for swipe in Swipe.objects.filter(student=student, swipe_day__gte=year_start):
        day_to_swipes[swipe.swipe_day].append(swipe)

    day_to_swipes = dict(day_to_swipes)

    school_days = sorted(
        Swipe.objects.filter(swipe_day__gte=year_start)
        .values_list("swipe_day", flat=True)
        .distinct(),
        reverse=True,
    )

    day_data = []
    total_seconds = 0
    for day in school_days:
        day_seconds = 0
        is_override = day in override_days

        swipe_data = []
        missing_swipe = False
        this_days_swipes = day_to_swipes.get(day, [])
        for swipe in this_days_swipes:
            swipe_data.append(
                {
                    "_id": swipe.id,
                    "day": format_date(day),
                    "nice_in_time": format_time(swipe.in_time, school_timezone),
                    "nice_out_time": format_time(swipe.out_time, school_timezone),
                    "student_id": student.id,
                    "out_time": swipe.out_time and swipe.out_time.isoformat(),
                    "in_time": swipe.in_time and swipe.in_time.isoformat(),
                }
            )
            if swipe.in_time and not swipe.out_time:
                missing_swipe = True

            if swipe.in_time and swipe.out_time:
                day_seconds += (swipe.out_time - swipe.in_time).total_seconds()

        is_short = day_seconds < DEFAULT_REQUIRED_MINUTES * 60
        day_data.append(
            {
                "absent": day not in day_to_swipes,
                "override": is_override,
                "day": format_date(day),
                "total_mins": DEFAULT_REQUIRED_MINUTES
                if is_override
                else (day_seconds // 60),
                "swipes": swipe_data,
                "short": is_short,
                "valid": not is_short
                and not missing_swipe
                and len(this_days_swipes) > 0,
                # -------TODO---------
                "excused": False,
            }
        )

        total_seconds += day_seconds

    return Response(
        {
            "student": {
                "_id": student.id,
                "absent_today": student.show_as_absent == local_now.date(),
                "days": day_data,
                "name": student.name,
                "required_minutes": DEFAULT_REQUIRED_MINUTES,
                "today": format_date(local_now.date()),
                "total_hours": total_seconds / 3600,
                "is_teacher": student.is_teacher,
                "last_swipe_date": format_date(max(day_to_swipes)),
                # -------- TODO: -----------
                "last_swipe_type": "out",
                "in_today": False,
                "total_abs": 0,
                "total_days": 0,
                "total_excused": 0,
                "total_overrides": 0,
                "total_short": 0,
            }
        }
    )
