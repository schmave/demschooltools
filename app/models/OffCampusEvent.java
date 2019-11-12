package models;

import java.util.*;
import java.sql.Time;

public class OffCampusEvent {
    
    public int attendance_day_id;
    public int person_id;
    public String name;
    public Date day;
    public Time departure_time;
    public Time return_time;

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
}
