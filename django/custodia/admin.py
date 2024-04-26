from django.contrib import admin

from custodia.models import (
    CustodiaUser,
    Student,
    Swipe,
)

admin.site.register(Swipe)
admin.site.register(Student)
admin.site.register(CustodiaUser)
