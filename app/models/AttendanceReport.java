package models;

import java.text.*;
import java.util.*;
import java.math.*;
import javax.persistence.*;
import com.fasterxml.jackson.annotation.*;
import play.data.*;
import com.avaje.ebean.*;
import java.sql.Time;

public class AttendanceReport {

    public List<AttendanceDay> events;

    @play.data.format.Formats.DateTime(pattern="MM/dd/yyyy")
    public Date start_date;

    @play.data.format.Formats.DateTime(pattern="MM/dd/yyyy")
    public Date end_date;

    public Time latest_departure_time;

    public AttendanceReport() {
        Organization org = OrgConfig.get().org;
        latest_departure_time = org.attendance_report_latest_departure_time;
    }

    public static AttendanceReport createFromForm(Form<AttendanceReport> form) {
        AttendanceReport model = form.get();

        model.events = AttendanceDay.find.where()
            .eq("person.organization", OrgConfig.get().org)
            .ge("day", model.start_date)
            .le("day", model.end_date)
            .gt("end_time", model.latest_departure_time)
            .order("person.first_name ASC, day ASC")
            .findList();

        return model;
    }

    public static String formatDate(Date date) {
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        return sdf.format(date);
    }
}
