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

@Security.Authenticated(EditorSecured.class)
public class ApplicationEditing extends Controller {

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

    public static Result editMeeting(int meeting_id) {
        Meeting the_meeting = Meeting.find.byId(meeting_id);
        return editMinutes(the_meeting);
    }

    public static Result createCase(Integer meeting_id) {
        Meeting m = Meeting.find.ref(meeting_id);

        String next_num = "" + (m.cases.size() + 1);
        if (next_num.length() == 1) {
            next_num = "0" + next_num;
        }
        String id = m.getCaseNumberPrefix() + next_num;

        Case new_case = Case.create(id, m);
        return ok(id);
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

    public static Result removeCharge(int id) {
        Charge c = Charge.find.byId(id);
        c.delete();

        return ok();
    }

    public static Result enterSchoolMeetingDecisions() {
        return ok(views.html.enter_sm_decisions.render(Application.getActiveSchoolMeetingReferrals()));
    }

    public static Result saveSchoolMeetingDecisions() {
        Map<String, String[]> form_data = request().body().asFormUrlEncoded();

        Integer charge_id = Integer.parseInt(form_data.get("charge_id")[0]);
        String decision = form_data.get("sm_decision")[0];
        Date date = Application.getDateFromString(form_data.get("date")[0]);

        Charge c = Charge.find.byId(charge_id);
        c.updateSchoolMeetingDecision(decision, date);
        c.save();

        return redirect(routes.ApplicationEditing.enterSchoolMeetingDecisions());
    }

	public static Result viewRules() {
		return ok(views.html.rules.render(Rule.find.orderBy("title ASC").findList(), Form.form(Rule.class)));
	}

	public static Result editRuleForm(Integer id) {
		Form<Rule> filled_form = new Form<Rule>(Rule.class).fill(Rule.find.byId(id));
		return ok(views.html.edit_rule.render(filled_form));
	}

	public static Result editRule(Integer id) {
		// save rule change
		Form<Rule> form = new Form<Rule>(Rule.class).bindFromRequest();
		Rule.find.byId(id).updateFromForm(form);

		return redirect(routes.ApplicationEditing.viewRules());
	}

	public static Result addRule() {
		// save new rule
		Form<Rule> form = new Form<Rule>(Rule.class).bindFromRequest();
		Rule new_rule = Rule.create(form);

		return redirect(routes.ApplicationEditing.viewRules());
	}

}
