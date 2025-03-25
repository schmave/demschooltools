from collections import defaultdict
from datetime import date, datetime, time, timedelta
from typing import Type

import requests
from django.conf import settings
from django.contrib.auth import logout
from django.contrib.auth.views import redirect_to_login
from django.db.models import Model
from django.db.transaction import atomic
from django.http import HttpRequest
from django.shortcuts import redirect, render
from django.utils import timezone
from django.views import View
from rest_framework.exceptions import NotFound, PermissionDenied
from rest_framework.generics import QuerySet
from rest_framework.permissions import BasePermission
from rest_framework.request import Request
from rest_framework.response import Response
from rest_framework.views import APIView

from custodia.models import (
    Excuse,
    Organization,
    Override,
    StudentRequiredMinutes,
    Swipe,
    Year,
)
from dst.models import AttendanceDay, AttendanceWeek, Person, User, UserRole

DEFAULT_REQUIRED_MINUTES = 345


class IndexView(View):
    def get(self, request):
        if not request.user.is_authenticated:
            return redirect_to_login("")

        return render(
            request,
            "index.html",
            {
                "dev_js_link": settings.CUSTODIA_JS_LINK,
                "rollbar_environment": settings.ROLLBAR_ENVIRONMENT,
            },
        )


class LoginView(View):
    def get(self, request):
        return render(request, "login.html")

    def post(self, request: HttpRequest):
        login_response = requests.post(
            ("http://" if settings.DEBUG else "https://")
            + request.get_host()
            + "/login",
            data={
                "email": request.POST["username"],
                "password": request.POST["password"],
            },
            allow_redirects=False,
        )

        if (
            login_response.status_code == 303
            and login_response.headers.get("Location") != "/login"
        ):
            # successful login
            response = redirect("/custodia")
            for cookie in login_response.cookies:
                response.set_cookie(
                    key=cookie.name,
                    value=cookie.value,  # type: ignore
                )
                return response

        return redirect_to_login("")


class LogoutView(View):
    def get(self, request: HttpRequest):
        if request.user.is_authenticated:
            logout(request)

        response = redirect(settings.LOGIN_URL)
        response.set_cookie("PLAY_SESSION", "", max_age=0)
        return response


def is_custodia_admin(user: User) -> bool:
    return user.hasRole(UserRole.ATTENDANCE)


class RequireAdmin(BasePermission):
    message = "You must have the permission to modify attendance records"

    def has_permission(self, request, view):  # type: ignore
        return is_custodia_admin(request.user)


def student_to_dict(
    student: Person,
    org: Organization,
    last_swipe: Swipe | None,
    in_today_time: datetime | None,
    is_teacher: bool,
):
    today = timezone.localdate()
    if in_today_time:
        in_today_time = timezone.localtime(in_today_time)
    in_today = last_swipe and last_swipe.swipe_day == today
    return {
        "_id": student.id,
        "name": student.get_name(),
        "last_swipe_type": (
            "in" if last_swipe and last_swipe.out_time is None else "out"
        ),
        "swiped_today_late": (in_today_time and in_today_time.time() > org.late_time),
        "is_teacher": is_teacher,
        "in_today": in_today,
        "absent_today": student.custodia_show_as_absent == today,
        "last_swipe_date": last_swipe and format_date(last_swipe.swipe_day),
    }


def get_person(request: Request, person_id: int):
    if getattr(request, "org") is None:
        raise PermissionDenied()

    person = Person.objects.filter(id=person_id, organization=request.org).first()
    if person is None:
        raise NotFound()

    return person


class AbsentView(APIView):
    def post(self, request: Request, person_id: int) -> Response:
        student = get_person(request, person_id)
        student.custodia_show_as_absent = timezone.localdate()
        student.save()

        return student_data_view(person_id, request.org)


def excuse_override_view(
    request: Request, person_id: int, model: Type[Model]
) -> Response:
    student = get_person(request, person_id)
    date: str = request.data["day"]  # type: ignore
    undo: bool = request.data["undo"]  # type: ignore

    filter_args = dict(person=student, date=date)
    if undo:
        for obj in model.objects.filter(**filter_args):
            obj.delete()
    else:
        model.objects.create(**filter_args)

    return student_data_view(person_id, request.org)


