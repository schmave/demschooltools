package controllers;

import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.*;
import models.*;
import play.libs.Json;
import play.mvc.*;
import org.apache.commons.lang3.StringUtils;

@Secured.Auth(UserRole.ROLE_CHECKIN_APP)
public class Checkin extends Controller {

  public String cleanTimeString(String time) {
    // As of April 2023, some browsers are including non-breaking spaces in
    // their localized time strings, so we need to replace them with regular
    // spaces before trying to parse.
    return time.replace('\u00A0', ' ').replace('\u2007', ' ').replace('\u202F', ' ');
  }

  public Result checkinData(String time, Http.Request request) throws ParseException {
    time = cleanTimeString(time);
    Date date = new SimpleDateFormat("M/d/yyyy, h:mm:ss a").parse(time);

    Date start_date = ModelUtils.getStartOfYear();
    Date end_date = new Date();
    Organization organization = Utils.getOrg(request);
    Map<Person, AttendanceStats> person_to_stats =
        Attendance.mapPeopleToStats(start_date, end_date, organization);

    boolean show_attendance_rate = organization.getAttendanceShowRateInCheckin();
    boolean use_weighted_attendance_rate = organization.getAttendanceShowWeightedPercent();

    List<CheckinPerson> people =
        Application.attendancePeople(organization).stream()
            .sorted(Comparator.comparing(Person::getDisplayName))
            .filter(p -> p.getPin() != null && !p.getPin().isEmpty())
            .map(
                p ->
                    new CheckinPerson(
                        p,
                        AttendanceDay.findCurrentDay(date, p.getPersonId(), organization),
                        person_to_stats.get(p),
                        show_attendance_rate,
                        use_weighted_attendance_rate))
            .collect(Collectors.toList());

    // Automatically set absence code if someone hasn't signed in by a certain time
    String defaultAbsenceCode = organization.getAttendanceDefaultAbsenceCode();
    Time defaultAbsenceCodeTime = organization.getAttendanceDefaultAbsenceCodeTime();
    Time currentTime = new Time((new SimpleDateFormat("h:mm:ss a").parse(time.split(", ")[1])).getTime());
    if (StringUtils.isNotBlank(defaultAbsenceCode) && currentTime.getTime() > defaultAbsenceCodeTime.getTime()) {
      for (CheckinPerson person : people) {
        if (StringUtils.isBlank(person.current_day_code) && 
            StringUtils.isBlank(person.current_day_start_time) &&
            StringUtils.isBlank(person.current_day_end_time)) {
          AttendanceDay attendance_day = AttendanceDay.findCurrentDay(date, person.personId, organization);
          if (attendance_day != null) {
            attendance_day.setCode(defaultAbsenceCode);
            attendance_day.update();
          }
        }
      }
    }

    // add admin
    Person admin = new Person();
    admin.setPersonId(-1);
    admin.setFirstName("Admin");
    admin.setPin(organization.getAttendanceAdminPin());
    people.add(0, new CheckinPerson(admin, null, null, show_attendance_rate, use_weighted_attendance_rate));

    List<String> absence_codes =
        AttendanceCode.all(organization).stream()
            .map(c -> c.getCode())
            .collect(Collectors.toList());

    return ok(Json.stringify(Json.toJson(new CheckinData(people, absence_codes))));
  }

  public Result checkinMessage(
      String time_string, int personId, boolean is_arriving, Http.Request request)
      throws ParseException {
    time_string = cleanTimeString(time_string);
    Date date = new SimpleDateFormat("M/d/yyyy, h:mm:ss a").parse(time_string);
    Time time = new Time(date.getTime());
    AttendanceDay attendance_day =
        AttendanceDay.findCurrentDay(date, personId, Utils.getOrg(request));
    // if this is an invalid day, ignore the message
    if (attendance_day == null) {
      return ok();
    }
    // If someone arrives or leaves, clear the absence code.
    if (attendance_day.getCode() != null && !attendance_day.getCode().isEmpty()) {
      attendance_day.setCode(null);
    }
    // Don't overwrite the start time. This way the earliest start time is the one we use.
    if (is_arriving && attendance_day.getStartTime() == null) {
      attendance_day.setStartTime(time);
    }
    // DO overwrite the end time. This way the latest end time is the one we use.
    else if (!is_arriving) {
      attendance_day.setEndTime(time);
    }
    attendance_day.update();
    return ok();
  }

  public Result adminMessage(
      int personId,
      String in_time,
      String out_time,
      String absence_code,
      String time_string,
      Http.Request request)
      throws Exception {
    Date date = new Date();
    // We use time_string to determine which day it is according to the client. This could be
    // different
    // from the current day according to the server, depending on the client's time zone.
    time_string = cleanTimeString(time_string);
    if (!time_string.isEmpty()) {
      date = new SimpleDateFormat("M/d/yyyy, h:mm:ss a").parse(time_string);
    }
    AttendanceDay attendance_day =
        AttendanceDay.findCurrentDay(date, personId, Utils.getOrg(request));
    if (attendance_day != null) {
      attendance_day.edit(absence_code, in_time, out_time);
    }
    return ok();
  }
}
