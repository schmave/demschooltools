from datetime import date, datetime, time

from django import forms
from django.utils import timezone


class DateToDatetimeField(forms.DateField):
    """
    Like a DateField, except that the cleaned value is a timezone-aware
    datetime object at midnight on the selected date.
    """

    def to_python(self, value):
        date_value = super().to_python(value)
        if date_value is None:
            return None

        assert isinstance(date_value, date)
        return timezone.make_aware(datetime.combine(date_value, time()))
