from datetime import datetime

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
        # TODO
        "show_as_absent": False,
        # TODO
        "absent_today": False,
        "last_swipe_date": last_swipe and last_swipe.swipe_day.strftime("%Y-%m-%d"),
    }


class SwipeView(APIView):
    def post(self, request: Request, student_id=None):
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


class StudentsView(APIView):
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
                "today": now.strftime("%Y-%m-%d"),
                "students": student_infos,
            }
        )
