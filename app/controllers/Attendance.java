package controllers;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.Expression;
import com.avaje.ebean.FetchConfig;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;
import com.avaje.ebean.SqlUpdate;
import com.csvreader.CsvWriter;

import models.*;

import play.*;
import play.data.*;
import play.mvc.*;
import play.mvc.Http.Context;

@With(DumpOnError.class)
@Secured.Auth(UserRole.ROLE_ALL_ACCESS)
public class Attendance extends Controller {

    public static final String CACHE_INDEX = "Attendance-index-";

    static Form<AttendanceCode> code_form = Form.form(AttendanceCode.class);

    public static Result index() {
        return ok(views.html.cached_page.render(
            new CachedPage(CACHE_INDEX,
                "Attendance",
                "attendance",
                "attendance_home") {
                @Override
                String render() {
        Map<Person, AttendanceStats> person_to_stats =
            new HashMap<Person, AttendanceStats>();

        Map<String, AttendanceCode> codes_map = getCodesMap();

        List<AttendanceDay> days =
            AttendanceDay.find.where()
                .eq("person.organization", OrgConfig.get().org)
                .ge("day", Application.getStartOfYear())
                .findList();

        for (AttendanceDay day : days) {
            if (!person_to_stats.containsKey(day.person)) {
                person_to_stats.put(day.person, new AttendanceStats());
            }

            AttendanceStats stats = person_to_stats.get(day.person);
            if (day.code != null && day.code.equals("_NS_")) {
                // special no school day code, do nothing.
            } else if (day.code != null || day.start_time == null || day.end_time == null) {
                stats.incrementCodeCount(codes_map.get(day.code));
            } else {
                stats.total_hours += day.getHours();
                stats.days_present++;
            }
        }

        List<AttendanceWeek> weeks =
            AttendanceWeek.find.where()
                .eq("person.organization", OrgConfig.get().org)
                .ge("monday", Application.getStartOfYear())
                .findList();

        for (AttendanceWeek week : weeks) {
            if (!person_to_stats.containsKey(week.person)) {
                person_to_stats.put(week.person, new AttendanceStats());
            }

            AttendanceStats stats = person_to_stats.get(week.person);
            stats.total_hours += week.extra_hours;
        }

        List<Person> all_people = new ArrayList<Person>(person_to_stats.keySet());
        Collections.sort(all_people, Person.SORT_DISPLAY_NAME);

        List<String> all_codes = new ArrayList<String>(codes_map.keySet());

        return views.html.attendance_index.render(
            all_people, person_to_stats, all_codes, codes_map).toString();
                }}));
    }

    public static Result viewOrEditWeek(String date, boolean do_view) {
        Calendar start_date = Utils.parseDateOrNow(date);
        Utils.adjustToPreviousDay(start_date, Calendar.MONDAY);

        Calendar end_date = (Calendar)start_date.clone();
        end_date.add(Calendar.DAY_OF_MONTH, 5);

        List<AttendanceDay> days =
            AttendanceDay.find.where()
                .eq("person.organization", OrgConfig.get().org)
                .ge("day", start_date.getTime())
                .lt("day", end_date.getTime())
                .order("day ASC")
                .findList();

        Map<Person, List<AttendanceDay>> person_to_days =
            new HashMap<Person, List<AttendanceDay>>();

        for (AttendanceDay day : days) {
            List<AttendanceDay> list = person_to_days.containsKey(day.person)
                ? person_to_days.get(day.person)
                : new ArrayList<AttendanceDay>();

            list.add(day);
            person_to_days.put(day.person, list);
        }

        List<AttendanceWeek> weeks =
            AttendanceWeek.find.where()
                .eq("person.organization", OrgConfig.get().org)
                .eq("monday", start_date.getTime())
                .findList();

        Map<Person, AttendanceWeek> person_to_week =
            new HashMap<Person, AttendanceWeek>();

        for (AttendanceWeek week : weeks) {
            person_to_week.put(week.person, week);
        }

        List<Person> all_people = new ArrayList<Person>(person_to_days.keySet());
        for (Person p : person_to_week.keySet()) {
            if (!all_people.contains(p)) {
                all_people.add(p);
            }
        }
        Collections.sort(all_people, Person.SORT_DISPLAY_NAME);

        Map<String, AttendanceCode> codes = getCodesMap();

        if (do_view) {
            return ok(views.html.attendance_week.render(
                start_date.getTime(),
                codes,
                all_people,
                person_to_days,
                person_to_week));
        } else {
            List<Person> additional_people = Application.allPeople();
            additional_people.removeAll(all_people);
            Collections.sort(additional_people, Person.SORT_DISPLAY_NAME);

            return ok(views.html.edit_attendance_week.render(
                start_date.getTime(),
                codes,
                all_people,
                additional_people,
                person_to_days,
                person_to_week));
        }
    }

    public static Result createPersonWeek(int person_id, String monday) {
        CachedPage.remove(CACHE_INDEX);

        Calendar start_date = Utils.parseDateOrNow(monday);
        Calendar end_date = (Calendar)start_date.clone();
        Person p = Person.findById(person_id);

        AttendanceWeek.create(start_date.getTime(), p);

        // look up our newly-created object so that we get the ID
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("week", AttendanceWeek.find.where()
            .eq("person", p)
            .eq("monday", start_date.getTime())
            .findUnique());

        for (int i = 0; i < 5; i++) {
            AttendanceDay.create(end_date.getTime(), p);
            end_date.add(Calendar.DAY_OF_MONTH, 1);
        }

        result.put("days",
            AttendanceDay.find.where()
            .eq("person", p)
            .ge("day", start_date.getTime())
            .le("day", end_date.getTime())
            .order("day ASC")
            .setMaxRows(5)
            .findList());

        return ok(Utils.toJson(result));
    }

