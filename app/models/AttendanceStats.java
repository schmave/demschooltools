package models;

import java.util.*;
import java.math.*;
import java.sql.Time;

public class AttendanceStats {

    public int days_present;
    public int partial_days_present;
    public int approved_absences;
    public int unapproved_absences;
    public double total_hours;

    public Map<AttendanceCode, Integer> absence_counts =
        new HashMap<AttendanceCode, Integer>();

    public void incrementCodeCount(AttendanceCode code) {
        if (code == null) {
            return;
        }
        // "no school" days do not count as absences
        if (!code.code.equals("_NS_") && !code.not_counted) {
            if (code.counts_toward_attendance) {
                approved_absences++;
            } else {
                unapproved_absences++;
            }
        }
        if (!absence_counts.containsKey(code)) {
            absence_counts.put(code, 0);
        }
        absence_counts.put(code, absence_counts.get(code) + 1);
    }

    public void incrementAttendance(AttendanceDay day) {
        double hours = day.getHours();
        total_hours += hours;

        if (day.isPartial()) {
            partial_days_present++;
        } else {
            days_present++;
        }
    }

    public double averageHoursPerDay() {
        return total_hours / (days_present + partial_days_present);
    }

    public double attendanceRate() {
        double attendance_days = (double)days_present + approved_absences;
        if (partial_days_present > 0) {
            BigDecimal partial_day_value = OrgConfig.get().org.attendance_partial_day_value;
            if (partial_day_value != null) {
                attendance_days += (double)partial_days_present * partial_day_value.doubleValue();
            }
        }
        int total_days = days_present + partial_days_present + approved_absences + unapproved_absences;
        return attendance_days / total_days;
    }
}
