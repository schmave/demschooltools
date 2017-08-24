package controllers;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.avaje.ebean.Expr;
import com.csvreader.CsvWriter;

import models.*;

import play.*;
import play.data.*;
import play.libs.Json;
import play.mvc.*;

import static controllers.Application.getConfiguration;

@With(DumpOnError.class)
@Secured.Auth(UserRole.ROLE_ALL_ACCESS)
public class Attendance extends Controller {

    static Form<AttendanceCode> code_form;

    static Form<AttendanceCode> getCodeForm() {
        if (code_form == null) {
            code_form = Form.form(AttendanceCode.class);
        }
        return code_form;
    }

    String renderIndexContent(Date start_date) {
        Date end_date = new Date(start_date.getTime());
        end_date.setYear(end_date.getYear() + 1);
        Map<Person, AttendanceStats> person_to_stats = new HashMap<>();
        Map<String, AttendanceCode> codes_map = getCodesMap(false);

        List<AttendanceDay> days =
                AttendanceDay.find.where()
                        .eq("person.organization", OrgConfig.get().org)
                        .ge("day", start_date)
                        .le("day", end_date)
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
                        .ge("monday", start_date)
                        .le("monday", end_date)
                        .findList();

        for (AttendanceWeek week : weeks) {
            if (!person_to_stats.containsKey(week.person)) {
                person_to_stats.put(week.person, new AttendanceStats());
            }

            AttendanceStats stats = person_to_stats.get(week.person);
            stats.total_hours += week.extra_hours;
        }

        List<Person> all_people = new ArrayList<>(person_to_stats.keySet());
        Collections.sort(all_people, Person.SORT_FIRST_NAME);

        List<String> all_codes = new ArrayList<>(codes_map.keySet());

        Date prev_date = new Date(start_date.getTime());
        prev_date.setYear(prev_date.getYear() - 1);
        Date next_date = new Date(start_date.getTime());
        next_date.setYear(next_date.getYear() + 1);
        if (start_date.getYear() == Application.getStartOfYear().getYear()) {
            next_date = null;
        }

        return views.html.attendance_index.render(
                all_people, person_to_stats, all_codes, codes_map,
                Application.allPeople(), start_date, prev_date, next_date).toString();
    }

    public Result index(String start_date) {
        if (start_date.equals("")) {
            return ok(views.html.cached_page.render(
                    new CachedPage(CachedPage.ATTENDANCE_INDEX,
                            "Attendance",
                            "attendance",
                            "attendance_home") {
                        @Override
                        String render() {
                            return renderIndexContent(Application.getStartOfYear());
                        }
                    }));
        } else {
            return ok(views.html.cached_page.render(
                    new CachedPage("",
                            "Attendance",
                            "attendance",
                            "attendance_home") {
                        @Override
                        public String getPage() {
                            return renderIndexContent(Utils.parseDateOrNow(start_date).getTime());
                        }

                        @Override
                        String render() {
                            throw new RuntimeException("This shouldn't be called");
                        }
                    }));
        }
    }

