package controllers;

import com.csvreader.CsvWriter;
import io.ebean.DB;
import io.ebean.Expr;
import io.ebean.SqlRow;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.inject.Inject;
import models.*;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.twirl.api.Html;
import views.html.*;

@Secured.Auth(UserRole.ROLE_ATTENDANCE)
public class Attendance extends Controller {

  FormFactory mFormFactory;
  final MessagesApi mMessagesApi;

  @Inject
  public Attendance(FormFactory ff, MessagesApi messagesApi) {
    this.mFormFactory = ff;
    mMessagesApi = messagesApi;
  }

  Html renderIndexContent(
      Date start_date,
      Date end_date,
      Boolean is_custom_date,
      Organization org,
      Http.Request request) {
    if (end_date == null) {
      end_date = new Date(start_date.getTime());
      end_date.setYear(end_date.getYear() + 1);
    }
    Map<Person, AttendanceStats> person_to_stats = mapPeopleToStats(start_date, end_date, org);
    Map<String, AttendanceCode> codes_map = getCodesMap(false, org);

    List<Person> all_people = new ArrayList<>(person_to_stats.keySet());
    all_people.sort(Person.SORT_FIRST_NAME);

    List<String> all_codes = new ArrayList<>(codes_map.keySet());

    Date prev_date = new Date(start_date.getTime());
    prev_date.setYear(prev_date.getYear() - 1);
    Date next_date = new Date(start_date.getTime());
    next_date.setYear(next_date.getYear() + 1);
    if (start_date.getYear() == ModelUtils.getStartOfYear().getYear()) {
      next_date = null;
    }

    return attendance_index.render(
        all_people,
        person_to_stats,
        all_codes,
        codes_map,
        Application.attendancePeople(org),
        start_date,
        end_date,
        is_custom_date,
        prev_date,
        next_date,
        request,
        mMessagesApi.preferred(request));
  }

  public Result index(
      String start_date_str, String end_date_str, Boolean is_custom_date, Http.Request request) {
    if (start_date_str.equals("")) {
      return ok(
          renderIndexContent(
              ModelUtils.getStartOfYear(), null, false, Utils.getOrg(request), request));
    } else {
      Date start_date = Utils.parseDateOrNow(start_date_str).getTime();
      Date end_date = null;
      if (!end_date_str.equals("")) {
        end_date = Utils.parseDateOrNow(end_date_str).getTime();
      }
      return ok(
          renderIndexContent(start_date, end_date, is_custom_date, Utils.getOrg(request), request));
    }
  }

  public Result viewOrEditWeek(String date, boolean do_view, Http.Request request) {
    Calendar start_date = Utils.parseDateOrNow(date);
    Utils.adjustToPreviousDay(start_date, Calendar.MONDAY);

    Calendar end_date = (Calendar) start_date.clone();
    end_date.add(Calendar.DAY_OF_MONTH, 5);

    Organization org = Utils.getOrg(request);
    List<AttendanceDay> days =
        AttendanceDay.find
            .query()
            .where()
            .eq("person.organization", org)
            .ge("day", start_date.getTime())
            .lt("day", end_date.getTime())
            .order("day ASC")
            .findList();

    Map<Person, List<AttendanceDay>> person_to_days = new HashMap<>();

    for (AttendanceDay day : days) {
      List<AttendanceDay> list =
          person_to_days.containsKey(day.getPerson())
              ? person_to_days.get(day.getPerson())
              : new ArrayList<>();

      list.add(day);
      person_to_days.put(day.getPerson(), list);
    }

    List<AttendanceWeek> weeks =
        AttendanceWeek.find
            .query()
            .where()
            .eq("person.organization", org)
            .eq("monday", start_date.getTime())
            .findList();

    Map<Person, AttendanceWeek> person_to_week = new HashMap<>();

    for (AttendanceWeek week : weeks) {
      person_to_week.put(week.getPerson(), week);
    }

    List<Person> all_people = new ArrayList<>(person_to_days.keySet());
    for (Person p : person_to_week.keySet()) {
      if (!all_people.contains(p)) {
        all_people.add(p);
      }
    }
    all_people.sort(Person.SORT_FIRST_NAME);

    Map<String, AttendanceCode> codes = getCodesMap(do_view, org);

    if (do_view) {
      return ok(
          attendance_week.render(
              start_date.getTime(),
              codes,
              all_people,
              person_to_days,
              person_to_week,
              request,
              mMessagesApi.preferred(request)));
    } else {
      List<Person> additional_people = Application.attendancePeople(org);
      additional_people.removeAll(all_people);

      additional_people.sort(Person.SORT_DISPLAY_NAME);
      return ok(edit_attendance_week.render(
              start_date.getTime(),
              codes,
              all_people,
              additional_people,
              person_to_days,
              person_to_week,
              request,
              mMessagesApi.preferred(request)))
          .withHeader("Cache-Control", "max-age=0, no-cache, no-store")
          .withHeader("Pragma", "no-cache");
    }
  }

