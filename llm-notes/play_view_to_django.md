# How to convert Play to Django

Convert Attendance.viewWeek in @/app/controllers/Attendance.java
to Django in @/django/dst/attendance_views.py .

Switch @routes to use a proxy for that route.

Use @login_required for permissions

Read @/django/dst/templates/attendance_index.html for an example of how to do the template

Look for existing occurrences of routes.<controller>.<method> and replace them with a string value.