package models;

import java.util.*;

public class AttendanceStats {
    public int days_present;

    public double total_hours;

    public Map<AttendanceCode, Integer> absence_counts =
        new HashMap<AttendanceCode, Integer>();

    public void incrementCodeCount(AttendanceCode code) {
        if (!absence_counts.containsKey(code)) {
            absence_counts.put(code, 0);
        }
        absence_counts.put(code, absence_counts.get(code) + 1);
    }

    public double averageHoursPerDay() {
        return total_hours / days_present;
    }

    public int absenceCount() {
        int sum = 0;
        for (int val : absence_counts.values()) {
            sum += val;
        }
        return sum;
    }

    public double attendanceRate() {
        return (double)days_present / (days_present + absenceCount());
    }
}
