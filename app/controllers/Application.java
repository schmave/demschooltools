package controllers;

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

/*
   TODO

* remove tags

* link up tags on person display page
* why aren't names sorted in tag view?


* browse by neighborhood
* use markdown for notes and comments


 */

@Security.Authenticated(Secured.class)
public class Application extends Controller {

    static Form<Person> personForm = Form.form(Person.class);
    static Form<Comment> commentForm = Form.form(Comment.class);

    public static Result index() {
        return redirect(routes.Application.people());
    }

    public static Result people() {
        List<Comment> recent_comments = Comment.find.orderBy("created DESC").setMaxRows(10).findList();
        return ok(views.html.index.render(Person.all(), recent_comments));
    }

    public static Result person(Integer id) {
        Person the_person = Person.find.ref(id);

        List<Person> family_members =
            Person.find.where().isNotNull("family").eq("family", the_person.family).
                ne("person_id", the_person.person_id).findList();

        Set<Integer> family_ids = new HashSet<Integer>();
        family_ids.add(the_person.person_id);
        for (Person family : family_members) {
            family_ids.add(family.person_id);
        }

        List<Comment> all_comments = Comment.find.where().in("person_id", family_ids).
            order("created DESC").findList();

        return ok(views.html.family.render(
            the_person,
            family_members,
            all_comments));
    }

    public static Result jsonPeople(String term) {
        String like_arg = "%" + term + "%";
        List<Person> selected_people =
            Person.find.where().or(
                Expr.ilike("first_name", "%" + term + "%"),
                Expr.ilike("last_name", "%" + term + "%")).findList();

        List<Map<String, String> > result = new ArrayList<Map<String, String> > ();
        for (Person p : selected_people) {
            HashMap<String, String> values = new HashMap<String, String>();
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

    public static Result jsonTags(String term, Integer personId) {
        String like_arg = "%" + term + "%";
        List<Tag> selected_tags =
            Tag.find.where().ilike("title", "%" + term + "%").findList();

        List<Tag> existing_tags = Person.find.byId(personId).tags;

        List<Map<String, String> > result = new ArrayList<Map<String, String> > ();
        for (Tag t : selected_tags) {
            if (existing_tags == null || !existing_tags.contains(t)) {
                HashMap<String, String> values = new HashMap<String, String>();
                values.put("label", t.title);
                values.put("id", "" + t.id);
                result.add(values);
            }
        }

        HashMap<String, String> values = new HashMap<String, String>();
        values.put("label", "Create new tag: " + term);
        values.put("id", "-1");
        result.add(values);

        return ok(Json.stringify(Json.toJson(result)));
    }

    public static Result addTag(Integer tagId, String title, Integer personId) {
        Person p = Person.find.byId(personId);
        if (p == null) {
            return badRequest();
        }

        Tag the_tag;
        if (tagId == null) {
            the_tag = Tag.create(title);
        } else {
            the_tag = Tag.find.ref(tagId);
        }

        PersonTag pt = PersonTag.create(
            the_tag,
            p,
            getCurrentUser());

        p.tags.add(the_tag);
        return ok();
    }

    public static Result viewTag(Integer id) {
        RawSql rawSql = RawSqlBuilder
            .parse("SELECT person.person_id, person.first_name, person.last_name from person natural join person_tag pt "+
                  "join tag on pt.tag_id=tag.id")
            .columnMapping("person.person_id", "person_id")
        .columnMapping("person.first_name", "first_name")
        .columnMapping("person.last_name", "last_name")
            .create();

        List<Person> people =
            Ebean.find(Person.class).setRawSql(rawSql).
            where().eq("tag.id", id).findList();

        return ok(views.html.tag.render(Tag.find.byId(id), people));
    }

    public static User getCurrentUser() {
        return User.findByAuthUserIdentity(
            PlayAuthenticate.getUser(Context.current().session()));
    }

    public static Result newPerson() {
        return ok(views.html.new_person.render(personForm));
    }

    public static Result makeNewPerson() {
        Form<Person> filledForm = personForm.bindFromRequest();
        if(filledForm.hasErrors()) {
            return badRequest(
                views.html.new_person.render(filledForm)
            );
        } else {
            Person new_person = Person.create(filledForm);
            return redirect(routes.Application.person(new_person.person_id));
        }
    }

    public static Result deletePerson(Integer id) {
        Person.delete(id);
        return redirect(routes.Application.people());
    }

    public static Result editPerson(Integer id) {
        return ok(views.html.edit_person.render(Person.find.ref(id).fillForm()));
    }

    public static Result savePersonEdits() {
        return redirect(routes.Application.person(Person.updateFromForm(personForm.bindFromRequest()).person_id));
    }

    public static Result addComment() {
        Form<Comment> filledForm = commentForm.bindFromRequest();
        Comment new_comment = new Comment();

        new_comment.person = Person.find.byId(Integer.parseInt(filledForm.field("person").value()));
        new_comment.user = getCurrentUser();
        new_comment.message = filledForm.field("message").value();

        if (new_comment.message.length() > 0) {
            new_comment.save();
            return ok(views.html.comment.render(Comment.find.byId(new_comment.id)));
        } else {
            return ok();
        }
    }

    public static String calcAge(Person p) {
        return "" + (int)((new Date().getTime() - p.dob.getTime()) / 1000 / 60 / 60 / 24 / 365.25);
    }

    public static Configuration getConfiguration() {
		return Play.application().configuration().getConfig("school_crm");
	}

    public static String formatDate(Date d) {
        d = new Date(d.getTime() +
            (getConfiguration().getInt("time_zone_offset") * 1000L * 60 * 60));
        Date now = new Date();
        String format = "EEE MMMM d, h:mm a";
        if (d.getYear() != now.getYear()) {
            format = "EEE MMMM d, yyyy";
        }
        return new SimpleDateFormat(format).format(d);
    }
}
