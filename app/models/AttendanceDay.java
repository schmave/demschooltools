package models;

import java.io.*;
import java.sql.Time;
import java.util.*;

import javax.persistence.*;

import com.avaje.ebean.Model;
import com.avaje.ebean.Model.Finder;

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

    public static Finder<Integer, AttendanceDay> find = new Finder<>(
        Integer.class, AttendanceDay.class
    );

    public static AttendanceDay create(Date day, Person p)
    {
        AttendanceDay result = new AttendanceDay();
        result.person = p;
        result.day = day;
        result.save();

        return result;
    }

    public static Time parseTime(String time_string) {
        if (time_string.equals("")) {
            return null;
        }

        return Time.valueOf(time_string);
    }

    public void edit(Map<String, String[]> query_string) {
        if (query_string.containsKey("code")) {
            code = query_string.get("code")[0];
        } else {
            code = null;
        }

        if (query_string.containsKey("start_time")) {
            start_time = parseTime(query_string.get("start_time")[0]);
        } else {
            start_time = null;
        }

        if (query_string.containsKey("end_time")) {
            end_time = parseTime(query_string.get("end_time")[0]);
        } else {
            end_time = null;
        }

        this.update();
    }

    public double getHours() {
        return (end_time.getTime() - start_time.getTime()) / (1000.0 * 60 * 60);
    }
}
