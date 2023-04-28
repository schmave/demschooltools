package models;

import io.ebean.*;

import javax.persistence.*;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class AttendanceWeek extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attendance_week_id_seq")
    private Integer id;

    private Date monday;

    @ManyToOne()
    @JoinColumn(name="personId")
    private Person person;

    private double extraHours = 0;

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
        ModelUtils.adjustToPreviousDay(calendar, Calendar.MONDAY);
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

    public void edit(double extraHours) {
        this.extraHours = extraHours;
        this.update();
    }
}