  public Result jsonPeople(String term, Http.Request request) {
    List<Person> name_matches =
        Person.find
            .query()
            .where()
            .add(
                Expr.or(
                    Expr.ilike("lastName", "%" + term + "%"),
                    Expr.or(
                        Expr.ilike("firstName", "%" + term + "%"),
                        Expr.ilike("displayName", "%" + term + "%"))))
            .eq("organization", Utils.getOrg(request))
            .eq("isFamily", false)
            .findList();

    List<Person> selected_people = new ArrayList<>();
    for (Person p : name_matches) {
      if (!p.attendance_days.isEmpty()) {
        selected_people.add(p);
      }
    }
    selected_people.sort(Person.SORT_FIRST_NAME);

    List<Map<String, String>> result = new ArrayList<>();
    for (Person p : selected_people) {
      HashMap<String, String> values = new HashMap<>();
      String label = p.getFirstName();
      if (p.getLastName() != null) {
        label += " " + p.getLastName();
      }
      if (p.getDisplayName() != null && !p.getDisplayName().equals("")) {
        label += " (\"" + p.getDisplayName() + "\")";
      }
      values.put("label", label);
      values.put("id", "" + p.getPersonId());
      result.add(values);
    }

    return ok(Json.stringify(Json.toJson(result)));
  }

  public Result viewPersonReport(
      Integer personId, String start_date_str, String end_date_str, Http.Request request) {
    Organization org = Utils.getOrg(request);
    Person p = Person.findById(personId, org);

    Date start_date = ModelUtils.getStartOfYear();
    Date end_date = new Date();

    if (!start_date_str.trim().isEmpty() || !end_date_str.trim().isEmpty()) {
      try {
        start_date = new SimpleDateFormat("yyyy-M-d").parse(start_date_str);
        end_date = new SimpleDateFormat("yyyy-M-d").parse(end_date_str);
      } catch (ParseException e) {
      }
    } else {
      AttendanceDay last_day =
          AttendanceDay.find
              .query()
              .where()
              .eq("person", p)
              .order("day DESC")
              .setMaxRows(1)
              .findOne();
      if (last_day != null) {
        end_date = last_day.getDay();
        start_date = ModelUtils.getStartOfYear(end_date);
      }
    }

    String sql =
        "select min(day) as min_date, max(day) as max_date from attendance_day where "
            + "person_id=:personId and "
            + "((code is not null and code != '_NS_') or "
            + "(start_time is not null and end_time is not null)) "
            + "group by person_id";
    SqlRow row = DB.sqlQuery(sql).setParameter("personId", p.getPersonId()).findOne();

    List<AttendanceDay> days =
        AttendanceDay.find
            .query()
            .where()
            .eq("person", p)
            .ge("day", start_date)
            .le("day", end_date)
            .order("day ASC")
            .findList();

    List<AttendanceWeek> weeks =
        AttendanceWeek.find
            .query()
            .where()
            .eq("person", p)
            .ge("monday", start_date)
            .le("monday", end_date)
            .findList();

    List<List<AttendanceDay>> school_days = listSchoolDays(start_date, end_date, org);

    Map<String, AttendanceCode> codes_map = getCodesMap(true, org);
    AttendanceStats stats = new AttendanceStats(org);
    boolean has_off_campus_time = false;

    for (int i = 0; i < school_days.size(); i++) {
      List<AttendanceDay> school_day = school_days.get(i);
      AttendanceDay day =
          school_day.stream()
              .filter(d -> d.getPerson().getPersonId().equals(personId))
              .findAny()
              .orElse(null);

      if (day != null) {
        stats.processDay(day, i, codes_map, org);
        if (day.getOffCampusDepartureTime() != null || day.getOffCampusReturnTime() != null) {
          has_off_campus_time = true;
        }
      }
    }

    for (AttendanceWeek week : weeks) {
      stats.total_hours += week.getExtraHours();
    }

    Map<Date, AttendanceWeek> day_to_week = new HashMap<>();
    for (AttendanceWeek w : weeks) {
      day_to_week.put(w.getMonday(), w);
    }

    List<String> codes =
        new ArrayList<>(codes_map.keySet())
            .stream().filter(c -> !Objects.equals(c, "_NS_")).collect(Collectors.toList());

    int table_width = has_off_campus_time ? 900 : 700;

    return ok(
        attendance_person.render(
            p,
            days,
            day_to_week,
            codes,
            codes_map,
            stats,
            start_date,
            end_date,
            row == null ? null : row.getDate("min_date"),
            row == null ? null : row.getDate("max_date"),
            has_off_campus_time,
            table_width,
            request,
            mMessagesApi.preferred(request)));
  }

