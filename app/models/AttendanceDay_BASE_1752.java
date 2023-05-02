package models;

import java.io.*;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.persistence.*;

import com.avaje.ebean.Model;
import com.avaje.ebean.Model.Finder;
import com.fasterxml.jackson.annotation.*;

import controllers.Application;

import play.libs.Json;

@Entity
public class AttendanceDay extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attendance_day_id_seq")
    public Integer id;

    public Date day;

    @ManyToOne()
    @JoinColumn(name="person_id")
    public Person person;

    public String code;

    public Time start_time;
    public Time end_time;

    public Time off_campus_departure_time;
    public Time off_campus_return_time;
    public Integer off_campus_minutes_exempted;

    @Transient
    public Integer late_fee;

    public static Finder<Integer, AttendanceDay> find = new Finder<Integer, AttendanceDay>(
        AttendanceDay.class
    );

    public static AttendanceDay findById(int id) {
        return find.where()
            .eq("person.organization", Organization.getByHost())
            .eq("id", id)
            .findUnique();
    }

    public static AttendanceDay create(Date day, Person p)
    {
        AttendanceDay result = new AttendanceDay();
        result.person = p;
        result.day = day;
        result.save();

        return result;
    }

    public static Time parseTime(String time_string) {
        if (time_string == null || time_string.equals("")) {
            return null;
        }

		String[] formats = {"h:mm a", "h:mma", "h:mm"};

		for (String format : formats) {
			try {
				Date d = new SimpleDateFormat(format).parse(time_string);
				return new Time(d.getTime());
			} catch (ParseException e) {
			}
		}

		return null;
    }

    public static Integer parseInt(String str) {
        if (str == null || str.equals("")) {
            return null;
        }
        return Integer.parseInt(str);
    }

    public void edit(String code, String start_time, String end_time) throws Exception {
        if (code.equals("")) {
            this.code = null;
        } else {
            this.code = code;
        }

        this.start_time = parseTime(start_time);
        this.end_time = parseTime(end_time);

        this.update();
    }

    @JsonIgnore
    public double getHours() {
        long off_campus_time = 0;
        if (off_campus_return_time != null && off_campus_departure_time != null) {
            off_campus_time = off_campus_return_time.getTime() - off_campus_departure_time.getTime();
            if (off_campus_minutes_exempted != null) {
                off_campus_time -= off_campus_minutes_exempted * 60 * 1000;
                if (off_campus_time < 0) {
                    off_campus_time = 0;
                }
            }
        }
        return (end_time.getTime() - start_time.getTime() - off_campus_time) / (1000.0 * 60 * 60);
    }

    @JsonIgnore
    public boolean isPartial() {
        Organization org = OrgConfig.get().org;

        if (org.attendance_enable_partial_days) {
            Double min_hours = org.attendance_day_min_hours;
            Time latest_start_time = org.attendance_day_latest_start_time;

            if (min_hours != null && getHours() < min_hours) {
                return true;
            }
            if (latest_start_time != null && start_time.getTime() > latest_start_time.getTime()) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("M/d/yy");
        return sdf.format(day);
    }

    public static AttendanceDay findCurrentDay(Date day, int person_id) {
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
