package models;

import java.util.*;
import java.sql.Time;

public class OffCampusEvent {
    
    public int attendance_day_id;
    public Integer person_id;
    public String name;
    public Date day;
    public Time departure_time;
    public Time return_time;

    public OffCampusEvent() {
    }

    public OffCampusEvent(AttendanceDay attendance_day) {
        attendance_day_id = attendance_day.id;
        person_id = attendance_day.person.person_id;
        name = attendance_day.person.getDisplayName();
        day = attendance_day.day;
        departure_time = attendance_day.off_campus_departure_time;
        return_time = attendance_day.off_campus_return_time;
    }

    public Date getDay() {
        return day;
    }

    public void save() {
        if (person_id == null || day == null || departure_time == null || return_time == null) {
            return;
        }
        AttendanceDay attendance_day = AttendanceDay.findCurrentDay(day, person_id);
        attendance_day.off_campus_departure_time = departure_time;
        attendance_day.off_campus_return_time = return_time;
        attendance_day.update();
    }
}
