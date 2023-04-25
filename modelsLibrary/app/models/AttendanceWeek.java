package models;

import io.ebean.*;
import controllers.Utils;

import javax.persistence.*;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

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

    public static Finder<Integer, AttendanceWeek> find = new Finder<>(
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

    public static AttendanceWeek findOrCreate(Date day, Person person) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(day);
        Utils.adjustToPreviousDay(calendar, Calendar.MONDAY);
        Date monday = calendar.getTime();

        AttendanceWeek result = AttendanceWeek.find.query().where()
            .eq("person", person)
            .eq("monday", monday)
            .findOne();

        if (result != null) return result;

        AttendanceWeek attendance_week = new AttendanceWeek();
        attendance_week.person = person;
        attendance_week.monday = monday;
        attendance_week.save();

        for (int i = 0; i < 5; i++) {
            AttendanceDay.create(calendar.getTime(), person);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return attendance_week;
    }

    public void edit(double extra_hours) {
        this.extra_hours = extra_hours;
        this.update();
    }
}
