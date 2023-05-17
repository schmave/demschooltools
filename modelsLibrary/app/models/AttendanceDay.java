package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.ebean.*;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class AttendanceDay extends Model {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attendance_day_id_seq")
  private Integer id;

  private Date day;

  @ManyToOne()
  @JoinColumn(name = "person_id")
  private Person person;

  private String code;

  private Time startTime;
  private Time endTime;

  private Time offCampusDepartureTime;
  private Time offCampusReturnTime;
  private Integer offCampusMinutesExempted;

  @Transient private Integer lateFee;

  public static Finder<Integer, AttendanceDay> find = new Finder<>(AttendanceDay.class);

  public static AttendanceDay findById(int id, Organization org) {
    return find.query().where().eq("person.organization", org).eq("id", id).findOne();
  }

  public static AttendanceDay create(Date day, Person p) {
    AttendanceDay result = new AttendanceDay();
    result.person = p;
    result.day = day;

    List<AttendanceRule> rules =
        AttendanceRule.currentRules(day, p.getPersonId(), p.getOrganization());
    for (AttendanceRule rule : rules) {
      String code = rule.getAbsenceCode();
      if (rule.doesMatchDaysOfWeek(day) && code != null && !code.equals("")) {
        result.code = code;
      }
    }

    result.save();
    return result;
  }

  public static Time parseTime(String time_string) {
    if (time_string == null || time_string.equals("")) {
      return null;
    }

    String[] formats = {"h:mm a", "h:mma", "h:mm"};

    for (String format : formats) {
      try {
        Date d = new SimpleDateFormat(format).parse(time_string);
        return new Time(d.getTime());
      } catch (ParseException e) {
      }
    }

    return null;
  }

  public static Integer parseInt(String str) {
    if (str == null || str.equals("")) {
      return null;
    }
    return Integer.parseInt(str);
  }

  public void edit(String code, String startTime, String endTime) throws Exception {
    if (code.equals("")) {
      this.code = null;
    } else {
      this.code = code;
    }

    this.startTime = parseTime(startTime);
    this.endTime = parseTime(endTime);

    this.update();
  }

  @JsonIgnore
  public double getHours() {
    long off_campus_time = 0;
    if (offCampusReturnTime != null && offCampusDepartureTime != null) {
      off_campus_time = offCampusReturnTime.getTime() - offCampusDepartureTime.getTime();
      if (offCampusMinutesExempted != null) {
        off_campus_time -= offCampusMinutesExempted * 60 * 1000;
        if (off_campus_time < 0) {
          off_campus_time = 0;
        }
      }
    }
    return (endTime.getTime() - startTime.getTime() - off_campus_time) / (1000.0 * 60 * 60);
  }

  @JsonIgnore
  public boolean isPartial(Organization org) {
    if (org.getAttendanceEnablePartialDays()) {
      Double min_hours = org.getAttendanceDayMinHours();
      Time latest_start_time = org.getAttendanceDayLatestStartTime();

      List<AttendanceRule> rules =
          AttendanceRule.currentRules(day, person.getPersonId(), person.getOrganization());
      for (AttendanceRule rule : rules) {
        if (rule.doesMatchDaysOfWeek(day)) {
          if (rule.getMinHours() != null) {
            min_hours = rule.getMinHours();
          }
          if (rule.getLatestStartTime() != null) {
            latest_start_time = rule.getLatestStartTime();
          }
        }
      }

      if (min_hours != null && getHours() < min_hours) {
        return true;
      }
      if (latest_start_time != null && startTime.getTime() > latest_start_time.getTime()) {
        return true;
      }
    }
    return false;
  }

  @JsonIgnore
  public String getFormattedDate() {
    SimpleDateFormat sdf = new SimpleDateFormat("M/d/yy");
    return sdf.format(day);
  }

  public static AttendanceDay findCurrentDay(Date day, int personId, Organization org) {
    // if the day is Saturday or Sunday, there can't be an attendance day
    Calendar calendar = new GregorianCalendar();
    calendar.setTime(day);
    int dow = calendar.get(Calendar.DAY_OF_WEEK);
    if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) {
      return null;
    }
    // find or create AttendanceWeek and AttendanceDay objects
    Person person = Person.findById(personId, org);
    AttendanceWeek.findOrCreate(day, person);
    return AttendanceDay.find.query().where().eq("person", person).eq("day", day).findOne();
  }
}
