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

from django.contrib import admin
from django.urls import path

from custodia.views import (
    DeleteSwipeView,
    IndexView,
    IsAdminView,
    LoginView,
    LogoutView,
    StudentDataView,
    StudentsTodayView,
    SwipeView,
)

urlpatterns = [
    path("admin/", admin.site.urls),
    path("", IndexView.as_view()),
    path("users/login", LoginView.as_view()),
    path("users/logout", LogoutView.as_view()),
    path("users/is-admin", IsAdminView.as_view()),
    path("students", StudentsTodayView.as_view()),
    path("students/<int:student_id>/swipe/delete", DeleteSwipeView.as_view()),
    path("students/<int:student_id>/swipe", SwipeView.as_view()),
    path("students/<int:student_id>", StudentDataView.as_view()),
]
