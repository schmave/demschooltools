package models;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class LateDepartureGroup {

  public String name;
  public List<AttendanceDay> events;

  public Integer late_fee;
  public Integer late_fee_2;
  public Integer late_fee_interval;
  public Integer late_fee_interval_2;
  public Time latest_departure_time;
  public Time latest_departure_time_2;

  public LateDepartureGroup(String person_name, Organization org) {
    name = person_name;
    events = new ArrayList<AttendanceDay>();

    late_fee = org.getAttendanceReportLateFee();
    late_fee_2 = org.getAttendanceReportLateFee_2();
    late_fee_interval = org.getAttendanceReportLateFeeInterval();
    late_fee_interval_2 = org.getAttendanceReportLateFeeInterval_2();
    latest_departure_time = org.getAttendanceReportLatestDepartureTime();
    latest_departure_time_2 = org.getAttendanceReportLatestDepartureTime_2();
  }

  public int getTotalOwed() {
    int total_owed = 0;
    for (AttendanceDay event : events) {
      event.setLateFee(calculateFee(event));
      total_owed += event.getLateFee();
    }
    return total_owed;
  }

  private int calculateFee(AttendanceDay event) {
    if (latest_departure_time == null) {
      return 0;
    }
    int fee_1 = 0;
    int fee_2 = 0;
    int minutes_late_1 = timeToMinutes(event.getEndTime()) - timeToMinutes(latest_departure_time);
    if (latest_departure_time_2 != null) {
      int minutes_late_2 =
          timeToMinutes(event.getEndTime()) - timeToMinutes(latest_departure_time_2);
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
    return (int) (time.getTime() / (60 * 1000));
  }
}