class ExcuseView(APIView):
    permission_classes = [RequireAdmin]

    @atomic
    def post(self, request: Request, person_id: int) -> Response:
        return excuse_override_view(request, person_id, Excuse)


class OverrideView(APIView):
    permission_classes = [RequireAdmin]

    @atomic
    def post(self, request: Request, person_id: int) -> Response:
        return excuse_override_view(request, person_id, Override)


def get_previous_monday(given_date: date) -> date:
    days_to_subtract = (given_date.weekday() - 0) % 7
    previous_monday = given_date - timedelta(days=days_to_subtract)
    return previous_monday


def sync_custodia_and_dst(person: Person, day: date):
    # The Attendance tab can't handle Saturday or Sunday sign-ins, so ignore them.
    if day.weekday() in (5, 6):
        return

    swipes = Swipe.objects.filter(person=person, swipe_day=day)
    min_in = min((x.in_time for x in swipes), default=None)
    max_out = max((x.out_time for x in swipes if x.out_time), default=None)

    monday = get_previous_monday(day)
    unused_week, created_week = AttendanceWeek.objects.get_or_create(
        monday=monday, person=person
    )

    if created_week:
        for i in range(5):
            AttendanceDay.objects.create(person=person, day=monday + timedelta(days=i))

    attendance_day = AttendanceDay.objects.get(person=person, day=day)
    attendance_day.start_time = min_in and timezone.localtime(min_in).timetz()
    attendance_day.end_time = max_out and timezone.localtime(max_out).timetz()
    attendance_day.save()


class SwipeView(APIView):
    @atomic
    def post(self, request: Request, person_id: int) -> Response:
        person = get_person(request, person_id)

        direction = request.data["direction"]  # type: ignore

        swipe_time = timezone.localtime()

        if request.data.get("overrideDate"):  # type: ignore
            the_date = datetime.fromisoformat(request.data["overrideDate"])  # type: ignore
            the_time = time.fromisoformat(
                request.data["overrideTime"]  # type: ignore
            )
            swipe_time = timezone.make_aware(
                the_date.replace(hour=the_time.hour, minute=the_time.minute)
            )

        swipe_filter = dict(person=person, swipe_day=swipe_time.date())
        if direction == "in":
            # Check to see if the user has somehow already swiped in for the day.
            # This will prevent there from being two swipes for the same person/day
            # where there is an in time but no out time.
            has_in_swipe = Swipe.objects.filter(**swipe_filter, out_time=None).exists()
            if not has_in_swipe:
                Swipe.objects.create(**swipe_filter, in_time=swipe_time)
        elif direction == "out":
            swipe = Swipe.objects.filter(**swipe_filter, out_time=None).first()
            # The user may have already swiped out, in which case there is nothing more to do.
            if swipe:
                swipe.out_time = swipe_time
                swipe.save()
        else:
            assert False, "invalid direction"

        sync_custodia_and_dst(person, swipe_time.date())

        return StudentsTodayView().get(request)


class DeleteSwipeView(APIView):
    permission_classes = [RequireAdmin]

    @atomic
    def post(self, request: Request, person_id: int) -> Response:
        student = get_person(request, person_id)
        swipe_id = request.data["swipe"]["_id"]  # type: ignore
        swipe = Swipe.objects.get(id=swipe_id, person=student)
        swipe.delete()
        sync_custodia_and_dst(student, swipe.swipe_day)
        return student_data_view(student.id, request.org)


class IsAdminView(APIView):
    def get(self, request: Request) -> Response:
        org: Organization = request.org
        user: User = request.user
        return Response(
            {
                "admin": "overseer.roles/admin" if is_custodia_admin(user) else None,
                "school": {
                    "_id": org.id,
                    "name": org.name,
                    "timezone": org.timezone,
                },
            }
        )


def format_date(dt: date) -> str:
    return dt.strftime("%Y-%m-%d")


def format_time(dt: datetime | None) -> str:
    if dt:
        return timezone.localtime(dt).strftime("%-I:%M %p").lower()
    else:
        return ""


