package controllers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;
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
        return ok(views.html.index.render());
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

    public static Result saveMinutes() {
        return ok();
    }

    static List<Person> getPeopleForTag(Integer id)
    {
        RawSql rawSql = RawSqlBuilder
            .parse("SELECT person.person_id, person.first_name, person.last_name from "+
                   "person join person_tag pt on person.person_id=pt.person_id "+
                  "join tag on pt.tag_id=tag.id")
            .columnMapping("person.person_id", "person_id")
        .columnMapping("person.first_name", "first_name")
        .columnMapping("person.last_name", "last_name")
            .create();

        return Ebean.find(Person.class).setRawSql(rawSql).
            where().eq("tag.id", id).orderBy("person.last_name, person.first_name").findList();
    }

    public static Result jsonPeople() {
        Tag cur_student_tag = Tag.find.where().eq("title", "Current Student").findUnique();
        Tag staff_tag = Tag.find.where().eq("title", "Staff").findUnique();

        List<Person> people = getPeopleForTag(cur_student_tag.id);
        people.addAll(getPeopleForTag(staff_tag.id));

        List<Map<String, String> > result = new ArrayList<Map<String, String> > ();
        for (Person p : people) {
            HashMap<String, String> values = new HashMap<String, String>();
            String name = p.first_name;
            if (p.last_name != null) {
                name += " " + p.last_name;
            }
            values.put("name", name);
            values.put("id", "" + p.person_id);
            result.add(values);
        }

        return ok(Json.stringify(Json.toJson(result)));
    }
}
