package models;

import play.data.Form;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AttendanceReport {

    public List<LateDepartureGroup> late_departures;

    @play.data.format.Formats.DateTime(pattern="MM/dd/yyyy")
    public Date start_date;

    @play.data.format.Formats.DateTime(pattern="MM/dd/yyyy")
    public Date end_date;

    public Time latest_departure_time;

    public AttendanceReport() {
        late_departures = new ArrayList<>();

        Organization org = OrgConfig.get().org;
        latest_departure_time = org.attendance_report_latest_departure_time;
    }

    public static AttendanceReport createFromForm(Form<AttendanceReport> form) {
        AttendanceReport model = form.get();

        List<AttendanceDay> events = AttendanceDay.find.query().where()
            .eq("person.organization", OrgConfig.get().org)
            .ge("day", model.start_date)
            .le("day", model.end_date)
            .gt("end_time", model.latest_departure_time)
            .order("person.first_name ASC, day ASC")
            .findList();

        for (AttendanceDay event : events) {
            String name = event.person.getDisplayName();
            LateDepartureGroup group = model.late_departures.stream()
                .filter(g -> name.equals(g.name))
                .findAny().orElse(null);

            if (group == null) {
                group = new LateDepartureGroup(name);
                model.late_departures.add(group);
            }
            group.events.add(event);
        }

        return model;
    }

    public static String formatDate(Date date) {
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        return sdf.format(date);
    }
}