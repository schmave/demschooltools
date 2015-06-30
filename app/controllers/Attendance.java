package controllers;

import java.io.*;
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

@Security.Authenticated(EditorSecured.class)
@With(DumpOnError.class)
public class Attendance extends Controller {

    static Form<AttendanceCode> code_form = Form.form(AttendanceCode.class);

    public static Result index() {
        return ok(views.html.attendance_index.render());
    }

    public static Result viewWeek(String date) {
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
        }

        List<AttendanceWeek> weeks =
            AttendanceWeek.find.where()
                .eq("person.organization", OrgConfig.get().org)
                .eq("monday", start_date.getTime())
                .findList();

        Map<Person, List<AttendanceWeek>> person_to_weeks =
            new HashMap<Person, List<AttendanceWeek>>();

        for (AttendanceWeek week : weeks) {
            List<AttendanceWeek> list = person_to_weeks.containsKey(week.person)
                ? person_to_weeks.get(week.person)
                : new ArrayList<AttendanceWeek>();

            list.add(week);
        }

        return ok(views.html.attendance_week.render(
            AttendanceCode.all(OrgConfig.get().org),
            person_to_days,
            person_to_weeks));
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
}