  public Result createPersonWeek(Http.Request request) {
    Map<String, String[]> data = request.body().asFormUrlEncoded();
    Calendar start_date = Utils.parseDateOrNow(data.get("monday")[0]);

    ArrayList<Object> result = new ArrayList<>();
    String[] person_ids = data.get("personId[]");

    if (person_ids == null) {
      return badRequest("No personId[] found");
    }

    for (String personId : person_ids) {
      Calendar end_date = (Calendar) start_date.clone();
      boolean alreadyExists = false;
      Person p = Person.findById(Integer.parseInt(personId), Utils.getOrg(request));
      try {
        AttendanceWeek.create(start_date.getTime(), p);
      } catch (javax.persistence.PersistenceException pe) {
        ModelUtils.eatIfUniqueViolation(pe);
        alreadyExists = true;
      }
      Map<String, Object> one_result = new HashMap<>();

      // look up our newly-created object so that we get the ID
      one_result.put(
          "week",
          AttendanceWeek.find
              .query()
              .where()
              .eq("person", p)
              .eq("monday", start_date.getTime())
              .findOne());

      for (int i = 0; i < 5; i++) {
        try {
          AttendanceDay.create(end_date.getTime(), p);
        } catch (javax.persistence.PersistenceException pe) {
          ModelUtils.eatIfUniqueViolation(pe);
          alreadyExists = true;
        }
        end_date.add(Calendar.DAY_OF_MONTH, 1);
      }

      one_result.put(
          "days",
          AttendanceDay.find
              .query()
              .where()
              .eq("person", p)
              .ge("day", start_date.getTime())
              .le("day", end_date.getTime())
              .order("day ASC")
              .setMaxRows(5)
              .findList());

      if (!alreadyExists) {
        result.add(one_result);
      }
    }

    return ok(Utils.toJson(result));
  }

  public Result deletePersonWeek(int personId, String monday, Http.Request request) {

    Calendar start_date = Utils.parseDateOrNow(monday);
    Calendar end_date = (Calendar) start_date.clone();
    end_date.add(Calendar.DAY_OF_MONTH, 5);

    Person p = Person.findById(personId, Utils.getOrg(request));

    List<AttendanceDay> days =
        AttendanceDay.find
            .query()
            .where()
            .eq("person", p)
            .ge("day", start_date.getTime())
            .le("day", end_date.getTime())
            .findList();

    for (AttendanceDay day : days) {
      day.delete();
    }

    List<AttendanceWeek> weeks =
        AttendanceWeek.find
            .query()
            .where()
            .eq("person", p)
            .eq("monday", start_date.getTime())
            .findList();

    for (AttendanceWeek week : weeks) {
      week.delete();
    }

    return ok();
  }