class StudentsTodayView(APIView):
    def get(self, request: Request):
        org = request.org
        today = timezone.localdate()

        student_infos = []

        people = list(
            Person.objects.filter(
                tags__show_in_attendance=True,
                tags__organization=org,
            ).distinct()
        )

        person_to_max_swipe: dict[int, Swipe] = {}
        for swipe in Swipe.objects.filter(
            person__in=people,
            # We will only try to fill in missing swipes if the missing swipe
            # happened in the last 10 days.
            swipe_day__gt=today - timedelta(days=10),
        ):
            if (
                swipe.person_id not in person_to_max_swipe
                or person_to_max_swipe[swipe.person_id].in_time < swipe.in_time
            ):
                person_to_max_swipe[swipe.person_id] = swipe

        student_ids = set(
            Person.objects.filter(
                id__in=[x.id for x in people],
                tags__organization=org,
                tags__use_student_display=True,
            ).values_list("id", flat=True)
        )

        student_to_in_time: dict[int, datetime] = {}
        for person_id, in_time in (
            Swipe.objects.filter(
                person__in=people,
                swipe_day=today,
            )
            .order_by("in_time")
            .values_list("person_id", "in_time")
        ):
            if person_id not in student_to_in_time:
                student_to_in_time[person_id] = in_time

        for person in people:
            last_swipe = person_to_max_swipe.get(person.id)
            student_infos.append(
                student_to_dict(
                    person,
                    org,
                    last_swipe,
                    student_to_in_time.get(person.id),
                    person.id not in student_ids,
                )
            )

        return Response(
            {
                "students": student_infos,
            }
        )


def get_start_of_school_year() -> datetime:
    local_now = timezone.localtime()
    result = timezone.make_aware(datetime(local_now.year, 8, 1))
    if result > local_now:
        return timezone.make_aware(datetime(local_now.year - 1, 8, 1))
    return result


class StudentDataView(APIView):
    @atomic
    def put(self, request: Request, person_id: int) -> Response:
        student = get_person(request, person_id)
        start_date_str: str
        if start_date_str := request.data["start_date"]:  # type: ignore
            student.custodia_start_date = datetime.strptime(
                start_date_str, "%Y-%m-%d"
            ).date()

        StudentRequiredMinutes.objects.update_or_create(
            person=student,
            fromdate=timezone.localdate(),
            defaults=dict(required_minutes=request.data["minutes"]),  # type: ignore
        )

        student.save()

        return student_data_view(student.id, request.org)

    def get(self, request: Request, person_id: int) -> Response:
        get_person(request, person_id)  # just call this for auth
        return student_data_view(person_id, request.org)


def get_year_start_end(year: Year | None) -> tuple[datetime, datetime]:
    if year:
        return (year.from_time, year.to_time)
    return (
        get_start_of_school_year(),
        datetime(3000, 1, 1),
    )


def get_school_days(org: Organization, start: datetime, end: datetime) -> list[date]:
    return sorted(
        Swipe.objects.filter(
            person__organization=org,
            swipe_day__gte=start.date(),
            swipe_day__lte=end.date(),
        )
        .values_list("swipe_day", flat=True)
        .distinct(),
    )


def student_data_view(
    person_id: int, org: Organization, year: Year | None = None
) -> Response:
    return Response(
        {
            "student": get_student_batch_data(
                Person.objects.filter(id=person_id, organization=org),
                year,
                org,
                # Pass include_all=True so that in case this student has no swipes, they are
                # still included in the output.
                include_all=True,
            )[person_id]
        }
    )


