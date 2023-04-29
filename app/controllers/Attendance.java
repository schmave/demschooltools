package controllers;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.Time;
import java.text.*;
import java.util.*;
import java.util.stream.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.SqlRow;
import com.csvreader.CsvWriter;

import models.*;

import play.*;
import play.data.*;
import play.libs.Json;
import play.mvc.*;

import static controllers.Application.getConfiguration;


@With(DumpOnError.class)
@Secured.Auth(UserRole.ROLE_ATTENDANCE)
public class Attendance extends Controller {

    static Form<AttendanceCode> code_form;

    static Form<AttendanceCode> getCodeForm() {
        if (code_form == null) {
            code_form = Form.form(AttendanceCode.class);
        }
        return code_form;
    }

    String renderIndexContent(Date start_date, Date end_date, Boolean is_custom_date) {
        if (end_date == null) {
            end_date = new Date(start_date.getTime());
            end_date.setYear(end_date.getYear() + 1);
        }
        Map<Person, AttendanceStats> person_to_stats = mapPeopleToStats(start_date, end_date);
        Map<String, AttendanceCode> codes_map = getCodesMap(false);

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
            Application.attendancePeople(), start_date, end_date, is_custom_date, prev_date, next_date
        ).toString();
    }

    public Result index(String start_date_str, String end_date_str, Boolean is_custom_date) {
        if (start_date_str.equals("")) {
            return ok(views.html.cached_page.render(
                    new CachedPage(CachedPage.ATTENDANCE_INDEX,
                            "Attendance",
                            "attendance",
                            "attendance_home") {
                        @Override
                        String render() {
                            return renderIndexContent(Application.getStartOfYear(), null, false);
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
                            Date start_date = Utils.parseDateOrNow(start_date_str).getTime();
                            Date end_date = null;
                            if (!end_date_str.equals("")) {
                                end_date = Utils.parseDateOrNow(end_date_str).getTime();
                            }
                            return renderIndexContent(start_date, end_date, is_custom_date);
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
            List<Person> additional_people = Application.attendancePeople();
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
                    .add(Expr.or(
                        Expr.ilike("last_name", "%" + term + "%"),
                        Expr.or(
                            Expr.ilike("first_name", "%" + term + "%"),
                            Expr.ilike("display_name", "%" + term + "%"))))
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
                label += " " + p.last_name;
            }
            if (p.display_name != null && !p.display_name.equals("")) {
                label += " (\"" + p.display_name + "\")";
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

        String sql = "select min(day) as min_date, max(day) as max_date from attendance_day where " +
                "person_id=:person_id and " +
                "((code is not null and code != '_NS_') or " +
                "(start_time is not null and end_time is not null)) " +
                "group by person_id";
        SqlRow row = Ebean.createSqlQuery(sql)
                .setParameter("person_id", p.person_id)
                .findUnique();

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

        List<List<AttendanceDay>> school_days = listSchoolDays(start_date, end_date);

        Map<String, AttendanceCode> codes_map = getCodesMap(true);
        AttendanceStats stats = new AttendanceStats();
        boolean has_off_campus_time = false;

        for (int i = 0; i < school_days.size(); i++) {
            List<AttendanceDay> school_day = school_days.get(i);
            AttendanceDay day = school_day.stream().filter(d -> d.person.person_id.equals(person_id)).findAny().orElse(null);
            
            if (day != null) {
                stats.processDay(day, i, codes_map);
                if (day.off_campus_departure_time != null || day.off_campus_return_time != null) {
                    has_off_campus_time = true;
                }
            }
        }

        for (AttendanceWeek week : weeks) {
            stats.total_hours += week.extra_hours;
        }

        Map<Date, AttendanceWeek> day_to_week = new HashMap<Date, AttendanceWeek>();
        for (AttendanceWeek w : weeks) {
            day_to_week.put(w.monday, w);
        }

        List<String> codes = new ArrayList<String>(codes_map.keySet()).stream()
            .filter(c -> c != "_NS_")
            .collect(Collectors.toList());

        int table_width = has_off_campus_time ? 900 : 700;

        return ok(views.html.attendance_person.render(
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
            table_width
        ));
    }

    public Result importFromCustodia() {
        Map<String,String[]> data = request().body().asFormUrlEncoded();
        Calendar start_date = Utils.parseDateOrNow(data.get("monday")[0]);
        Calendar end_date = (Calendar) start_date.clone();
        end_date.add(Calendar.DAY_OF_MONTH, 4);

        List<AttendanceDay> days =
                AttendanceDay.find.where()
                        .eq("person.organization", OrgConfig.get().org)
                        .ge("day", start_date.getTime())
                        .le("day", end_date.getTime())
                        .findList();

        Set<Person> people = new HashSet<>();
        for (AttendanceDay day : days) {
            people.add(day.person);
        }

        List<Integer> person_ids = new ArrayList<>();
        for (Person person : people) {
            person_ids.add(person.person_id);
        }

        if (!person_ids.isEmpty()) {
            String sql = "select dst_id, swipe_day, " +
                    "min(in_time) at time zone :time_zone as in_time, " +
                    "max(out_time) at time zone :time_zone as out_time " +
                    "from overseer.swipes sw join overseer.students stu on sw.student_id=stu._id " +
                    "where in_time is not null and out_time is not null " +
                    "and dst_id in (:person_ids) " +
                    "and swipe_day >= :first_date and swipe_day <= :last_date " +
                    "group by dst_id, swipe_day";

            List<SqlRow> custodiaRows = Ebean.createSqlQuery(sql)
                    .setParameter("time_zone", OrgConfig.get().time_zone.getID())
                    .setParameter("person_ids", person_ids)
                    .setParameter("first_date", start_date.getTime())
                    .setParameter("last_date", end_date.getTime())
                    .findList();

            Map<Integer, Map<Date, SqlRow>> person_day_custodia = new HashMap<>();
            for (SqlRow row : custodiaRows) {
                int person_id = row.getInteger("dst_id");
                Date day = row.getDate("swipe_day");
                if (!person_day_custodia.containsKey(person_id)) {
                    person_day_custodia.put(person_id, new HashMap<>());
                }
                person_day_custodia.get(person_id).put(day, row);
            }

            for (AttendanceDay day : days) {
                if (day.code == null
                        && person_day_custodia.containsKey(day.person.person_id)
                        && person_day_custodia.get(day.person.person_id).containsKey(day.day)) {
                    SqlRow row = person_day_custodia.get(day.person.person_id).get(day.day);
                    boolean updated = false;
                    if (day.start_time == null) {
                        day.start_time = new Time(row.getDate("in_time").getTime());
                        updated = true;
                    }
                    if (day.end_time == null) {
                        day.end_time = new Time(row.getDate("out_time").getTime());
                        updated = true;
                    }
                    if (updated) {
                        day.save();
                    }
                }
            }

            CachedPage.remove(CachedPage.ATTENDANCE_INDEX);
        }

        return redirect(routes.Attendance.editWeek(Application.forDateInput(start_date.getTime())));
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

        response().setHeader("Content-Type", "application/zip");
        response().setHeader("Content-Disposition",
                "attachment; filename=attendance.zip");

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
            allDates.add(day.day);

            String name = day.person.first_name + " " + day.person.last_name;
            if (!personDateAttendance.containsKey(name)) {
                personDateAttendance.put(name, new HashMap<>());
            }
            personDateAttendance.get(name).put(day.day, day);
        }

        zos.putNextEntry(new ZipEntry("attendance/daily_hours.csv"));
        // Adding the BOM here causes Excel 2010 on Windows to realize
        // that the file is Unicode-encoded.
        zos.write("\ufeff".getBytes(charset));
        zos.write(getDailyHoursFile(allDates, personDateAttendance, charset));
        zos.closeEntry();

        zos.putNextEntry(new ZipEntry("attendance/daily_signins.csv"));
        // Adding the BOM here causes Excel 2010 on Windows to realize
        // that the file is Unicode-encoded.
        zos.write("\ufeff".getBytes(charset));
        zos.write(getDailySigninsFile(allDates, personDateAttendance, charset));
        zos.closeEntry();

        zos.close();
        return ok(zipBytes.toByteArray());
    }

    private byte[] getDailyHoursFile(TreeSet<Date> allDates,
                                     TreeMap<String, HashMap<Date, AttendanceDay>> personDateAttendance,
                                     Charset charset) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CsvWriter writer = new CsvWriter(baos, ',', charset);
        writer.write("Name");
        for (Date d : allDates) {
            writer.write(Application.yymmddDate(d));
        }
        writer.endRecord();

        for (String name : personDateAttendance.keySet()) {
            writer.write(name);
            for (Date date : allDates) {
                AttendanceDay day = personDateAttendance.get(name).get(date);
                if (day == null || day.start_time == null || day.end_time == null) {
                    writer.write(day == null || day.code == null ? "" : day.code);
                } else {
                    writer.write(String.format("%.2f", day.getHours()));
                }
            }
            writer.endRecord();
        }

        writer.close();
        return baos.toByteArray();
    }

    private byte[] getDailySigninsFile(TreeSet<Date> allDates,
            TreeMap<String, HashMap<Date, AttendanceDay>> personDateAttendance,
            Charset charset) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CsvWriter writer = new CsvWriter(baos, ',', charset);
        writer.write("Name");
        for (Date d : allDates) {
            writer.write(Application.yymmddDate(d));
        }
        writer.endRecord();

        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        for (String name : personDateAttendance.keySet()) {
            for (int x = 0; x < 2; x++) {
                writer.write(x == 0 ? name : "");
                for (Date date : allDates) {
                    AttendanceDay day = personDateAttendance.get(name).get(date);
                    if (day == null || day.start_time == null || day.end_time == null) {
                        writer.write(day == null || day.code == null ? "" : day.code);
                    } else {
                        writer.write(dateFormat.format(x == 0 ? day.start_time : day.end_time));
                    }
                }
                writer.endRecord();
            }
        }

        writer.close();
        return baos.toByteArray();
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
            no_school.code = "_NS_";
            codes.put("_NS_", no_school);
        }

        return codes;
    }

    public static Map<Person, AttendanceStats> mapPeopleToStats(Date start_date, Date end_date) {
        Map<Person, AttendanceStats> person_to_stats = new HashMap<>();
        Map<String, AttendanceCode> codes_map = getCodesMap(false);

        List<List<AttendanceDay>> school_days = listSchoolDays(start_date, end_date);

        for (int i = 0; i < school_days.size(); i++) {
            List<AttendanceDay> school_day = school_days.get(i);
            for (AttendanceDay day : school_day) {
                if (!person_to_stats.containsKey(day.person)) {
                    person_to_stats.put(day.person, new AttendanceStats());
                }
                AttendanceStats stats = person_to_stats.get(day.person);
                stats.processDay(day, i, codes_map);
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

        return person_to_stats;
    }

    public static List<List<AttendanceDay>> listSchoolDays(Date start_date, Date end_date) {
        List<List<AttendanceDay>> school_days = new ArrayList<>();

        List<AttendanceDay> days = AttendanceDay.find.where()
            .eq("person.organization", OrgConfig.get().org)
            .ge("day", start_date)
            .le("day", end_date)
            .findList();

        // group AttendanceDays by date, sorted into reverse chronological order
        SortedMap<Date, List<AttendanceDay>> groups = new TreeMap<Date, List<AttendanceDay>>(Collections.reverseOrder());
        for (AttendanceDay day : days) {
            if (!groups.containsKey(day.day)) {
                groups.put(day.day, new ArrayList<AttendanceDay>());
            }
            List<AttendanceDay> group = groups.get(day.day);
            group.add(day);
        }

        for (List<AttendanceDay> group : groups.values()) {
            // Iterate through all AttendanceDays in the group until we find one that has some attendance
            // data that's not _NS_. If we don't find any, the date is not a school day.
            for (AttendanceDay day : group) {
                boolean is_absence = day.code != null && !day.code.equals("_NS_");
                boolean is_attendance = day.start_time != null && day.end_time != null;
                if (is_absence || is_attendance) {
                    school_days.add(group);
                    break;
                }
            }
        }
        return school_days;
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

    public Result rules() {
        Organization org = OrgConfig.get().org;
        Map<String, AttendanceCode> codes_map = getCodesMap(false);
        List<AttendanceRule> current_rules = AttendanceRule.all();
        List<AttendanceRule> past_rules = AttendanceRule.all();

        return ok(views.html.attendance_rules.render(
            current_rules,
            past_rules,
            codes_map,
            org.attendance_enable_partial_days,
            org.attendance_show_reports
        ));
    }

    public Result newRule() {
        return rule(null);
    }

    public Result rule(Integer id) {
        Organization org = OrgConfig.get().org;
        String people_json = Application.attendancePeopleJson();
        AttendanceRule rule = id == null ? new AttendanceRule() : AttendanceRule.findById(id);
        return ok(views.html.attendance_edit_rule.render(
            rule,
            people_json,
            org.attendance_enable_partial_days,
            org.attendance_show_reports
        ));
    }

    public Result saveRule() throws Exception {
        Form<AttendanceRule> form = Form.form(AttendanceRule.class);
        Form<AttendanceRule> filledForm = form.bindFromRequest();
        AttendanceRule.save(filledForm);
        return redirect(routes.Attendance.rules());
    }

    public Result deleteRule(Integer id) {
        return redirect(routes.Attendance.rules());
    }

    public Result offCampusTime() {
        List<AttendanceDay> events = AttendanceDay.find.where()
            .eq("person.organization", OrgConfig.get().org)
            .gt("day", Application.getStartOfYear())
            .ne("off_campus_departure_time", null)
            .order("day ASC")
            .findList();

        return ok(views.html.attendance_off_campus.render(events));
    }

    public Result deleteOffCampusTime() {
        DynamicForm form = Form.form().bindFromRequest();
        Map<String, String> data = form.data();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (entry.getKey().isEmpty()) continue;
            Integer attendance_day_id = Integer.parseInt(entry.getKey());
            AttendanceDay attendance_day = AttendanceDay.findById(attendance_day_id);
            attendance_day.off_campus_departure_time = null;
            attendance_day.off_campus_return_time = null;
            attendance_day.update();
        }
        return redirect(routes.Attendance.offCampusTime());
    }

    public Result addOffCampusTime() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Date today = Calendar.getInstance().getTime();
        String current_date = df.format(today);
        String people_json = Application.attendancePeopleJson();
        return ok(views.html.attendance_add_off_campus.render(current_date, people_json));
    }

    public Result saveOffCampusTime() throws ParseException {
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
        DynamicForm form = Form.form().bindFromRequest();
        Map<String, String> data = form.data();

        for (int i = 0; i < 10; i++) {
            try {
                Integer person_id = Integer.parseInt(data.get("personid-" + i));
                Date day = df.parse(data.get("day-" + i));
                Time departure_time = AttendanceDay.parseTime(data.get("departuretime-" + i));
                Time return_time = AttendanceDay.parseTime(data.get("returntime-" + i));
                Integer minutes_exempted = AttendanceDay.parseInt(data.get("minutesexempted-" + i));

                if (person_id != null && day != null && departure_time != null && return_time != null) {
                    AttendanceDay attendance_day = AttendanceDay.findCurrentDay(day, person_id);
                    attendance_day.off_campus_departure_time = departure_time;
                    attendance_day.off_campus_return_time = return_time;
                    attendance_day.off_campus_minutes_exempted = minutes_exempted;
                    attendance_day.update();
                }
            } catch (NumberFormatException e) {
            }
        }
        return redirect(routes.Attendance.offCampusTime());
    }

    public Result reports() {
        return ok(views.html.attendance_reports.render(new AttendanceReport()));
    }

    public Result runReport() {
        Form<AttendanceReport> form = Form.form(AttendanceReport.class);
        Form<AttendanceReport> filledForm = form.bindFromRequest();
        if (filledForm.hasErrors()) {
            System.out.println("ERRORS: " + filledForm.errorsAsJson().toString());
            return badRequest(views.html.attendance_reports.render(new AttendanceReport()));
        }
        AttendanceReport report = AttendanceReport.createFromForm(filledForm);
        return ok(views.html.attendance_reports.render(report));
    }

    public Result assignPINs() {
        List<Person> people = Application.attendancePeople();
        Collections.sort(people, Person.SORT_DISPLAY_NAME);
        // add admin PIN
        Person admin = new Person();
        admin.person_id = -1;
        admin.first_name = "Admin";
        admin.pin = OrgConfig.get().org.attendance_admin_pin;
        people.add(0, admin);
        return ok(views.html.attendance_pins.render(people));
    }

    public Result savePINs() {
        List<Person> people = Application.attendancePeople();
        HashMap<Integer, Person> people_by_id = new HashMap<Integer, Person>();
        for (Person p : people) {
            people_by_id.put(p.person_id, p);
        }
        Set<Map.Entry<String,String[]>> entries = request().queryString().entrySet();
        for (Map.Entry<String, String[]> entry : entries) {
            Integer person_id = Integer.parseInt(entry.getKey());
            if (person_id == -1) {
                OrgConfig.get().org.setAttendanceAdminPIN(entry.getValue()[0]);
            }
            else {
                Person person = people_by_id.get(person_id);
                if (person != null) {
                    person.pin = entry.getValue()[0];
                    person.save();
                }
            }
        }
        return redirect(routes.Attendance.assignPINs());
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

    public static String formatAsPercent(double d) {
        return String.format("%,.1f", d * 100) + "%";
    }
}