    public static Result deletePersonWeek(int person_id, String monday) {
        CachedPage.remove(CACHE_INDEX);

        Calendar start_date = Utils.parseDateOrNow(monday);
        Calendar end_date = (Calendar)start_date.clone();
        end_date.add(Calendar.DAY_OF_MONTH, 5);

        Person p = Person.findById(person_id);

        List<AttendanceDay> days = AttendanceDay.find.where()
            .eq("person", p)
            .ge("day", start_date.getTime())
            .le("day", end_date.getTime())
            .findList();

        for (AttendanceDay day : days) {
            day.delete();
        }

        List<AttendanceWeek> weeks = AttendanceWeek.find.where()
            .eq("person", p)
            .eq("monday", start_date.getTime())
            .findList();

        for (AttendanceWeek week : weeks) {
            week.delete();
        }

        return ok();
    }

    public static Result download() throws IOException {
        response().setHeader("Content-Type", "text/csv; charset=utf-8");
        response().setHeader("Content-Disposition",
            "attachment; filename=Attendance.csv");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Charset charset = Charset.forName("UTF-8");
        CsvWriter writer = new CsvWriter(baos, ',', charset);

        writer.write("Name");
        writer.write("Day");
        writer.write("Absence code");
        writer.write("Arrival time");
        writer.write("Departure time");
        writer.write("Extra hours");
        writer.endRecord();

        List<AttendanceDay> days =
            AttendanceDay.find.where()
                .eq("person.organization", OrgConfig.get().org)
                .ge("day", Application.getStartOfYear())
                .findList();

        for (AttendanceDay day : days) {
            writer.write(day.person.first_name + " " + day.person.last_name);
            writer.write(Application.yymmddDate(day.day));
            if (day.code != null) {
                writer.write(day.code);
                writer.write(""); // empty start_time and end_time
                writer.write("");
            } else {
                writer.write("");
                writer.write(day.start_time != null ? day.start_time.toString() : "");
                writer.write(day.end_time != null ? day.end_time.toString() : "");
            }
            writer.write(""); // no extra hours
            writer.endRecord();
        }

        List<AttendanceWeek> weeks =
            AttendanceWeek.find.where()
                .eq("person.organization", OrgConfig.get().org)
                .ge("monday", Application.getStartOfYear())
                .findList();

        for (AttendanceWeek week : weeks) {
            writer.write(week.person.first_name + " " + week.person.last_name);
            writer.write(Application.yymmddDate(week.monday));
            for (int i = 0; i < 3; i++) {
                writer.write("");
            }
            writer.write("" + week.extra_hours);
            writer.endRecord();
        }

        writer.close();
        // Adding the BOM here causes Excel 2010 on Windows to realize
        // that the file is Unicode-encoded.
        return ok("\ufeff" + new String(baos.toByteArray(), charset));
    }

    public static Result viewWeek(String date) {
        return viewOrEditWeek(date, true);
    }

    public static Result editWeek(String date) {
        return viewOrEditWeek(date, false);
    }

    public static Result saveWeek(Integer week_id, Double extra_hours) {
        CachedPage.remove(CACHE_INDEX);

        AttendanceWeek.find.byId(week_id).edit(extra_hours);
        return ok();
    }

    public static Result saveDay(Integer day_id, String code,
        String start_time, String end_time) throws Exception {
        CachedPage.remove(CACHE_INDEX);

        AttendanceDay.find.byId(day_id).edit(code, start_time, end_time);
        return ok();
    }

    public static Map<String, AttendanceCode> getCodesMap() {
        Map<String, AttendanceCode> codes = new HashMap<String, AttendanceCode>();

        for (AttendanceCode code : AttendanceCode.all(OrgConfig.get().org)) {
            codes.put(code.code, code);
        }

        return codes;
    }

    public static Result viewCodes() {
        return ok(views.html.attendance_codes.render(
            AttendanceCode.all(OrgConfig.get().org),
            code_form));
    }

    public static Result newCode() {
        AttendanceCode ac = AttendanceCode.create(OrgConfig.get().org);
        Form<AttendanceCode> filled_form = code_form.bindFromRequest();
        ac.edit(filled_form);

        return redirect(routes.Attendance.viewCodes());
    }

    public static Result editCode(Integer code_id) {
        AttendanceCode ac = AttendanceCode.findById(code_id);
        Form<AttendanceCode> filled_form = code_form.fill(ac);
        return ok(views.html.edit_attendance_code.render(filled_form));
    }

    public static Result saveCode() {
        Form<AttendanceCode> filled_form = code_form.bindFromRequest();
        AttendanceCode ac = AttendanceCode.findById(
            Integer.parseInt(filled_form.field("id").value()));
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
            if (day.code == null && day.start_time != null && day.end_time != null) {
                result++;
            }
        }

        return result;
    }

    public static double getTotalHours(List<AttendanceDay> days, AttendanceWeek week) {
        double result = 0;
        for (AttendanceDay day : days) {
            if (day.code == null && day.start_time != null && day.end_time != null) {
                result += day.getHours();
            }
        }

        if (week != null) {
            result += week.extra_hours;
        }

        return result;
    }

    public static double getAverageHours(List<AttendanceDay> days, AttendanceWeek week) {
        int daysPresent = getDaysPresent(days);
        if (daysPresent == 0) {
            return 0;
        }

        return getTotalHours(days, week) / (double)daysPresent;
    }

    public static String format(double d) {
        return String.format("%,.1f", d);
    }
}