def get_student_batch_data(
    students: QuerySet[Person],
    year: Year | None,
    org: Organization,
    include_all: bool = False,
) -> dict[int, dict]:
    year_start, year_end = get_year_start_end(year)
    school_days = get_school_days(org, year_start, year_end)

    overrides: dict[int, set[date]] = defaultdict(set)
    for person_id, the_date in Override.objects.filter(person__in=students).values_list(
        "person_id", "date"
    ):
        overrides[person_id].add(the_date)

    excuses: dict[int, set[date]] = defaultdict(set)
    for person_id, the_date in Excuse.objects.filter(person__in=students).values_list(
        "person_id", "date"
    ):
        excuses[person_id].add(the_date)

    swipes: dict[int, dict[date, list[Swipe]]] = defaultdict(lambda: defaultdict(list))

    for swipe in Swipe.objects.filter(
        person__in=students, swipe_day__gte=year_start, swipe_day__lte=year_end
    ):
        swipes[swipe.person_id][swipe.swipe_day].append(swipe)

    required_minutes: dict[int, list[StudentRequiredMinutes]] = defaultdict(list)
    for srm in StudentRequiredMinutes.objects.filter(person__in=students).order_by(
        "fromdate"
    ):
        required_minutes[srm.person_id].append(srm)

    if include_all:
        all_ids = students.values_list("id", flat=True)
    else:
        all_ids = set(
            overrides.keys() | excuses.keys() | swipes.keys() | required_minutes.keys()
        )
    result: dict[int, dict] = {}
    for student in Person.objects.filter(id__in=all_ids):
        result[student.id] = get_student_data(
            student,
            school_days,
            overrides[student.id],
            excuses[student.id],
            dict(swipes[student.id]),
            required_minutes[student.id],
        )
    return result


