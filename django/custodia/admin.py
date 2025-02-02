from django.contrib import admin

from custodia.models import (
    Student,
    StudentRequiredMinutes,
    Swipe,
)

admin.site.register(Swipe)
admin.site.register(Student)
admin.site.register(StudentRequiredMinutes)