    public Result viewOrEditWeek(String date, boolean do_view) {
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
        Collections.sort(all_people, Person.SORT_FIRST_NAME);

        Map<String, AttendanceCode> codes = getCodesMap(do_view);

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

            response().setHeader("Cache-Control", "max-age=0, no-cache, no-store");
            response().setHeader("Pragma", "no-cache");
            return ok(views.html.edit_attendance_week.render(
                start_date.getTime(),
                codes,
                all_people,
                additional_people,
                person_to_days,
                person_to_week));
        }
    }

    public Result jsonPeople(String term) {
        List<Person> name_matches =
                Person.find.where()
                    .add(Expr.or(Expr.ilike("last_name", "%" + term + "%"),
                                 Expr.ilike("first_name", "%" + term + "%")))
                    .eq("organization", Organization.getByHost())
                    .eq("is_family", false)
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
            String label = p.first_name;
            if (p.last_name != null) {
                label = label + " " + p.last_name;
            }
            values.put("label", label);
            values.put("id", "" + p.person_id);
            result.add(values);
        }

        return ok(Json.stringify(Json.toJson(result)));
    }

    public Result viewPersonReport(Integer person_id, String start_date_str, String end_date_str) {
        Person p = Person.findById(person_id);

        Date start_date = Application.getStartOfYear();
        Date end_date = new Date();

        if (!start_date_str.trim().isEmpty() || !end_date_str.trim().isEmpty()) {
            try {
                start_date = new SimpleDateFormat("yyyy-M-d").parse(start_date_str);
                end_date = new SimpleDateFormat("yyyy-M-d").parse(end_date_str);
            } catch (ParseException e) {
            }
        } else {
            AttendanceDay last_day =
                    AttendanceDay.find.where()
                            .eq("person", p)
                            .order("day DESC")
                            .setMaxRows(1)
                            .findUnique();
            if (last_day != null) {
                end_date = last_day.day;
                start_date = Application.getStartOfYear(end_date);
            }
        }

        List<AttendanceDay> days =
            AttendanceDay.find.where()
                .eq("person", p)
                .ge("day", start_date)
                .le("day", end_date)
                .order("day ASC")
                .findList();

        List<AttendanceWeek> weeks =
            AttendanceWeek.find.where()
                .eq("person", p)
                .ge("monday", start_date)
                .le("monday", end_date)
                .findList();

        Map<String, AttendanceCode> codes = getCodesMap(true);

        Map<Date, AttendanceWeek> day_to_week = new HashMap<Date, AttendanceWeek>();
        for (AttendanceWeek w : weeks) {
            day_to_week.put(w.monday, w);
        }

        return ok(views.html.attendance_person.render(
            p,
            days,
            day_to_week,
            new ArrayList<String>(codes.keySet()),
            codes,
            start_date,
            end_date
        ));
    }

    public Result createPersonWeek() {
        CachedPage.remove(CachedPage.ATTENDANCE_INDEX);

        Map<String,String[]> data = request().body().asFormUrlEncoded();
        Calendar start_date = Utils.parseDateOrNow(data.get("monday")[0]);

        ArrayList<Object> result = new ArrayList<Object>();
        String[] person_ids = data.get("person_id[]");

        if (person_ids == null) {
            return badRequest("No person_id[] found");
        }

        for (String person_id : person_ids) {
            Calendar end_date = (Calendar)start_date.clone();
            boolean alreadyExists = false;
            Person p = Person.findById(Integer.parseInt(person_id));
            try {
                AttendanceWeek.create(start_date.getTime(), p);
            } catch (javax.persistence.PersistenceException pe) {
                Utils.eatIfUniqueViolation(pe);
                alreadyExists = true;
            }
            Map<String, Object> one_result = new HashMap<String, Object>();

            // look up our newly-created object so that we get the ID
            one_result.put("week", AttendanceWeek.find.where()
                .eq("person", p)
                .eq("monday", start_date.getTime())
                .findUnique());

            for (int i = 0; i < 5; i++) {
                try {
                    AttendanceDay.create(end_date.getTime(), p);
                } catch (javax.persistence.PersistenceException pe) {
                    Utils.eatIfUniqueViolation(pe);
                    alreadyExists = true;
                }
                end_date.add(Calendar.DAY_OF_MONTH, 1);
            }

            one_result.put("days",
                AttendanceDay.find.where()
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

    public Result deletePersonWeek(int person_id, String monday) {
        CachedPage.remove(CachedPage.ATTENDANCE_INDEX);

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

    public Result download(String start_date_str) throws IOException {
        Date start_date = Application.getStartOfYear();
        if (!start_date_str.equals("")) {
            start_date = Utils.parseDateOrNow(start_date_str).getTime();
        }
        Date end_date = new Date(start_date.getTime());
        end_date.setYear(end_date.getYear() + 1);

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
                .ge("day", start_date)
                .le("day", end_date)
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
                .ge("monday", start_date)
                .le("monday", end_date)
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

    public Result viewWeek(String date) {
        return viewOrEditWeek(date, true);
    }

    public Result editWeek(String date) {
        return viewOrEditWeek(date, false);
    }

    public Result saveWeek(Integer week_id, Double extra_hours) {
        CachedPage.remove(CachedPage.ATTENDANCE_INDEX);

        AttendanceWeek.find.byId(week_id).edit(extra_hours);
        return ok();
    }

    public Result saveDay(Integer day_id, String code,
        String start_time, String end_time) throws Exception {
        CachedPage.remove(CachedPage.ATTENDANCE_INDEX);

        AttendanceDay.find.byId(day_id).edit(code, start_time, end_time);
        return ok();
    }

    public static Map<String, AttendanceCode> getCodesMap(boolean include_no_school) {
        Map<String, AttendanceCode> codes = new HashMap<String, AttendanceCode>();

        for (AttendanceCode code : AttendanceCode.all(OrgConfig.get().org)) {
            codes.put(code.code, code);
        }

        if (include_no_school) {
            AttendanceCode no_school = new AttendanceCode();
            no_school.description = "No school";
            no_school.color = "#cc9";
            codes.put("_NS_", no_school);
        }

        return codes;
    }

    public Result viewCustodiaAdmin() {
        Map<String, Object> scopes = new HashMap<>();
        Configuration conf = getConfiguration();
        scopes.put("custodiaUrl", conf.getString("custodia_url"));
        scopes.put("custodiaUsername", OrgConfig.get().org.short_name + "-admin");
        scopes.put("custodiaPassword", conf.getString("custodia_password"));
        Result result = ok(views.html.main_with_mustache.render(
                "Sign in system",
                "custodia",
                "",
                "custodia_admin.html",
                scopes));
        return result;
    }

    public Result viewCodes() {
        return ok(views.html.attendance_codes.render(
            AttendanceCode.all(OrgConfig.get().org),
            getCodeForm()));
    }

    public Result newCode() {
        AttendanceCode ac = AttendanceCode.create(OrgConfig.get().org);
        Form<AttendanceCode> filled_form = getCodeForm().bindFromRequest();
        ac.edit(filled_form);

        CachedPage.remove(CachedPage.ATTENDANCE_INDEX);

        return redirect(routes.Attendance.viewCodes());
    }

    public Result editCode(Integer code_id) {
        AttendanceCode ac = AttendanceCode.findById(code_id);
        Form<AttendanceCode> filled_form = getCodeForm().fill(ac);
        return ok(views.html.edit_attendance_code.render(filled_form));
    }

    public Result saveCode() {
        Form<AttendanceCode> filled_form = getCodeForm().bindFromRequest();
        AttendanceCode ac = AttendanceCode.findById(
            Integer.parseInt(filled_form.field("id").value()));
        ac.edit(filled_form);

        CachedPage.remove(CachedPage.ATTENDANCE_INDEX);

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
