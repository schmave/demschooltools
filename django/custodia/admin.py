from django.contrib import admin

from custodia.models import (
    CustodiaUser,
    Student,
    StudentRequiredMinutes,
    Swipe,
)

admin.site.register(Swipe)
admin.site.register(Student)
admin.site.register(StudentRequiredMinutes)
admin.site.register(CustodiaUser)