  public Result download(String start_date_str, Http.Request request) throws IOException {
    Date start_date = ModelUtils.getStartOfYear();
    if (!start_date_str.equals("")) {
      start_date = Utils.parseDateOrNow(start_date_str).getTime();
    }
    Date end_date = new Date(start_date.getTime());
    end_date.setYear(end_date.getYear() + 1);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Charset charset = StandardCharsets.UTF_8;
    CsvWriter writer = new CsvWriter(baos, ',', charset);

    writer.write("Name");
    writer.write("Day");
    writer.write("Absence code");
    writer.write("Arrival time");
    writer.write("Departure time");
    writer.write("Extra hours");
    writer.endRecord();

    final Organization org = Utils.getOrg(request);
    List<AttendanceDay> days =
        AttendanceDay.find
            .query()
            .where()
            .eq("person.organization", org)
            .ge("day", start_date)
            .le("day", end_date)
            .findList();

    final OrgConfig orgConfig = Utils.getOrgConfig(org);
    for (AttendanceDay day : days) {
      writer.write(day.getPerson().getFirstName() + " " + day.getPerson().getLastName());
      writer.write(Application.yymmddDate(orgConfig, day.getDay()));
      if (day.getCode() != null) {
        writer.write(day.getCode());
        writer.write(""); // empty startTime and endTime
        writer.write("");
      } else {
        writer.write("");
        writer.write(day.getStartTime() != null ? day.getStartTime().toString() : "");
        writer.write(day.getEndTime() != null ? day.getEndTime().toString() : "");
      }
      writer.write(""); // no extra hours
      writer.endRecord();
    }

    List<AttendanceWeek> weeks =
        AttendanceWeek.find
            .query()
            .where()
            .eq("person.organization", org)
            .ge("monday", start_date)
            .le("monday", end_date)
            .findList();

    for (AttendanceWeek week : weeks) {
      writer.write(week.getPerson().getFirstName() + " " + week.getPerson().getLastName());
      writer.write(Application.yymmddDate(orgConfig, week.getMonday()));
      for (int i = 0; i < 3; i++) {
        writer.write("");
      }
      writer.write("" + week.getExtraHours());
      writer.endRecord();
    }

    writer.close();

    ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(zipBytes);
    zos.putNextEntry(new ZipEntry("attendance/all_data.csv"));
    // Adding the BOM here causes Excel 2010 on Windows to realize
    // that the file is Unicode-encoded.
    zos.write("\ufeff".getBytes(charset));
    zos.write(baos.toByteArray());
    zos.closeEntry();

    TreeSet<Date> allDates = new TreeSet<>();
    TreeMap<String, HashMap<Date, AttendanceDay>> personDateAttendance = new TreeMap<>();
    for (AttendanceDay day : days) {
      allDates.add(day.getDay());

      String name = day.getPerson().getFirstName() + " " + day.getPerson().getLastName();
      if (!personDateAttendance.containsKey(name)) {
        personDateAttendance.put(name, new HashMap<>());
      }
      personDateAttendance.get(name).put(day.getDay(), day);
    }

    zos.putNextEntry(new ZipEntry("attendance/daily_hours.csv"));
    // Adding the BOM here causes Excel 2010 on Windows to realize
    // that the file is Unicode-encoded.
    zos.write("\ufeff".getBytes(charset));
    zos.write(getDailyHoursFile(allDates, personDateAttendance, charset, orgConfig));
    zos.closeEntry();

    zos.putNextEntry(new ZipEntry("attendance/daily_signins.csv"));
    // Adding the BOM here causes Excel 2010 on Windows to realize
    // that the file is Unicode-encoded.
    zos.write("\ufeff".getBytes(charset));
    zos.write(getDailySigninsFile(allDates, personDateAttendance, charset, orgConfig));
    zos.closeEntry();

    zos.close();
    return ok(zipBytes.toByteArray())
        .as("application/zip")
        .withHeader("Content-Disposition", "attachment; filename=attendance.zip");
  }

