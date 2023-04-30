package models;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class LateDepartureGroup {

  public String name;
  public List<AttendanceDay> events;

  public Integer lateFee;
  public Integer late_fee_interval;
  public Time latest_departure_time;

  public LateDepartureGroup(String person_name, Organization org) {
    name = person_name;
    events = new ArrayList<>();

    lateFee = org.getAttendanceReportLateFee();
    late_fee_interval = org.getAttendanceReportLateFeeInterval();
    latest_departure_time = org.getAttendanceReportLatestDepartureTime();
  }

  public Integer getTotalOwed() {
    if (lateFee == null || lateFee == 0 || late_fee_interval == null || late_fee_interval == 0) {
      return null;
    }
    int total_owed = 0;
    for (AttendanceDay event : events) {
      int minutes_late =
          (int) (event.getEndTime().getTime() / (60 * 1000))
              - (int) (latest_departure_time.getTime() / (60 * 1000));
      int intervals = (minutes_late + late_fee_interval - 1) / late_fee_interval;
      event.setLateFee(intervals * lateFee);
      total_owed += event.getLateFee();
    }
    return total_owed;
  }
}
