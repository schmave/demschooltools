package models;

import java.util.HashMap;
import java.util.Map;

public class AttendanceStats {

    public int days_present;
    public int partial_days_present;
    public int approved_absences;
    public int unapproved_absences;
    public double total_hours;

    public Map<AttendanceCode, Integer> absence_counts =
            new HashMap<>();

    private final Map<Integer, Double> values;

    private double partial_day_value;

    public AttendanceStats(Organization org) {
        values = new HashMap<>();
        partial_day_value = 0;
        if (org.attendance_partial_day_value != null) {
            partial_day_value = org.attendance_partial_day_value.doubleValue();
        }
    }

    public void processDay(AttendanceDay day, int index, Map<String, AttendanceCode> codes_map) {
        if (day == null) {
            return;
        }
        if (day.code != null || day.start_time == null || day.end_time == null) {
            incrementCodeCount(codes_map.get(day.code), index);
        }
        else {
            incrementAttendance(day, index);
        }
    }

    private void incrementCodeCount(AttendanceCode code, int index) {
        if (code == null) {
            return;
        }
        if (!code.not_counted) {
            if (code.counts_toward_attendance) {
                approved_absences++;
                values.put(index, 1d);
            } else {
                unapproved_absences++;
                values.put(index, 0d);
            }
        }
        if (!absence_counts.containsKey(code)) {
            absence_counts.put(code, 0);
        }
        absence_counts.put(code, absence_counts.get(code) + 1);
    }

    private void incrementAttendance(AttendanceDay day, int index) {
        double hours = day.getHours();
        total_hours += hours;

        if (day.isPartial()) {
            partial_days_present++;
            values.put(index, partial_day_value);
        } else {
            days_present++;
            values.put(index, 1d);
        }
    }

    public double averageHoursPerDay() {
        return total_hours / (days_present + partial_days_present);
    }

    public double attendanceRate() {
        double total = 0d;
        for (Map.Entry<Integer, Double> v : values.entrySet()) {
            total += v.getValue();
        }
        return total / values.size();
    }

    public double weightedAttendanceRate() {
        double total = 0d;
        double reference_total = 0d;
        for (Map.Entry<Integer, Double> v : values.entrySet()) {
            Integer index = v.getKey();
            Double value = v.getValue();
            total += weightFunction(index, value);
            reference_total += weightFunction(index, 1d);
        }
        return total / reference_total;
    }

    private double weightFunction(Integer index, Double value) {
        // The way the weighted attendance rate works is that the present day is worth 100%,
        // and a long time in the past (i.e. several months ago) is worth close to 0%,
        // with a smooth transition in between.
        // reference_days is the number of school days in the past when the weight reaches 20%,
        // so you can adjust this to stretch or compress the curve. I've chosen 60 because
        // this is equivalent to 3 months. You could change it to e.g. 45 to make recent
        // days weighted more as compared to older days.
        double reference_days = 60d;
        double curve_constant = Math.pow(5d/(4d * reference_days), 2);
        // The curve is a Gaussian function, i.e. a bell curve, so near present day (index = 0)
        // the weight decreases slowly at first, then faster, then reaches an inflection point
        // and starts slowing down again, and finally has a long tail that goes out to infinity.
        // Try graphing this function with a graphing app so you can visualize it.
        return value * Math.exp(-curve_constant * Math.pow(index, 2));
    }
}
