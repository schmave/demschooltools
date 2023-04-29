package models;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import play.data.Form;

public class AttendanceReport {

    public List<LateDepartureGroup> late_departures;

    @play.data.format.Formats.DateTime(pattern="MM/dd/yyyy")
    public Date start_date;

    @play.data.format.Formats.DateTime(pattern="MM/dd/yyyy")
    public Date end_date;

    public Time latest_departure_time;

    public AttendanceReport() {
        late_departures = new ArrayList<>();
    }

    public static AttendanceReport createFromForm(Form<AttendanceReport> form, Organization org) {
        AttendanceReport model = form.get();

        model.latest_departure_time = org.getAttendanceReportLatestDepartureTime();

        List<AttendanceDay> events = AttendanceDay.find.query().where()
            .eq("person.organization", org)
            .ge("day", model.start_date)
            .le("day", model.end_date)
            .gt("endTime", model.latest_departure_time)
            .order("person.firstName ASC, day ASC")
            .findList();

        for (AttendanceDay event : events) {
            String name = event.getPerson().getDisplayName();
            LateDepartureGroup group = model.late_departures.stream()
                .filter(g -> name.equals(g.name))
                .findAny().orElse(null);

            if (group == null) {
                group = new LateDepartureGroup(name, org);
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
