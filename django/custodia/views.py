from datetime import date, datetime

import pytz
from django.contrib.auth import authenticate, login, logout
from django.contrib.auth.views import redirect_to_login
from django.http import HttpResponse
from django.shortcuts import redirect, render
from django.views import View
from rest_framework.request import Request
from rest_framework.response import Response
from rest_framework.views import APIView

from custodia.models import School, Student, Swipe


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


def format_date(dt: datetime | date):
    return dt.strftime("%Y-%m-%d")


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


class StudentDataView(APIView):
    def get(self, request: Request, student_id: int):
        student = Student.objects.get(id=student_id)
        school: School = request.user.school
        local_now = datetime.now(pytz.timezone(school.timezone))

        return Response(
            {
                "student": {
                    "_id": student.id,
                    "absent_today": student.show_as_absent == local_now.date(),
                    "days": [
                        {
                            "valid": False,
                            "short": True,
                            "absent": False,
                            "override": False,
                            "excused": False,
                            "day": "2024-05-03",
                            "total_mins": 0,
                            "swipes": [
                                {
                                    "archived": False,
                                    "_id": 10,
                                    "day": "2024-05-03",
                                    "nice_in_time": "03:47",
                                    "nice_out_time": "03:48",
                                    "student_id": student.id,
                                    "out_time": "2024-05-03T19:48:06Z",
                                    "in_time": "2024-05-03T19:47:03Z",
                                }
                            ],
                        }
                    ],
                    "in_today": False,
                    "is_teacher": False,
                    "last_swipe_date": "2024-05-03",
                    "last_swipe_type": "out",
                    "name": student.name,
                    "required_minutes": 345,
                    "today": format_date(local_now),
                    "total_abs": 0,
                    "total_days": 0,
                    "total_excused": 0,
                    "total_hours": 0.016667,
                    "total_overrides": 0,
                    "total_short": 3,
                }
            }
        )