def get_student_data(
    person: Person,
    school_days: list[date],
    override_days: set[date],
    excused_days: set[date],
    day_to_swipes: dict[date, list[Swipe]],
    required_minutes: list[StudentRequiredMinutes],
) -> dict:
    required_minutes_i = -1
    current_required_minutes = DEFAULT_REQUIRED_MINUTES

    day_data = []
    total_seconds = 0
    total_abs = 0
    total_days = 0
    total_excused = 0
    total_overrides = 0
    total_short = 0
    for day in school_days:
        if person.custodia_start_date and day < person.custodia_start_date:
            continue

        if (
            required_minutes_i + 1 < len(required_minutes)
            and day >= required_minutes[required_minutes_i + 1].fromdate
        ):
            required_minutes_i += 1
            current_required_minutes = required_minutes[
                required_minutes_i
            ].required_minutes

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
                    "nice_in_time": format_time(swipe.in_time),
                    "nice_out_time": format_time(swipe.out_time),
                    "student_id": person.id,
                    "out_time": swipe.out_time and swipe.out_time.isoformat(),
                    "in_time": swipe.in_time and swipe.in_time.isoformat(),
                }
            )
            if swipe.in_time and not swipe.out_time:
                missing_swipe = True

            if swipe.in_time and swipe.out_time:
                day_seconds += (swipe.out_time - swipe.in_time).total_seconds()

        is_short = day_seconds < current_required_minutes * 60
        is_excused = day in excused_days
        is_absent = not is_excused and (day not in day_to_swipes)
        day_data.append(
            {
                "absent": is_absent,
                "override": is_override,
                "excused": is_excused,
                "day": format_date(day),
                "total_mins": current_required_minutes
                if is_override
                else (day_seconds // 60),
                "swipes": swipe_data,
                "short": is_short,
                "valid": is_override
                or (not is_short and not missing_swipe and len(this_days_swipes) > 0),
            }
        )

        if is_absent:
            total_abs += 1
        elif is_excused:
            total_excused += 1
        elif is_override:
            total_overrides += 1
        elif is_short:
            total_short += 1
        else:
            total_days += 1

        total_seconds += day_seconds

    today = timezone.localdate()

    last_swipe = None
    last_swipe_in_time = None
    if day_to_swipes:
        for swipe in day_to_swipes[max(day_to_swipes)]:
            if last_swipe_in_time is None or (
                swipe.in_time is not None and swipe.in_time > last_swipe_in_time
            ):
                last_swipe_in_time = swipe.in_time
                last_swipe = swipe

    last_swipe_date = max(day_to_swipes) if day_to_swipes else None
    return {
        "_id": person.id,
        "absent_today": person.custodia_show_as_absent == today,
        "days": reversed(day_data),
        "name": person.get_name(),
        "required_minutes": required_minutes[-1].required_minutes
        if required_minutes
        else DEFAULT_REQUIRED_MINUTES,
        "total_hours": total_seconds / 3600,
        "last_swipe_date": format_date(last_swipe_date) if last_swipe_date else None,
        "total_abs": total_abs,
        "total_days": total_days,
        "total_excused": total_excused,
        "total_overrides": total_overrides,
        "total_short": total_short,
        "start_date": person.custodia_start_date.isoformat()
        if person.custodia_start_date
        else None,
        "last_swipe_type": (
            "in" if last_swipe and last_swipe.out_time is None else "out"
        ),
        "in_today": last_swipe_date == timezone.localdate(),
    }


class ReportYears(APIView):
    permission_classes = [RequireAdmin]

    def get(self, request: Request) -> Response:
        org: Organization = request.org
        years: list[str] = []

        now = timezone.localtime()

        current_year = None
        for year in Year.objects.filter(organization=org).order_by(
            "from_time", "to_time"
        ):
            if current_year is None and now > year.from_time and now < year.to_time:
                current_year = year.name
            years.append(year.name)

        if current_year is None:
            from_date = get_start_of_school_year()
            year = self.create_year(org, from_date, date(from_date.year + 1, 8, 1))
            current_year = year.name

        return Response(
            {
                "years": years,
                "current_year": current_year,
            }
        )

    def create_year(self, org: Organization, from_date: date, to_date: date) -> Year:
        to_time = timezone.make_aware(
            datetime(to_date.year, to_date.month, to_date.day) - timedelta(seconds=1)
        )

        year, _ = Year.objects.get_or_create(
            organization=org,
            from_time=timezone.make_aware(
                datetime(from_date.year, from_date.month, from_date.day)
            ),
            to_time=to_time,
            name=self.make_period_name(from_date, to_time),
        )

        return year

    @atomic
    def post(self, request: Request) -> Response:
        def parse_date(date_str: str) -> date:
            return datetime.strptime(date_str, "%Y-%m-%d")

        from_date = parse_date(request.data["from_date"])  # type: ignore
        to_date = parse_date(request.data["to_date"])  # type: ignore

        year = self.create_year(request.org, from_date, to_date + timedelta(days=1))

        return Response({"made": {"name": year.name}})

    def make_period_name(self, from_date: date, to_date: date) -> str:
        if from_date.year != to_date.year:
            return (
                f"{from_date.strftime('%B %-d, %Y')} - {to_date.strftime('%B %-d, %Y')}"
            )
        elif from_date.month != to_date.month:
            return f"{from_date.strftime('%B %-d')} - {to_date.strftime('%B %-d, %Y')}"
        elif from_date.day != to_date.day:
            return f"{from_date.strftime('%B %-d')}-{to_date.strftime('%-d, %Y')}"

        return from_date.strftime("%B %-d, %Y")

    @atomic
    def delete(self, request: Request, year_name: str) -> Response:
        Year.objects.filter(organization=request.org, name=year_name).delete()
        return self.get(request)


class ReportView(APIView):
    permission_classes = [RequireAdmin]

    def get(self, request: Request, year_name: str, class_id: int = -1) -> Response:
        org = request.org
        year = Year.objects.get(organization=org, name=year_name)

        show_attended = request.query_params.get("filterStudents") == "all"
        show_current_students = not show_attended
        if show_attended:
            people = Person.objects.filter(organization=org)
        else:
            people = Person.objects.filter(
                tags__show_in_attendance=True,
                tags__organization=org,
            )

        people = people.order_by("first_name").distinct()

        student_info = []
        batch_data = get_student_batch_data(
            people, year, org, include_all=show_current_students
        )
        for student_data in batch_data.values():
            if (
                show_attended
                and student_data["total_days"] == 0
                and student_data["total_short"] == 0
                and student_data["total_overrides"] == 0
            ):
                continue
            student_info.append(
                {
                    "person_id": student_data["_id"],
                    "_id": student_data["_id"],
                    "name": student_data["name"],
                    "total_hours": student_data["total_hours"],
                    "good": student_data["total_days"],
                    "short": student_data["total_short"],
                    "overrides": student_data["total_overrides"],
                    "excuses": student_data["total_excused"],
                    "unexcused": student_data["total_abs"],
                }
            )

        student_info.sort(key=lambda x: x["name"])

        return Response(student_info)
