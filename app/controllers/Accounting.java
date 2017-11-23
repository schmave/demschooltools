package controllers;

import java.util.*;
import models.*;
import play.mvc.*;
import play.data.*;

@With(DumpOnError.class)
@Secured.Auth(UserRole.ROLE_ALL_ACCESS)
public class Accounting extends Controller {

    public Result transaction(Integer id) {
        Transaction transaction = Transaction.findById(id);
        return ok(views.html.transaction.render(transaction));
    }

    public Result newTransaction() {
        Form<Transaction> form = Form.form(Transaction.class);
        return ok(views.html.new_transaction.render(form));
    }

    public Result makeNewTransaction() {
        Form<Transaction> form = Form.form(Transaction.class);
        Form<Transaction> filledForm = form.bindFromRequest();
        if(filledForm.hasErrors()) {
            System.out.println("ERRORS: " + filledForm.errorsAsJson().toString());
            return badRequest(
                views.html.new_transaction.render(filledForm)
            );
        } else {
            Transaction transaction = Transaction.create(filledForm);
            return redirect(routes.Accounting.transaction(transaction.id));
        }
    }

    public Result institutions() {
        List<Institution> institutions = Institution.all();
        return ok(views.html.institutions.render(institutions));
    }

    public Result newInstitution() {
        Form<Institution> form = Form.form(Institution.class);
        return ok(views.html.new_institution.render(form));
    }

    public Result makeNewInstitution() {
        Form<Institution> form = Form.form(Institution.class);
        Form<Institution> filledForm = form.bindFromRequest();
        if(filledForm.hasErrors()) {
            System.out.println("ERRORS: " + filledForm.errorsAsJson().toString());
            return badRequest(
                views.html.new_institution.render(filledForm)
            );
        } else {
            String name = filledForm.field("name").value();
            InstitutionType type = parseInstitutionType(filledForm.field("type").value());
            Institution institution = Institution.create(name, type);
            return redirect(routes.Accounting.institutions());
        }
    }

    private InstitutionType parseInstitutionType(String value) {
        return value == null || value == "" 
            ? null 
            : InstitutionType.values()[Integer.parseInt(value)];
    }
}