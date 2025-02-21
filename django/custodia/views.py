from collections import defaultdict
from datetime import date, datetime, time

import requests
from django.contrib.auth import logout
from django.contrib.auth.views import redirect_to_login
from django.db.models import Max
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
    Override,
    School,
    Student,
    StudentRequiredMinutes,
    Swipe,
    Year,
)
from demschooltools.settings import LOGIN_URL
from dst.models import User, UserRole

DEFAULT_REQUIRED_MINUTES = 345


class IndexView(View):
    def get(self, request):
        if not request.user.is_authenticated:
            return redirect_to_login("")

        return render(request, "index.html")


class LoginView(View):
    def get(self, request):
        return render(request, "login.html")

    def post(self, request: HttpRequest):
        login_response = requests.post(
            ("https://" if request.is_secure() else "http://")
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

        response = redirect(LOGIN_URL)
        response.set_cookie("PLAY_SESSION", "", max_age=0)
        return response


def is_custodia_admin(user: User) -> bool:
    return user.hasRole(UserRole.ATTENDANCE)


class RequireAdmin(BasePermission):
    message = "You must have the permission to modify attendance records"

    def has_permission(self, request, view):  # type: ignore
        return is_custodia_admin(request.user)


def student_to_dict(
    student: Student,
    school: School,
    last_swipe: Swipe | None,
    in_today_time: datetime | None,
):
    today = timezone.localdate()
    if in_today_time:
        in_today_time = timezone.localtime(in_today_time)
    in_today = last_swipe and last_swipe.swipe_day == today
    return {
        "_id": student.id,
        "name": student.name,
        "last_swipe_type": (
            "in" if last_swipe and last_swipe.out_time is None else "out"
        ),
        "swiped_today_late": (
            in_today_time and in_today_time.time() > school.late_time
        ),
        "is_teacher": student.is_teacher,
        "in_today": in_today,
        "absent_today": student.show_as_absent == today,
        "last_swipe_date": last_swipe and format_date(last_swipe.swipe_day),
    }


def get_student(request: Request, student_id: int):
    if getattr(request, "school") is None:
        raise PermissionDenied()

    student = Student.objects.filter(id=student_id, school=request.school).first()
    if student is None:
        raise NotFound()

    return student


class AbsentView(APIView):
    permission_classes = [RequireAdmin]

    def post(self, request: Request, student_id: int) -> Response:
        student = get_student(request, student_id)
        student.show_as_absent = timezone.localdate()
        student.save()

        return student_data_view(student_id, request.school)


class ExcuseView(APIView):
    permission_classes = [RequireAdmin]

    @atomic
    def post(self, request: Request, student_id: int) -> Response:
        student = get_student(request, student_id)
        date: str = request.data["day"]  # type: ignore

        Excuse.objects.create(student=student, date=date)

        return student_data_view(student_id, request.school)


class OverrideView(APIView):
    permission_classes = [RequireAdmin]

    @atomic
    def post(self, request: Request, student_id: int) -> Response:
        student = get_student(request, student_id)
        date: str = request.data["day"]  # type: ignore

        Override.objects.create(student=student, date=date)

        return student_data_view(student_id, request.school)


class SwipeView(APIView):
    @atomic
    def post(self, request: Request, student_id: int) -> Response:
        student = get_student(request, student_id)

        direction = request.data["direction"]  # type: ignore

        school: School = request.school
        swipe_time = timezone.localtime()

        if request.data.get("overrideDate"):  # type: ignore
            the_date = datetime.fromisoformat(request.data["overrideDate"])  # type: ignore
            the_time = time.fromisoformat(
                request.data["overrideTime"]  # type: ignore
            )
            swipe_time = timezone.make_aware(
                the_date.replace(hour=the_time.hour, minute=the_time.minute)
            )

        if direction == "in":
            swipe = Swipe.objects.create(
                student=student,
                swipe_day=swipe_time.date(),
                in_time=swipe_time,
            )
        elif direction == "out":
            swipe = Swipe.objects.get(
                student=student, swipe_day=swipe_time.date(), out_time=None
            )
            swipe.out_time = swipe_time
            swipe.save()
        else:
            assert False, "invalid direction"

        in_time_today = (
            Swipe.objects.filter(student=student, swipe_day=timezone.localdate())
            .order_by("in_time")
            .values_list("in_time", flat=True)
            .first()
        )

        return Response(student_to_dict(student, school, swipe, in_time_today))


class DeleteSwipeView(APIView):
    permission_classes = [RequireAdmin]

    @atomic
    def post(self, request: Request, student_id: int) -> Response:
        student = get_student(request, student_id)
        swipe_id = request.data["swipe"]["_id"]  # type: ignore
        Swipe.objects.get(id=swipe_id, student=student).delete()
        return student_data_view(student.id, request.school)


class IsAdminView(APIView):
    def get(self, request: Request) -> Response:
        school: School = request.school
        user: User = request.user
        return Response(
            {
                "admin": "overseer.roles/admin" if is_custodia_admin(user) else None,
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


def format_time(dt: datetime | None) -> str:
    if dt:
        return timezone.localtime(dt).strftime("%-I:%M %p").lower()
    else:
        return ""


class StudentsTodayView(APIView):
    def get(self, request: Request):
        school = request.school
        today = timezone.localdate()

        student_infos = []

        students = list(
            Student.objects.filter(
                person__tags__show_in_attendance=True, school=school
            ).annotate(max_swipe_id=Max("swipe__id"))
        )

        student_to_last_swipe = {
            x.student_id: x
            for x in Swipe.objects.filter(
                id__in=[x.max_swipe_id for x in students]  # type: ignore
            )
        }

        student_to_in_time: dict[int, datetime] = {}
        for student_id, in_time in (
            Swipe.objects.filter(
                student__in=students,
                swipe_day=today,
            )
            .order_by("in_time")
            .values_list("student_id", "in_time")
        ):
            if student_id not in student_to_in_time:
                student_to_in_time[student_id] = in_time

        for student in students:
            last_swipe = student_to_last_swipe[student.id]
            student_infos.append(
                student_to_dict(
                    student,
                    school,
                    last_swipe,
                    student_to_in_time.get(student.id),
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
    def put(self, request: Request, student_id: int) -> Response:
        student = get_student(request, student_id)
        start_date_str: str
        if start_date_str := request.data["start_date"]:  # type: ignore
            student.start_date = datetime.strptime(start_date_str, "%Y-%m-%d").date()

        StudentRequiredMinutes.objects.update_or_create(
            student=student,
            fromdate=timezone.localdate(),
            defaults=dict(required_minutes=request.data["minutes"]),  # type: ignore
        )

        student.save()

        return student_data_view(student.id, request.school)

    def get(self, request: Request, student_id: int) -> Response:
        get_student(request, student_id)  # just call this for auth
        return student_data_view(student_id, request.school)


def get_year_start_end(year: Year | None) -> tuple[datetime, datetime]:
    if year:
        return (year.from_time, year.to_time)
    return (
        get_start_of_school_year(),
        datetime(3000, 1, 1),
    )


def get_school_days(school: School, start: datetime, end: datetime) -> list[date]:
    return sorted(
        Swipe.objects.filter(
            student__school=school,
            swipe_day__gte=start.date(),
            swipe_day__lte=end.date(),
        )
        .values_list("swipe_day", flat=True)
        .distinct(),
    )


def student_data_view(
    student_id: int, school: School, year: Year | None = None
) -> Response:
    return Response(
        {
            "student": get_student_batch_data(
                Student.objects.filter(id=student_id, school=school), year
            )[student_id]
        }
    )


def get_student_batch_data(
    students: QuerySet[Student], year: Year | None
) -> dict[int, dict]:
    school = students[0].school
    year_start, year_end = get_year_start_end(year)
    school_days = get_school_days(school, year_start, year_end)

    overrides: dict[int, set[date]] = defaultdict(set)
    for student_id, the_date in Override.objects.filter(
        student__in=students
    ).values_list("student_id", "date"):
        overrides[student_id].add(the_date)

    excuses: dict[int, set[date]] = defaultdict(set)
    for student_id, the_date in Excuse.objects.filter(student__in=students).values_list(
        "student_id", "date"
    ):
        excuses[student_id].add(the_date)

    swipes: dict[int, dict[date, list[Swipe]]] = defaultdict(lambda: defaultdict(list))

    for swipe in Swipe.objects.filter(
        student__in=students, swipe_day__gte=year_start, swipe_day__lte=year_end
    ):
        swipes[swipe.student_id][swipe.swipe_day].append(swipe)

    required_minutes: dict[int, list[StudentRequiredMinutes]] = defaultdict(list)
    for srm in StudentRequiredMinutes.objects.filter(student__in=students).order_by(
        "fromdate"
    ):
        required_minutes[srm.student_id].append(srm)

    result: dict[int, dict] = {}
    for student in students:
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
    student: Student,
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
        if student.start_date and day < student.start_date:
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
                    "student_id": student.id,
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
        "_id": student.id,
        "absent_today": student.show_as_absent == today,
        "days": day_data,
        "name": student.name,
        "required_minutes": required_minutes[-1].required_minutes
        if required_minutes
        else DEFAULT_REQUIRED_MINUTES,
        "total_hours": total_seconds / 3600,
        "is_teacher": student.is_teacher,
        "last_swipe_date": format_date(last_swipe_date) if last_swipe_date else None,
        "total_abs": total_abs,
        "total_days": total_days,
        "total_excused": total_excused,
        "total_overrides": total_overrides,
        "total_short": total_short,
        "start_date": student.start_date.isoformat() if student.start_date else None,
        "last_swipe_type": (
            "in" if last_swipe and last_swipe.out_time is None else "out"
        ),
        "in_today": last_swipe_date == timezone.localdate(),
    }


class ReportYears(APIView):
    permission_classes = [RequireAdmin]

    def get(self, request: Request) -> Response:
        school: School = request.school
        years: list[str] = []

        now = timezone.localtime()

        current_year = None
        for year in Year.objects.filter(school=school).order_by("from_time", "to_time"):
            if current_year is None and now > year.from_time and now < year.to_time:
                current_year = year.name
            years.append(year.name)

        if current_year is None:
            from_date = get_start_of_school_year().date()
            to_date = date(from_date.year + 1, 7, 31)
            year = Year.objects.create(
                school=school,
                from_time=from_date,
                to_time=to_date,
                name=self.make_period_name(from_date, to_date),
            )
            current_year = year.name

        return Response(
            {
                "years": years,
                "current_year": current_year,
            }
        )

    @atomic
    def post(self, request: Request) -> Response:
        def parse_date(date_str: str) -> date:
            return datetime.strptime(date_str, "%Y-%m-%d")

        from_date = parse_date(request.data["from_date"])  # type: ignore
        to_date = parse_date(request.data["to_date"])  # type: ignore

        year, _ = Year.objects.get_or_create(
            school=request.school,
            from_time=timezone.make_aware(
                datetime(from_date.year, from_date.month, from_date.day)
            ),
            to_time=timezone.make_aware(
                datetime(to_date.year, to_date.month, to_date.day)
            ),
            name=self.make_period_name(from_date, to_date),
        )

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
        Year.objects.filter(school=request.school, name=year_name).delete()

        return self.get(request)


class ReportView(APIView):
    permission_classes = [RequireAdmin]

    def get(self, request: Request, year_name: str, class_id: int = -1) -> Response:
        school = request.school
        year = Year.objects.get(school=school, name=year_name)

        show_attended = request.query_params.get("filterStudents") == "all"
        if show_attended:
            students = Student.objects.filter(school=school).order_by("name")
        else:
            students = Student.objects.filter(
                person__tags__show_in_attendance=True, school=school
            ).order_by("name")

        student_info = []
        batch_data = get_student_batch_data(students, year)
        for student in students:
            student_data = batch_data[student.id]
            if (
                show_attended
                and student_data["total_days"] == 0
                and student_data["total_short"] == 0
                and student_data["total_overrides"] == 0
            ):
                continue
            student_info.append(
                {
                    "student_id": student_data["_id"],
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

        return Response(student_info)
