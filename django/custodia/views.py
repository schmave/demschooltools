from collections import defaultdict
from datetime import date, datetime

from django.contrib.auth import authenticate, login, logout
from django.contrib.auth.views import redirect_to_login
from django.db.models import Max
from django.db.transaction import atomic
from django.http import HttpResponse
from django.shortcuts import redirect, render
from django.utils import timezone
from django.views import View
from rest_framework.generics import QuerySet
from rest_framework.request import Request
from rest_framework.response import Response
from rest_framework.views import APIView

from custodia.models import Excuse, Override, School, Student, Swipe, Year

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


class AbsentView(APIView):
    def post(self, request: Request, student_id: int) -> Response:
        student = Student.objects.get(id=student_id, school=request.user.school)
        student.show_as_absent = timezone.localdate()
        student.save()

        return student_data_view(student_id, request.user.school)


class ExcuseView(APIView):
    def post(self, request: Request, student_id: int) -> Response:
        student = Student.objects.get(id=student_id, school=request.user.school)
        date: str = request.data["day"]  # type: ignore

        Excuse.objects.create(student=student, date=date)

        return student_data_view(student_id, request.user.school)


class OverrideView(APIView):
    def post(self, request: Request, student_id: int) -> Response:
        student = Student.objects.get(id=student_id)
        date: str = request.data["day"]  # type: ignore

        Override.objects.create(student=student, date=date)

        return student_data_view(student_id, request.user.school)


class SwipeView(APIView):
    def post(self, request: Request, student_id: int) -> Response:
        student = Student.objects.get(id=student_id, school=request.user.school)

        direction = request.data["direction"]  # type: ignore

        school: School = request.user.school
        swipe_time = timezone.localtime()

        if request.data.get("overrideDateTime"):  # type: ignore
            swipe_time = datetime.fromisoformat(
                request.data["overrideDateTime"]  # type: ignore
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
    def post(self, request: Request, student_id: int) -> Response:
        swipe_id = request.data["swipe"]["_id"]  # type: ignore
        Swipe.objects.get(id=swipe_id).delete()
        return student_data_view(student_id, request.user.school)


class LogoutView(View):
    def get(self, request):
        if request.user.is_authenticated:
            logout(request)
        return redirect("/")


class IsAdminView(APIView):
    def get(self, request: Request) -> Response:
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


def format_time(dt: datetime | None) -> str:
    if dt:
        return timezone.localtime(dt).strftime("%I:%M")
    else:
        return ""


class StudentsTodayView(APIView):
    def get(self, request: Request):
        school = request.user.school
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
    def get(self, request: Request, student_id: int) -> Response:
        return student_data_view(student_id, request.user.school)


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
        reverse=True,
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

    result: dict[int, dict] = {}
    for student in students:
        result[student.id] = get_student_data(
            student,
            school_days,
            overrides[student.id],
            excuses[student.id],
            dict(swipes[student.id]),
        )
    return result


def get_student_data(
    student: Student,
    school_days: list[date],
    override_days: set[date],
    excused_days: set[date],
    day_to_swipes: dict[date, list[Swipe]],
) -> dict:
    day_data = []
    total_seconds = 0
    total_abs = 0
    total_days = 0
    total_excused = 0
    total_overrides = 0
    total_short = 0
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

        is_short = day_seconds < DEFAULT_REQUIRED_MINUTES * 60
        is_excused = day in excused_days
        is_absent = not is_excused and (day not in day_to_swipes)
        day_data.append(
            {
                "absent": is_absent,
                "override": is_override,
                "excused": is_excused,
                "day": format_date(day),
                "total_mins": DEFAULT_REQUIRED_MINUTES
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
        "required_minutes": DEFAULT_REQUIRED_MINUTES,
        "total_hours": total_seconds / 3600,
        "is_teacher": student.is_teacher,
        "last_swipe_date": format_date(last_swipe_date) if last_swipe_date else None,
        "total_abs": total_abs,
        "total_days": total_days,
        "total_excused": total_excused,
        "total_overrides": total_overrides,
        "total_short": total_short,
        "last_swipe_type": (
            "in" if last_swipe and last_swipe.out_time is None else "out"
        ),
        "in_today": last_swipe_date == timezone.localdate(),
    }


class ReportYears(APIView):
    def get(self, request: Request) -> Response:
        school: School = request.user.school
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
            school=request.user.school,
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

    def delete(self, request: Request, year_name: str) -> Response:
        Year.objects.filter(school=request.user.school, name=year_name).delete()

        return self.get(request)


class ReportView(APIView):
    def get(self, request: Request, year_name: str, class_id: int = -1) -> Response:
        school = request.user.school
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
