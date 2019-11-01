package controllers;

import java.io.*;
import java.sql.Time;
import java.util.*;
import java.util.stream.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import models.*;

import play.*;
import play.data.*;
import play.libs.Json;
import play.mvc.*;


@With(DumpOnError.class)
@Secured.Auth(UserRole.ROLE_CHECKIN_APP)
public class Checkin extends Controller {

    public Result checkinData(String time) throws ParseException {
        Date date = new SimpleDateFormat("M/d/yyyy, h:mm:ss a").parse(time);
        List<CheckinPerson> people = Application.attendancePeople().stream()
            .sorted(Comparator.comparing(Person::getDisplayName))
            .filter(p -> p.pin != null && !p.pin.isEmpty())
            .map(p -> new CheckinPerson(p, findCurrentDay(date, p.person_id)))
            .collect(Collectors.toList());

        return ok(Json.stringify(Json.toJson(people)));
    }

    public Result checkinMessage(String time_string, int person_id, boolean is_arriving) throws ParseException {
        Date date = new SimpleDateFormat("M/d/yyyy, h:mm:ss a").parse(time_string);
        Time time = new Time(date.getTime());
        AttendanceDay attendance_day = findCurrentDay(date, person_id);
        // if this is an invalid day, ignore the message
        if (attendance_day == null) {
            return ok();
        }
        // Messages from the app should never overwrite existing data.
        // Check if the value exists and set it if it doesn't.
        if (attendance_day.code == null || attendance_day.code.isEmpty()) {
            if (is_arriving && attendance_day.start_time == null) {
                attendance_day.start_time = time;
                attendance_day.update();
            }
            else if (!is_arriving && attendance_day.end_time == null) {
                attendance_day.end_time = time;
                attendance_day.update();
            }
        }
        return ok();
    }

    private AttendanceDay findCurrentDay(Date day, int person_id) {
        // if the day is Saturday or Sunday, there can't be an attendance day
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(day);
        int dow = calendar.get(Calendar.DAY_OF_WEEK);
        if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) {
            return null;
        }
        // find or create AttendanceWeek and AttendanceDay objects
        Person person = Person.findById(person_id);
        AttendanceWeek.findOrCreate(day, person);
        return AttendanceDay.find.where()
            .eq("person", person)
            .eq("day", day)
            .findUnique();
    }
}
