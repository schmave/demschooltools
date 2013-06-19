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

/*
   TODO

** search all fields for people search



* use biological symbols for M/F/T instead of colors

* show recent comments for people in a tag on tag page
* hide data export sections on tag page by default

* add ability to link to people in comments

* bug --can't show comment form in firefox; return false in javascript?

* add tool to merge people

* confirmation when creating a person with the same name as someone already in
  the database

* strip/trim spaces from person's first and last name on edit/create (and other
  fields too)

* prevent double submit of comments
* disable comment button while request is pending

* upload guidance counselors and college professors

* add another way to link to people other than making them one family.
* add an "organization" field to a person

* show birthdays for the current day

* browse people by neighborhood

* share code between comment_fragment and people index.
* sort people list by date created, then name
* use markdown for notes and comments


* Be able to send email to tagged people: using a web rich text editor, or
  perhaps forwarding emails when they are sent to people+tag_parent@trvs.org
  from an approved sender? see mandrill

*/

@Security.Authenticated(Secured.class)
public class Application extends Controller {

    static Form<Person> personForm = Form.form(Person.class);
    static Form<Comment> commentForm = Form.form(Comment.class);
    static Form<Donation> donationForm = Form.form(Donation.class);

    public static Result index() {
        return redirect(routes.Application.people());
    }

    public static Result people() {
        List<Comment> recent_comments = Comment.find.orderBy("created DESC").setMaxRows(20).findList();
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

        List<Donation> all_donations = Donation.find.where().in("person_id", family_ids).
            order("date DESC").findList();

        return ok(views.html.family.render(
            the_person,
            family_members,
            all_comments,
            all_donations));
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
        return ok(views.html.tag_fragment.render(the_tag, p));
    }

    public static Result removeTag(Integer person_id, Integer tag_id) {
        Ebean.createSqlUpdate("DELETE from person_tag where person_id=" + person_id +
            " AND tag_id=" + tag_id).execute();

        return ok();
    }

    public static Result viewTag(Integer id) {
        Tag the_tag = Tag.find.byId(id);

        RawSql rawSql = RawSqlBuilder
            .parse("SELECT person.person_id, person.first_name, person.last_name from "+
                   "person join person_tag pt on person.person_id=pt.person_id "+
                  "join tag on pt.tag_id=tag.id")
            .columnMapping("person.person_id", "person_id")
        .columnMapping("person.first_name", "first_name")
        .columnMapping("person.last_name", "last_name")
            .create();

        List<Person> people =
            Ebean.find(Person.class).setRawSql(rawSql).
            where().eq("tag.id", id).orderBy("person.last_name, person.first_name").findList();

        Set<Person> people_with_family = new HashSet<Person>();
        for (Person p : people) {
            if (p.family != null) {
                people_with_family.addAll(p.family.family_members);
            }
        }

        if (the_tag.title.equals("Intent to Enroll") ||
            the_tag.title.equals("Enrolling"))
        {
            return viewIntentToEnroll(the_tag, people, people_with_family);
        }
        else
        {
            return ok(views.html.tag.render(the_tag, people, people_with_family));
        }
    }

    public static Result viewIntentToEnroll(Tag the_tag, List<Person> students,
        Set<Person> family_members)
    {
        return ok(views.html.intent_to_enroll_tag.render(the_tag, students, family_members));
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

        String task_id_string = filledForm.field("comment_task_ids").value();

        if (task_id_string.length() > 0 || new_comment.message.length() > 0) {
            new_comment.save();

            String[] task_ids = task_id_string.split(",");
            for (String id_string : task_ids) {
                if (!id_string.isEmpty()) {
                    int id = Integer.parseInt(id_string);
                    if (id >= 1) {
                        CompletedTask.create(Task.find.byId(id), new_comment);
                    }
                }
            }

            return ok(views.html.comment_fragment.render(Comment.find.byId(new_comment.id)));
        } else {
            return ok();
        }
    }

    public static Result addDonation() {
        Form<Donation> filledForm = donationForm.bindFromRequest();
        Donation new_donation = new Donation();

        new_donation.person = Person.find.byId(Integer.parseInt(filledForm.field("person").value()));
        new_donation.description = filledForm.field("description").value();
        new_donation.dollar_value = Float.parseFloat(filledForm.field("dollar_value").value());

        try
        {
            new_donation.date = new SimpleDateFormat("yyyy-MM-dd").parse(filledForm.field("date").value());
            // Set time to 12 noon so that time zone issues won't bump us to the wrong day.
            new_donation.date.setHours(12);
        }
        catch (ParseException e)
        {
            new_donation.date = new Date();
        }

        new_donation.is_cash = filledForm.field("donation_type").value().equals("Cash");
        if (filledForm.field("needs_thank_you").value() != null) {
            new_donation.thanked = !filledForm.field("needs_thank_you").value().equals("on");
        } else {
            new_donation.thanked = true;
        }

        if (filledForm.field("needs_indiegogo_reward").value() != null) {
            new_donation.indiegogo_reward_given = !filledForm.field("needs_indiegogo_reward").value().equals("on");
        } else {
            new_donation.indiegogo_reward_given = true;
        }

        new_donation.save();

        return ok(views.html.donation_fragment.render(Donation.find.byId(new_donation.id)));
    }

    public static Result donationThankYou(int id)
    {
        Donation d = Donation.find.byId(id);
        d.thanked = true;
        d.thanked_by_user = getCurrentUser();
        d.thanked_time = new Date();

        d.save();
        return ok();
    }

    public static Result donationIndiegogoReward(int id)
    {
        Donation d = Donation.find.byId(id);
        d.indiegogo_reward_given = true;
        d.indiegogo_reward_by_user = getCurrentUser();
        d.indiegogo_reward_given_time = new Date();

        d.save();
        return ok();
    }

    public static Result donationsNeedingThankYou()
    {
        List<Donation> donations = Donation.find.where().eq("thanked", false).orderBy("date DESC").findList();

        return ok(views.html.donation_list.render("Donations needing thank you", donations));
    }

    public static Result donationsNeedingIndiegogo()
    {
        List<Donation> donations = Donation.find.where().eq("indiegogo_reward_given", false).orderBy("date DESC").findList();

        return ok(views.html.donation_list.render("Donations needing Indiegogo reward", donations));
    }

    public static Result donations()
    {
        return ok(views.html.donation_list.render("All donations", Donation.find.orderBy("date DESC").findList()));
    }

    public static int calcAge(Person p) {
        return (int)((new Date().getTime() - p.dob.getTime()) / 1000 / 60 / 60 / 24 / 365.25);
    }

    public static int calcAgeAtBeginningOfSchool(Person p) {
        return (int)((new Date(113, 8, 29).getTime() - p.dob.getTime()) / 1000 / 60 / 60 / 24 / 365.25);
    }

    public static Configuration getConfiguration() {
		return Play.application().configuration().getConfig("school_crm");
	}

    public static String formatDob(Date d) {
        return new SimpleDateFormat("MMMM d, ''yy").format(d);
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

    public static Result viewTaskList(Integer id) {
        TaskList list = TaskList.find.byId(id);
        List<Person> people = new ArrayList<Person>();

        for( Task t: list.tasks ) {
            for( CompletedTask ct : t.completed_tasks ) {
                if (!people.contains(ct.person)) {
                    people.add(ct.person);
                }
            }
        }

        Collections.sort(people);

        return ok(views.html.task_list.render(list, people));
    }
}
