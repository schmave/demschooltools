package models;

import java.text.*;
import java.util.*;
import java.math.*;
import javax.persistence.*;
import com.fasterxml.jackson.annotation.*;
import play.data.*;
import com.avaje.ebean.*;
import java.sql.Time;

public class LateDepartureGroup {
    
    public String name;
    public List<AttendanceDay> events;

    public Integer late_fee;
    public Integer late_fee_2;
    public Integer late_fee_interval;
    public Integer late_fee_interval_2;
    public Time latest_departure_time;
    public Time latest_departure_time_2;

    public LateDepartureGroup(String person_name) {
        name = person_name;
        events = new ArrayList<AttendanceDay>();

        Organization org = OrgConfig.get().org;
        late_fee = org.attendance_report_late_fee;
        late_fee_2 = org.attendance_report_late_fee_2;
        late_fee_interval = org.attendance_report_late_fee_interval;
        late_fee_interval_2 = org.attendance_report_late_fee_interval_2;
        latest_departure_time = org.attendance_report_latest_departure_time;
        latest_departure_time_2 = org.attendance_report_latest_departure_time_2;
    }

    public int getTotalOwed() {
        int total_owed = 0;
        for (AttendanceDay event : events) {
            event.late_fee = calculateFee(event);
            total_owed += event.late_fee;
        }
        return total_owed;
    }

    private int calculateFee(AttendanceDay event) {
        if (latest_departure_time == null) {
            return 0;
        }
        int fee_1 = 0;
        int fee_2 = 0;
        int minutes_late_1 = timeToMinutes(event.end_time) - timeToMinutes(latest_departure_time);
        if (latest_departure_time_2 != null) {
            int minutes_late_2 = timeToMinutes(event.end_time) - timeToMinutes(latest_departure_time_2);
            if (minutes_late_2 > 0) {
                minutes_late_1 -= minutes_late_2;
                fee_2 = calculateFeeForRule(minutes_late_2, late_fee_2, late_fee_interval_2);
            }
        }
        fee_1 = calculateFeeForRule(minutes_late_1, late_fee, late_fee_interval);
        return fee_1 + fee_2;
    }

    private int calculateFeeForRule(int minutes_late, Integer fee, Integer interval) {
        if (minutes_late <= 0 || fee == null || fee == 0 || interval == null || interval == 0) {
            return 0;
        }
        int intervals = (minutes_late + interval - 1) / interval;
        return intervals * fee;
    }

    private int timeToMinutes(Time time) {
        return (int)(time.getTime() / (60 * 1000));
    }
}