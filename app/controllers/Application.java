package controllers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;
import com.avaje.ebean.SqlUpdate;
import com.feth.play.module.pa.PlayAuthenticate;

import models.*;

import play.*;
import play.data.*;
import play.libs.Json;
import play.mvc.*;
import play.mvc.Http.Context;

@Security.Authenticated(Secured.class)
public class Application extends Controller {

    public static Result index() {
        List<Meeting> meetings = Meeting.find.orderBy("date DESC").findList();
        return ok(views.html.index.render(meetings));
    }

    public static Result editTodaysMinutes() {
        Meeting the_meeting = Meeting.find.where().eq("date", new Date()).findUnique();
        if (the_meeting == null) {
            the_meeting = Meeting.create(new Date());
            the_meeting.save();
        }
        return editMinutes(the_meeting);
    }

    public static Result editMinutes(Meeting meeting) {
        return ok(views.html.edit_minutes.render(meeting));
    }

    public static Result viewMeeting(int meeting_id) {
        return ok(views.html.view_meeting.render(Meeting.find.byId(meeting_id)));
    }

    public static Result editMeeting(int meeting_id) {
        Meeting the_meeting = Meeting.find.byId(meeting_id);
        return editMinutes(the_meeting);
    }

    public static Result createCase(String id, Integer meeting_id) {
        Case.create(id, Meeting.find.ref(meeting_id));
        return ok();
    }

    public static Result getPersonHistory(Integer id) {
        Person p = Person.find.byId(id);
        return ok(views.html.person_history.render(p));
    }

    public static Result saveCase(String id) {
        Case c = Case.find.byId(id);

        c.edit(request().queryString());
        c.save();

        return ok();
    }

    public static Result addPersonAtMeeting(Integer meeting_id, Integer person_id,
        Integer role) {
        Meeting m = Meeting.find.ref(meeting_id);

        PersonAtMeeting.create(m, Person.find.ref(person_id), role);

        return ok();
    }

    public static Result removePersonAtMeeting(Integer meeting_id, Integer person_id,
        Integer role) {
        SqlUpdate update = Ebean.createSqlUpdate(
            "DELETE from person_at_meeting where meeting_id = :meeting_id"+
            " and person_id = :person_id and role = :role");
        update.setParameter("meeting_id", meeting_id);
        update.setParameter("person_id", person_id);
        update.setParameter("role", role);

        Ebean.execute(update);

        return ok();
    }

    public static Result addTestifier(String case_number, Integer person_id)
    {
        TestifyRecord.create(Case.find.ref(case_number), Person.find.ref(person_id));
        return ok();
    }

    public static Result removeTestifier(String case_number, Integer person_id)
    {
        SqlUpdate update = Ebean.createSqlUpdate(
            "DELETE from testify_record where case_number = :case_number "+
            "and person_id = :person_id");
        update.setParameter("case_number", case_number);
        update.setParameter("person_id", person_id);

        Ebean.execute(update);
        return ok();
    }

    public static Result addCharge(String case_number)
    {
        Charge c = Charge.create(Case.find.ref(case_number));
        return ok("" + c.id);
    }

    public static Result saveCharge(int id) {
        Charge c = Charge.find.byId(id);

        c.edit(request().queryString());
        c.save();

        return ok();
    }

    static List<Person> getPeopleForTag(Integer id)
    {
        RawSql rawSql = RawSqlBuilder
            .parse("SELECT person.person_id, person.first_name, person.last_name, person.display_name from "+
                   "person join person_tag pt on person.person_id=pt.person_id "+
                  "join tag on pt.tag_id=tag.id")
            .columnMapping("person.person_id", "person_id")
        .columnMapping("person.first_name", "first_name")
        .columnMapping("person.last_name", "last_name")
		.columnMapping("person.display_name", "display_name")
            .create();

        return Ebean.find(Person.class).setRawSql(rawSql).
            where().eq("tag.id", id).orderBy("person.last_name, person.first_name").findList();
    }

    public static Result jsonPeople(String term) {
        Tag cur_student_tag = Tag.find.where().eq("title", "Current Student").findUnique();
        Tag staff_tag = Tag.find.where().eq("title", "Staff").findUnique();

        List<Person> people = getPeopleForTag(cur_student_tag.id);
        people.addAll(getPeopleForTag(staff_tag.id));
		
		term = term.toLowerCase();

        List<Map<String, String> > result = new ArrayList<Map<String, String> > ();
        for (Person p : people) {
            if (p.first_name.toLowerCase().contains(term) ||
                p.last_name.toLowerCase().contains(term) ||
				p.display_name.toLowerCase().contains(term)) {
                HashMap<String, String> values = new HashMap<String, String>();
                values.put("label", p.getDisplayName());
                values.put("id", "" + p.person_id);
                result.add(values);
            }
        }

        return ok(Json.stringify(Json.toJson(result)));
    }

    public static Result jsonRules(String term) {
		term = term.toLowerCase();

        List<Rule> rules = Rule.find.findList();

        List<Map<String, String> > result = new ArrayList<Map<String, String> > ();
        for (Rule r : rules) {
            if (r.title.toLowerCase().contains(term)) {
                HashMap<String, String> values = new HashMap<String, String>();
                values.put("label", r.title);
                values.put("id", "" + r.id);
                result.add(values);
            }
        }

        return ok(Json.stringify(Json.toJson(result)));
    }

    public static String formatMeetingDate(Date d) {
        return new SimpleDateFormat("EEEE MMMM dd, yyyy").format(d);
    }
}
