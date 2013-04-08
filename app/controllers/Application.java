package controllers;

import play.*;
import play.data.*;
import play.mvc.*;

import models.*;

import views.html.*;


public class Application extends Controller {

    static Form<Student> studentForm = Form.form(Student.class);

    public static Result index() {
        return redirect(routes.Application.students());
    }

    public static Result students() {
        return ok(views.html.index.render(Student.all(), studentForm));
    }

    public static Result newStudent() {
        Form<Student> filledForm = studentForm.bindFromRequest();
        if(filledForm.hasErrors()) {
            return badRequest(
                views.html.index.render(Student.all(), filledForm)
            );
        } else {
            Student.create(filledForm.get());
            return redirect(routes.Application.students());
        }
    }

    public static Result deleteStudent(Integer id) {
        Student.delete(id);
        return redirect(routes.Application.students());
    }

}
