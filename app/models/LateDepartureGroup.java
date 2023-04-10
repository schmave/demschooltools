package models;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class LateDepartureGroup {
    
    public String name;
    public List<AttendanceDay> events;

    public Integer late_fee;
    public Integer late_fee_interval;
    public Time latest_departure_time;

    public LateDepartureGroup(String person_name) {
        name = person_name;
        events = new ArrayList<>();

        Organization org = OrgConfig.get().org;
        late_fee = org.attendance_report_late_fee;
        late_fee_interval = org.attendance_report_late_fee_interval;
        latest_departure_time = org.attendance_report_latest_departure_time;
    }

    public Integer getTotalOwed() {
        if (late_fee == null || late_fee == 0 || late_fee_interval == null || late_fee_interval == 0) {
            return null;
        }
        int total_owed = 0;
        for (AttendanceDay event : events) {
            int minutes_late = (int)(event.end_time.getTime() / (60 * 1000)) - (int)(latest_departure_time.getTime() / (60 * 1000));
            int intervals = (minutes_late + late_fee_interval - 1) / late_fee_interval;
            event.late_fee = intervals * late_fee;
            total_owed += event.late_fee;
        }
        return total_owed;
    }
}