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

        List<Charge> sm_charges = Charge.find.where().eq("referred_to_sm", true).eq("sm_decision", null).findList();

        return ok(views.html.index.render(meetings, sm_charges));
    }

    public static Result viewMeeting(int meeting_id) {
        return ok(views.html.view_meeting.render(Meeting.find.byId(meeting_id)));
    }

    public static Result getPersonHistory(Integer id) {
        Person p = Person.find.byId(id);
        return ok(views.html.person_history.render(p, new PersonHistory(p)));
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

    public static String jsonPeople(String term) {
        Tag cur_student_tag = Tag.find.where().eq("title", "Current Student").findUnique();
        Tag staff_tag = Tag.find.where().eq("title", "Staff").findUnique();

        List<Person> people = getPeopleForTag(cur_student_tag.id);
        people.addAll(getPeopleForTag(staff_tag.id));
        Collections.sort(people, Person.SORT_DISPLAY_NAME);

		term = term.toLowerCase();

        List<Map<String, String> > result = new ArrayList<Map<String, String> > ();
        for (Person p : people) {
            if (p.searchStringMatches(term)) {
                HashMap<String, String> values = new HashMap<String, String>();
                values.put("label", p.getDisplayName());
                values.put("id", "" + p.person_id);
                result.add(values);
            }
        }

        return Json.stringify(Json.toJson(result));
    }

    public static String jsonRules(String term) {
		term = term.toLowerCase();

        List<Rule> rules = Rule.find.orderBy("title ASC").findList();

        List<Map<String, String> > result = new ArrayList<Map<String, String> > ();
        for (Rule r : rules) {
            if (r.title.toLowerCase().contains(term)) {
                HashMap<String, String> values = new HashMap<String, String>();
                values.put("label", r.title);
                values.put("id", "" + r.id);
                result.add(values);
            }
        }

        return Json.stringify(Json.toJson(result));
    }

    public static String formatDateShort(Date d) {
        return new SimpleDateFormat("MM/dd").format(d);
    }

    public static String formatMeetingDate(Date d) {
        return new SimpleDateFormat("EE--MMMM dd, yyyy").format(d);
    }

    public static String currentUsername() {
        return Context.current().request().username();
    }

    public static boolean isUserEditor(String username) {
        return username != null && username.contains("@");
    }

    public static boolean isCurrentUserEditor() {
        return isUserEditor(currentUsername());
    }

    public static Configuration getConfiguration() {
        return Play.application().configuration().getConfig("jcdb");
    }

    public static String getRemoteIp() {
        Context ctx = Context.current();
        Configuration conf = getConfiguration();

        if (conf.getBoolean("heroku_ips")) {
            String header = ctx.request().getHeader("HTTP_X_FORWARDED_FOR");
            String splits[] = header.split("[, ]");
            return splits[splits.length - 1];
        } else {
            return ctx.request().remoteAddress();
        }
    }
}
