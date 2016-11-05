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
public class AttendanceWeek extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attendance_week_id_seq")
    public Integer id;

    public Date monday;

    @ManyToOne()
    @JoinColumn(name="person_id")
    public Person person;

    public double extra_hours = 0;

    public static Finder<Integer, AttendanceWeek> find = new Finder<Integer, AttendanceWeek>(
        AttendanceWeek.class
    );

    public static AttendanceWeek create(Date m, Person p)
    {
        AttendanceWeek result = new AttendanceWeek();
        result.person = p;
        result.monday = m;
        result.save();

        return result;
    }

    public void edit(double extra_hours) {
        this.extra_hours = extra_hours;
        this.update();
    }
}
