"""demschooltools URL Configuration

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/3.2/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  path('', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  path('', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.urls import include, path
    2. Add a URL to urlpatterns:  path('blog/', include('blog.urls'))
"""

from django.conf import settings
from django.contrib import admin
from django.urls import include, path

from custodia.views import (
    AbsentView,
    DeleteSwipeView,
    ErrorTestView,
    ExcuseView,
    IndexView,
    IsAdminView,
    LoginView,
    LogoutView,
    OverrideView,
    ReportView,
    ReportYears,
    StudentDataView,
    StudentsTodayView,
    SwipeView,
)
from dst.manual_views import (
    CreateUpdateChapter,
    CreateUpdateSection,
    view_chapter,
    view_manual,
)

urlpatterns = [
    path("admin/", admin.site.urls),
    path(
        "custodia-api/",
        (
            [
                path("users/is-admin", IsAdminView.as_view()),
                path("students", StudentsTodayView.as_view()),
                path(
                    "students/<int:person_id>/swipe/delete", DeleteSwipeView.as_view()
                ),
                path("students/<int:person_id>/swipe", SwipeView.as_view()),
                path("students/<int:person_id>/absent", AbsentView.as_view()),
                path("students/<int:person_id>/excuse", ExcuseView.as_view()),
                path("students/<int:person_id>/override", OverrideView.as_view()),
                path("students/<int:person_id>", StudentDataView.as_view()),
                path("reports/years/<str:year_name>", ReportYears.as_view()),
                path("reports/years", ReportYears.as_view()),
                path("reports/<str:year_name>/<int:class_id>", ReportView.as_view()),
                path("reports/<str:year_name>", ReportView.as_view()),
            ],
            "custodia-api",
            "custodia-api",
        ),
    ),
    path("viewManual", view_manual),
    path("viewChapter/<int:chapter_id>", view_chapter),
    path("addChapter", CreateUpdateChapter.as_view()),
    path("editChapter", CreateUpdateChapter.as_view()),
    path("editChapter/<int:object_id>", CreateUpdateChapter.as_view()),
    path("addSection/<int:chapter_id>", CreateUpdateSection.as_view()),
    path("editSection", CreateUpdateSection.as_view()),
    path("editSection/<int:object_id>", CreateUpdateSection.as_view()),
    path("", IndexView.as_view()),
    path("custodia/", IndexView.as_view()),
    path("custodia/error-test", ErrorTestView.as_view()),
    path("custodia/login", LoginView.as_view()),
    path("custodia/logout", LogoutView.as_view()),
]

if settings.SILK_ENABLED:
    urlpatterns += [path("silk/", include("silk.urls", namespace="silk"))]