  private byte[] getDailyHoursFile(
      TreeSet<Date> allDates,
      TreeMap<String, HashMap<Date, AttendanceDay>> personDateAttendance,
      Charset charset,
      OrgConfig orgConfig)
      throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    CsvWriter writer = new CsvWriter(baos, ',', charset);
    writer.write("Name");
    for (Date d : allDates) {
      writer.write(Application.yymmddDate(orgConfig, d));
    }
    writer.endRecord();

    for (String name : personDateAttendance.keySet()) {
      writer.write(name);
      for (Date date : allDates) {
        AttendanceDay day = personDateAttendance.get(name).get(date);
        if (day == null || day.getStartTime() == null || day.getEndTime() == null) {
          writer.write(day == null || day.getCode() == null ? "" : day.getCode());
        } else {
          writer.write(String.format("%.2f", day.getHours()));
        }
      }
      writer.endRecord();
    }

    writer.close();
    return baos.toByteArray();
  }

  private byte[] getDailySigninsFile(
      TreeSet<Date> allDates,
      TreeMap<String, HashMap<Date, AttendanceDay>> personDateAttendance,
      Charset charset,
      OrgConfig orgConfig)
      throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    CsvWriter writer = new CsvWriter(baos, ',', charset);
    writer.write("Name");
    for (Date d : allDates) {
      writer.write(Application.yymmddDate(orgConfig, d));
    }
    writer.endRecord();

    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
    for (String name : personDateAttendance.keySet()) {
      for (int x = 0; x < 2; x++) {
        writer.write(x == 0 ? name : "");
        for (Date date : allDates) {
          AttendanceDay day = personDateAttendance.get(name).get(date);
          if (day == null || day.getStartTime() == null || day.getEndTime() == null) {
            writer.write(day == null || day.getCode() == null ? "" : day.getCode());
          } else {
            writer.write(dateFormat.format(x == 0 ? day.getStartTime() : day.getEndTime()));
          }
        }
        writer.endRecord();
      }
    }

    writer.close();
    return baos.toByteArray();
  }

  public Result viewWeek(String date, Http.Request request) {

    return viewOrEditWeek(date, true, request);
  }

  public Result signInSheet(Http.Request request) {

    Organization org = Utils.getOrg(request);
    String peopleJson = Application.attendancePeopleWithTagsJson(org);
    String tagsJson = Application.attendanceTagsJson(org);
    String allPeopleJson = Application.allPeopleJson(org);
    return ok(
        attendance_sign_in_sheet.render(
            peopleJson, tagsJson, allPeopleJson, request, mMessagesApi.preferred(request)));
  }

  public Result editWeek(String date, Http.Request request) {
    return viewOrEditWeek(date, false, request);
  }

  public Result saveWeek(Integer week_id, Double extraHours, Http.Request request) {

    AttendanceWeek.find.byId(week_id).edit(extraHours);
    return ok();
  }

  public Result saveDay(
      Integer day_id, String code, String startTime, String endTime, Http.Request request)
      throws Exception {

    AttendanceDay.find.byId(day_id).edit(code, startTime, endTime);
    return ok();
  }

  public static Map<String, AttendanceCode> getCodesMap(
      boolean include_no_school, Organization org) {
    Map<String, AttendanceCode> codes = new HashMap<>();

    for (AttendanceCode code : AttendanceCode.all(org)) {
      codes.put(code.getCode(), code);
    }

    if (include_no_school) {
      AttendanceCode no_school = new AttendanceCode();
      no_school.setDescription("No school");
      no_school.setColor("#cc9");
      no_school.setCode("_NS_");
      codes.put("_NS_", no_school);
    }

    return codes;
  }

  public static Map<Person, AttendanceStats> mapPeopleToStats(
      Date start_date, Date end_date, Organization org) {
    Map<Person, AttendanceStats> person_to_stats = new HashMap<>();
    Map<String, AttendanceCode> codes_map = getCodesMap(false, org);

    List<List<AttendanceDay>> school_days = listSchoolDays(start_date, end_date, org);

    for (int i = 0; i < school_days.size(); i++) {
      List<AttendanceDay> school_day = school_days.get(i);
      for (AttendanceDay day : school_day) {
        if (!person_to_stats.containsKey(day.getPerson())) {
          person_to_stats.put(day.getPerson(), new AttendanceStats(org));
        }
        AttendanceStats stats = person_to_stats.get(day.getPerson());
        stats.processDay(day, i, codes_map, org);
      }
    }

    List<AttendanceWeek> weeks =
        AttendanceWeek.find
            .query()
            .where()
            .eq("person.organization", org)
            .ge("monday", start_date)
            .le("monday", end_date)
            .findList();

    for (AttendanceWeek week : weeks) {
      if (!person_to_stats.containsKey(week.getPerson())) {
        person_to_stats.put(week.getPerson(), new AttendanceStats(org));
      }

      AttendanceStats stats = person_to_stats.get(week.getPerson());
      stats.total_hours += week.getExtraHours();
    }

    return person_to_stats;
  }

  public static List<List<AttendanceDay>> listSchoolDays(
      Date start_date, Date end_date, Organization org) {
    List<List<AttendanceDay>> school_days = new ArrayList<>();

    List<AttendanceDay> days =
        AttendanceDay.find
            .query()
            .where()
            .eq("person.organization", org)
            .ge("day", start_date)
            .le("day", end_date)
            .findList();

    // group AttendanceDays by date, sorted into reverse chronological order
    SortedMap<Date, List<AttendanceDay>> groups = new TreeMap<>(Collections.reverseOrder());
    for (AttendanceDay day : days) {
      if (!groups.containsKey(day.getDay())) {
        groups.put(day.getDay(), new ArrayList<>());
      }
      List<AttendanceDay> group = groups.get(day.getDay());
      group.add(day);
    }

    for (List<AttendanceDay> group : groups.values()) {
      // Iterate through all AttendanceDays in the group until we find one that has some attendance
      // data that's not _NS_. If we don't find any, the date is not a school day.
      for (AttendanceDay day : group) {
        boolean is_absence = day.getCode() != null && !day.getCode().equals("_NS_");
        boolean is_attendance = day.getStartTime() != null && day.getEndTime() != null;
        if (is_absence || is_attendance) {
          school_days.add(group);
          break;
        }
      }
    }
    return school_days;
  }

  public Result viewCustodiaAdmin(Http.Request request) {
    return ok(
        main_with_mustache.render(
            "Sign in system",
            "custodia",
            "",
            "custodia_admin.html",
            new HashMap<>(),
            request,
            mMessagesApi.preferred(request)));
  }

  public Result rules(Http.Request request) {
    Organization org = Utils.getOrg(request);
    Map<String, AttendanceCode> codes_map = getCodesMap(false, org);
    List<AttendanceRule> current_rules = AttendanceRule.currentRules(new Date(), null, org);
    List<AttendanceRule> future_rules = AttendanceRule.futureRules(new Date(), org);
    List<AttendanceRule> past_rules = AttendanceRule.pastRules(new Date(), org);
    AttendanceRule.sortCurrentRules(current_rules);
    AttendanceRule.sortCurrentRules(future_rules);
    AttendanceRule.sortPastRules(past_rules);

    return ok(
        attendance_rules.render(
            current_rules,
            future_rules,
            past_rules,
            codes_map,
            org.getAttendanceEnablePartialDays(),
            org.getAttendanceShowReports(),
            request,
            mMessagesApi.preferred(request)));
  }

  public Result newRule(Http.Request request) {
    return rule(null, request);
  }

  public Result rule(Integer id, Http.Request request) {
    Organization org = Utils.getOrg(request);
    String people_json = Application.attendancePeopleJson(org);
    AttendanceRule rule = id == null ? new AttendanceRule() : AttendanceRule.findById(id, org);
    return ok(
        attendance_edit_rule.render(
            rule,
            people_json,
            org.getAttendanceEnablePartialDays(),
            org.getAttendanceShowReports(),
            request,
            mMessagesApi.preferred(request)));
  }

  public Result saveRule(Http.Request request) throws Exception {
    Form<AttendanceRule> form = mFormFactory.form(AttendanceRule.class);
    Form<AttendanceRule> filledForm = form.bindFromRequest(request);
    AttendanceRule.save(filledForm, Utils.getOrg(request));
    return redirect(routes.Attendance.rules());
  }

  public Result deleteRule(Integer id, Http.Request request) {
    AttendanceRule.delete(id);
    return redirect(routes.Attendance.rules());
  }

  public Result offCampusTime(Http.Request request) {
    List<AttendanceDay> events =
        AttendanceDay.find
            .query()
            .where()
            .eq("person.organization", Utils.getOrg(request))
            .gt("day", ModelUtils.getStartOfYear())
            .ne("offCampusDepartureTime", null)
            .order("day ASC")
            .findList();

    return ok(attendance_off_campus.render(events, request, mMessagesApi.preferred(request)));
  }

  public Result deleteOffCampusTime(Http.Request request) {
    DynamicForm form = mFormFactory.form().bindFromRequest(request);
    Map<String, String> data = form.rawData();
    for (Map.Entry<String, String> entry : data.entrySet()) {
      if (entry.getKey().isEmpty()) continue;
      Integer attendance_day_id = Integer.parseInt(entry.getKey());
      AttendanceDay attendance_day =
          AttendanceDay.findById(attendance_day_id, Utils.getOrg(request));
      attendance_day.setOffCampusDepartureTime(null);
      attendance_day.setOffCampusReturnTime(null);
      attendance_day.update();
    }
    return redirect(routes.Attendance.offCampusTime());
  }

  public Result addOffCampusTime(Http.Request request) {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    Date today = Calendar.getInstance().getTime();
    String current_date = df.format(today);
    String people_json = Application.attendancePeopleJson(Utils.getOrg(request));
    return ok(
        attendance_add_off_campus.render(
            current_date, people_json, request, mMessagesApi.preferred(request)));
  }

  public Result saveOffCampusTime(Http.Request request) throws ParseException {
    DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
    DynamicForm form = mFormFactory.form().bindFromRequest(request);
    Map<String, String> data = form.rawData();

    for (int i = 0; i < 10; i++) {
      try {
        Integer personId = Integer.parseInt(data.get("personid-" + i));
        Date day = df.parse(data.get("day-" + i));
        Time departure_time = AttendanceDay.parseTime(data.get("departuretime-" + i));
        Time return_time = AttendanceDay.parseTime(data.get("returntime-" + i));
        Integer minutes_exempted = AttendanceDay.parseInt(data.get("minutesexempted-" + i));

        if (day != null && departure_time != null && return_time != null) {
          AttendanceDay attendance_day =
              AttendanceDay.findCurrentDay(day, personId, Utils.getOrg(request));
          attendance_day.setOffCampusDepartureTime(departure_time);
          attendance_day.setOffCampusReturnTime(return_time);
          attendance_day.setOffCampusMinutesExempted(minutes_exempted);
          attendance_day.update();
        }
      } catch (NumberFormatException e) {
      }
    }
    return redirect(routes.Attendance.offCampusTime());
  }

  public Result reports(Http.Request request) {
    return ok(
        attendance_reports.render(
            new AttendanceReport(), request, mMessagesApi.preferred(request)));
  }

  public Result runReport(Http.Request request) {
    Form<AttendanceReport> form =
        mFormFactory.form(AttendanceReport.class).withDirectFieldAccess(true);
    Form<AttendanceReport> filledForm = form.bindFromRequest(request);
    if (filledForm.hasErrors()) {
      System.out.println("ERRORS: " + filledForm.errorsAsJson().toString());
      return badRequest(
          attendance_reports.render(
              new AttendanceReport(), request, mMessagesApi.preferred(request)));
    }
    AttendanceReport report = AttendanceReport.createFromForm(filledForm, Utils.getOrg(request));
    return ok(attendance_reports.render(report, request, mMessagesApi.preferred(request)));
  }

  public Result assignPINs(Http.Request request) {
    Organization org = Utils.getOrg(request);
    List<Person> people = Application.attendancePeople(org);
    people.sort(Person.SORT_DISPLAY_NAME);
    // add admin PIN
    Person admin = new Person();
    admin.setPersonId(-1);
    admin.setFirstName("Admin");
    admin.setPin(org.getAttendanceAdminPin());
    people.add(0, admin);
    return ok(attendance_pins.render(people, request, mMessagesApi.preferred(request)));
  }

  public Result savePINs(Http.Request request) {
    List<Person> people = Application.attendancePeople(Utils.getOrg(request));
    HashMap<Integer, Person> people_by_id = new HashMap<>();
    for (Person p : people) {
      people_by_id.put(p.getPersonId(), p);
    }
    Set<Map.Entry<String, String[]>> entries = request.queryString().entrySet();
    for (Map.Entry<String, String[]> entry : entries) {
      Integer personId = Integer.parseInt(entry.getKey());
      if (personId == -1) {
        Organization organization = Utils.getOrg(request);
        organization.setAttendanceAdminPin(entry.getValue()[0]);
        organization.save();
      } else {
        Person person = people_by_id.get(personId);
        if (person != null) {
          person.setPin(entry.getValue()[0]);
          person.save();
        }
      }
    }
    return redirect(routes.Attendance.assignPINs());
  }

  Form<AttendanceCode> getCodeForm() {
    return mFormFactory.form(AttendanceCode.class);
  }

  public Result viewCodes(Http.Request request) {
    return ok(
        attendance_codes.render(
            AttendanceCode.all(Utils.getOrg(request)),
            getCodeForm(),
            request,
            mMessagesApi.preferred(request)));
  }

  public Result newCode(Http.Request request) {
    AttendanceCode ac = AttendanceCode.create(Utils.getOrg(request));
    Form<AttendanceCode> filled_form = getCodeForm().bindFromRequest(request);
    ac.edit(filled_form);

    return redirect(routes.Attendance.viewCodes());
  }

  public Result editCode(Integer code_id, Http.Request request) {
    AttendanceCode ac = AttendanceCode.findById(code_id, Utils.getOrg(request));
    Form<AttendanceCode> filled_form = getCodeForm().fill(ac);
    return ok(edit_attendance_code.render(filled_form, request, mMessagesApi.preferred(request)));
  }

  public Result saveCode(Http.Request request) {
    Form<AttendanceCode> filled_form = getCodeForm().bindFromRequest(request);
    AttendanceCode ac =
        AttendanceCode.findById(
            Integer.parseInt(filled_form.field("id").value().get()), Utils.getOrg(request));
    ac.edit(filled_form);

    return redirect(routes.Attendance.viewCodes());
  }

  public static String formatTime(Time t) {
    if (t != null) {
      return new SimpleDateFormat("h:mm a").format(t);
    } else {
      return "---";
    }
  }

  public static int getDaysPresent(List<AttendanceDay> days) {
    int result = 0;
    for (AttendanceDay day : days) {
      if (day.getCode() == null && day.getStartTime() != null && day.getEndTime() != null) {
        result++;
      }
    }

    return result;
  }

  public static double getTotalHours(List<AttendanceDay> days, AttendanceWeek week) {
    double result = 0;
    for (AttendanceDay day : days) {
      if (day.getCode() == null && day.getStartTime() != null && day.getEndTime() != null) {
        result += day.getHours();
      }
    }

    if (week != null) {
      result += week.getExtraHours();
    }

    return result;
  }

  public static double getAverageHours(List<AttendanceDay> days, AttendanceWeek week) {
    int daysPresent = getDaysPresent(days);
    if (daysPresent == 0) {
      return 0;
    }

    return getTotalHours(days, week) / (double) daysPresent;
  }

  public static String format(double d) {
    return String.format("%,.1f", d);
  }

  public static String formatAsPercent(double d) {
    return String.format("%,.1f", d * 100) + "%";
  }
}
