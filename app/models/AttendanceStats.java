package models;

import java.util.*;

public class AttendanceStats {

    public int days_present;
    public int approved_absences;
    public int unapproved_absences;
    public double total_hours;

    public Map<AttendanceCode, Integer> absence_counts =
        new HashMap<AttendanceCode, Integer>();

    public void incrementCodeCount(AttendanceCode code) {
        if (code == null) {
            return;
        }
        if (code.counts_toward_attendance) {
            approved_absences++;
        } else {
            unapproved_absences++;
        }
        if (!absence_counts.containsKey(code)) {
            absence_counts.put(code, 0);
        }
        absence_counts.put(code, absence_counts.get(code) + 1);
    }

    public double averageHoursPerDay() {
        return total_hours / days_present;
    }

    public double attendanceRate() {
        int days_present_or_approved = days_present + approved_absences;
        int total_days = days_present_or_approved + unapproved_absences;
        return (double)days_present_or_approved / total_days;
    }
}
