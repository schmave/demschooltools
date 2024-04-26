import json
from datetime import datetime

import pytz
from django.contrib.auth import authenticate, login, logout
from django.contrib.auth.views import redirect_to_login
from django.http import HttpResponse
from django.shortcuts import redirect, render
from django.views import View

from custodia.models import School, Student


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


class LogoutView(View):
    def get(self, request):
        if request.user.is_authenticated:
            logout(request)
        return redirect("/")


class IsAdminView(View):
    def get(self, request):
        school: School = request.user.school
        return HttpResponse(
            json.dumps(
                {
                    "admin": None,  #  "overseer.roles/admin" if admin
                    "school": {
                        "_id": school.id,
                        "name": school.name,
                        "timezone": school.timezone,
                        "use_display_name": school.use_display_name,
                    },
                }
            ),
            content_type="application/json",
        )


class StudentsView(View):
    def get(self, request):
        school = request.user.school
        tz = pytz.timezone(school.timezone)
        now = datetime.now(tz)

        student_infos = []

        for student in Student.objects.filter(person__tags__show_in_attendance=True):
            student_infos.append(
                {
                    "archived": False,
                    "_id": student.id,
                    "name": student.name,
                    "last_swipe_type": "out",
                    "swiped_today_late": False,
                    "is_teacher": student.is_teacher,
                    "in_today": False,
                    "swiped_today": False,
                    "late_time": None,
                    "show_as_absent": False,
                    "absent_today": False,
                    "last_swipe_date": None,  # "2024-04-11",
                }
            )

        return HttpResponse(
            json.dumps(
                {
                    "today": now.strftime("%Y-%m-%d"),
                    "students": student_infos,
                }
            ),
            content_type="application/json",
        )
