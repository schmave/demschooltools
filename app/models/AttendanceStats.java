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
}
