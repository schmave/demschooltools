package controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.avaje.ebean.Expr;

import models.*;

import play.*;
import play.data.*;
import play.libs.Json;
import play.mvc.*;

import views.html.*;

/*
   TODO

* be able to edit a Person (whether family or no)

* add/remove phone numbers
* add/remove tags
* add comments

* browse people by tag

 */

@Security.Authenticated(Secured.class)
public class Application extends Controller {

    static Form<Person> personForm = Form.form(Person.class);

    public static Result index() {
        return redirect(routes.Application.people());
    }

    public static Result people() {
        return ok(views.html.index.render(Person.all()));
    }

    public static Result person(Integer id) {
        Person the_person = Person.find.ref(id);
        return ok(views.html.family.render(
            the_person,
            Person.find.where().isNotNull("family").eq("family", the_person.family).ne("person_id", the_person.person_id).findList()));
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
            values.put("label", p.first_name + " " + p.last_name);
            values.put("id", "" + p.person_id);
            result.add(values);
        }

        return ok(Json.stringify(Json.toJson(result)));
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
            Person.create(filledForm.get(), Integer.parseInt(filledForm.field("same_family_id").value()));
            return redirect(routes.Application.people());
        }
    }

    public static Result deletePerson(Integer id) {
        Person.delete(id);
        return redirect(routes.Application.people());
    }

}
