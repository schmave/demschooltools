package models;

import java.text.SimpleDateFormat;

public class CheckinPerson {
    
    public int person_id;
    public String pin;
    public String name;
    public String current_day_code;
    public String current_day_start_time;
    public String current_day_end_time;
    public String attendance_rate;

    public CheckinPerson(Person person, AttendanceDay current_day, AttendanceStats stats, boolean show_weighted_percent) {
        person_id = person.person_id;
        pin = person.pin;
        name = person.getDisplayName();

        if (stats != null && show_weighted_percent) {
            double rate = stats.weightedAttendanceRate();
            if (!Double.isNaN(rate)) {
                attendance_rate = ModelUtils.formatAsPercent(rate);
            }
        }

        if (current_day != null) {
            SimpleDateFormat format = new SimpleDateFormat("h:mm aa");
        	current_day_code = current_day.code;
            if (current_day.start_time != null) {
                current_day_start_time = format.format(current_day.start_time.getTime());
            }
            if (current_day.end_time != null) {
        	   current_day_end_time = format.format(current_day.end_time.getTime());
            }
        }
    }
}
