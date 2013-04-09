package controllers;

import play.*;
import play.data.*;
import play.mvc.*;

import models.*;

import views.html.*;


public class Application extends Controller {

    static Form<Person> personForm = Form.form(Person.class);

    public static Result index() {
        return redirect(routes.Application.people());
    }

    public static Result people() {
        return ok(views.html.index.render(Person.all(), personForm));
    }

    public static Result newPerson() {
        Form<Person> filledForm = personForm.bindFromRequest();
        if(filledForm.hasErrors()) {
            return badRequest(
                views.html.index.render(Person.all(), filledForm)
            );
        } else {
            Person.create(filledForm.get());
            return redirect(routes.Application.people());
        }
    }

    public static Result deletePerson(Integer id) {
        Person.delete(id);
        return redirect(routes.Application.people());
    }

}
