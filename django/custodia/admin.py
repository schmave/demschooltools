from django.contrib import admin

from custodia.models import (
    Student,
    Swipe,
)

admin.site.register(Swipe)
admin.site.register(Student)
