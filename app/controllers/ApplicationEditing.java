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

@With(DumpOnError.class)
public class ApplicationEditing extends Controller {

    @Secured.Auth(UserRole.ROLE_EDIT_RECENT_JC)
    public static Result editTodaysMinutes() {
        Meeting the_meeting = Meeting.find.where()
            .eq("organization", Organization.getByHost())
            .eq("date", new Date()).findUnique();
        if (the_meeting == null) {
            CachedPage.remove(Application.CACHE_INDEX);
            the_meeting = Meeting.create(new Date());
            the_meeting.save();
        }
        return editMinutes(the_meeting);
    }

    static Result tooOldToEdit() {
        return unauthorized("This JC data is too old for you to edit.");
    }

    static boolean authToEdit(Date date) {
        User u = Application.getCurrentUser();

        if (u.hasRole(UserRole.ROLE_EDIT_ALL_JC)) {
            return true;
        }

        Date now = new Date();
        // Can only edit things that are less than 7 days old.
        return (now.getTime() - date.getTime()) < 7 * 24 * 60 * 60 * 1000;
    }

    @Secured.Auth(UserRole.ROLE_EDIT_RECENT_JC)
    public static Result editMinutes(Meeting meeting) {
        if (!authToEdit(meeting.date)) {
            return tooOldToEdit();
        }

        response().setHeader("Cache-Control", "max-age=0, no-cache, no-store");
        response().setHeader("Pragma", "no-cache");
        return ok(views.html.edit_minutes.render(meeting, Case.getOpenCases()));
    }

    @Secured.Auth(UserRole.ROLE_EDIT_RECENT_JC)
    public static Result editMeeting(int meeting_id) {
        Meeting the_meeting = Meeting.findById(meeting_id);
        return editMinutes(the_meeting);
    }

    // This method is synchronized so that there is no race condition
    // between the moment the new case number is generated and the moment
    // the new case is persisted. If not synchronized, it is possible
    // for two cases with the same ID to be generated, leading to an error when
    // the second case is persisted because of a violated unique constraint.
    @Secured.Auth(UserRole.ROLE_EDIT_RECENT_JC)
    public synchronized static Result createCase(Integer meeting_id) {
        Meeting m = Meeting.findById(meeting_id);

        String next_num = "" + (m.cases.size() + 1);
        if (next_num.length() == 1) {
            next_num = "0" + next_num;
        }
        String case_num = OrgConfig.get().getCaseNumberPrefix(m) + next_num;

        Case new_case = Case.create(case_num, m);
        return ok("[" + new_case.id + ", \"" + new_case.case_number + "\"]");
    }

    @Secured.Auth(UserRole.ROLE_EDIT_RECENT_JC)
    public static Result continueCase(Integer meeting_id, Integer case_id) {
        Meeting m = Meeting.findById(meeting_id);
        Case c = Case.findById(case_id);

        if (m == null || c == null || c.date_closed != null || c.meeting == m) {
            System.out.println("Error in continueCase -- illegal access");
            return unauthorized();
        }

        c.continueInMeeting(m);
        return ok(Utils.toJson(c));
    }

    @Secured.Auth(UserRole.ROLE_EDIT_RECENT_JC)
    public static Result saveCase(Integer id) {
        CachedPage.remove(Application.CACHE_INDEX);
        Case c = Case.findById(id);

        c.edit(request().queryString());
        c.save();

        return ok();
    }

    @Secured.Auth(UserRole.ROLE_EDIT_RECENT_JC)
    public static Result addPersonAtMeeting(Integer meeting_id, Integer person_id,
        Integer role) {
        Meeting m = Meeting.find.byId(meeting_id);

        PersonAtMeeting.create(m, Person.find.ref(person_id), role);

        return ok();
    }

    @Secured.Auth(UserRole.ROLE_EDIT_RECENT_JC)
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

    @Secured.Auth(UserRole.ROLE_EDIT_RECENT_JC)
    public static Result addPersonAtCase(Integer case_id, Integer person_id, Integer role)
    {
        CachedPage.remove(Application.CACHE_INDEX);
        PersonAtCase.create(Case.find.ref(case_id), Person.find.ref(person_id), role);
        return ok();
    }

    @Secured.Auth(UserRole.ROLE_EDIT_RECENT_JC)
    public static Result removePersonAtCase(Integer case_id, Integer person_id, Integer role)
    {
        SqlUpdate update = Ebean.createSqlUpdate(
            "DELETE from person_at_case where case_id = :case_id "+
            "and person_id = :person_id "+
            "and role = :role");
        update.setParameter("case_id", case_id);
        update.setParameter("person_id", person_id);
        update.setParameter("role", role);

        Ebean.execute(update);
        return ok();
    }

    @Secured.Auth(UserRole.ROLE_EDIT_RECENT_JC)
    public static Result addCharge(Integer case_id)
    {
        Charge c = Charge.create(Case.find.ref(case_id));
        return ok("" + c.id);
    }

    @Secured.Auth(UserRole.ROLE_EDIT_RECENT_JC)
    public static Result saveCharge(int id) {
        CachedPage.remove(Application.CACHE_INDEX);
        Charge c = Charge.findById(id);

        c.edit(request().queryString());
        c.save();

        return ok();
    }

    @Secured.Auth(UserRole.ROLE_EDIT_RECENT_JC)
    public static Result setResolutionPlanComplete(Integer chargeId, Boolean complete) {
        Charge c = Charge.findById(chargeId);
        c.setRPComplete(complete);

        return ok();
    }

    @Secured.Auth(UserRole.ROLE_EDIT_RECENT_JC)
    public static Result removeCharge(int id) {
        Charge c = Charge.findById(id);
        c.delete();

        return ok();
    }

    @Secured.Auth(UserRole.ROLE_EDIT_RECENT_JC)
    public static Result enterSchoolMeetingDecisions() {
        return ok(views.html.enter_sm_decisions.render(Application.getActiveSchoolMeetingReferrals()));
    }

    @Secured.Auth(UserRole.ROLE_EDIT_RECENT_JC)
    public static Result editSchoolMeetingDecision(Integer charge_id) {
        Charge c = Charge.findById(charge_id);

        if (!authToEdit(c.sm_decision_date)) {
            return tooOldToEdit();
        }

        return ok(views.html.edit_sm_decision.render(c));
    }

    @Secured.Auth(UserRole.ROLE_EDIT_RECENT_JC)
    public static Result saveSchoolMeetingDecisions() {
        CachedPage.remove(Application.CACHE_INDEX);
        Map<String, String[]> form_data = request().body().asFormUrlEncoded();

        Integer charge_id = Integer.parseInt(form_data.get("charge_id")[0]);
        String decision = form_data.get("sm_decision")[0];
        Date date = Application.getDateFromString(form_data.get("date")[0]);

        Charge c = Charge.findById(charge_id);
        c.updateSchoolMeetingDecision(decision, date);
        c.save();

        return redirect(routes.ApplicationEditing.enterSchoolMeetingDecisions());
    }

    @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
	public static Result addChapter() {
        CachedPage.remove(Application.CACHE_MANUAL);
		Form<Chapter> form = Form.form(Chapter.class);
		return ok(views.html.edit_chapter.render(form, true));
	}

    @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
	public static Result editChapter(Integer id) {
		Form<Chapter> filled_form = new Form<Chapter>(Chapter.class).fill(Chapter.findById(id));
		return ok(views.html.edit_chapter.render(filled_form, false));
	}

    @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
	public static Result saveChapter() {
        CachedPage.remove(Application.CACHE_MANUAL);
		Form<Chapter> form = new Form<Chapter>(Chapter.class).bindFromRequest();

		Chapter c = null;
		if (form.field("id").value() != null) {
			c = Chapter.findById(Integer.parseInt(form.field("id").value()));
			c.updateFromForm(form);
		} else {
			c = Chapter.create(form);
		}

		return redirect(routes.Application.viewChapter(c.id));
	}

    @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
	public static Result addSection(Integer chapterId) {
        CachedPage.remove(Application.CACHE_MANUAL);
		Form<Section> form = Form.form(Section.class);
		Map<String, String> map = new HashMap<String, String>();
		map.put("chapter.id", "" + chapterId);
		form = form.bind(map, "chapter.id");
		return ok(views.html.edit_section.render(form, Chapter.findById(chapterId), true, Chapter.all()));
	}

    @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
	public static Result editSection(Integer id) {
		Section existing_section = Section.findById(id);
		Form<Section> filled_form = new Form<Section>(Section.class).fill(existing_section);
		return ok(views.html.edit_section.render(filled_form, existing_section.chapter, false, Chapter.all()));
	}

    @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
	public static Result saveSection() {
        CachedPage.remove(Application.CACHE_MANUAL);
		Form<Section> form = new Form<Section>(Section.class).bindFromRequest();

		Section s = null;
		if (form.field("id").value() != null) {
			s = Section.findById(Integer.parseInt(form.field("id").value()));
			s.updateFromForm(form);
		} else {
			s = Section.create(form);
		}

		return redirect(routes.Application.viewChapter(s.chapter.id).url() + "#section_" + s.id);
	}

    @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
	public static Result addEntry(Integer sectionId) {
        CachedPage.remove(Application.CACHE_MANUAL);
		Form<Entry> form = Form.form(Entry.class);
		Map<String, String> map = new HashMap<String, String>();
		map.put("section.id", "" + sectionId);
		form = form.bind(map, "section.id");
		return ok(views.html.edit_entry.render(form, Section.findById(sectionId), true, Chapter.all()));
	}

    @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
	public static Result editEntry(Integer id) {
        Entry e = Entry.findById(id);
		Form<Entry> filled_form = new Form<Entry>(Entry.class).fill(e);
		return ok(views.html.edit_entry.render(filled_form, e.section, false, Chapter.all()));
	}

    @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
	public static Result saveEntry() {
        CachedPage.remove(Application.CACHE_MANUAL);
		Form<Entry> form = new Form<Entry>(Entry.class).bindFromRequest();

		Entry e = null;
		if (form.field("id").value() != null) {
			e = Entry.findById(Integer.parseInt(form.field("id").value()));
			e.updateFromForm(form);
		} else {
			e = Entry.create(form);
		}

		return redirect(routes.Application.viewChapter(e.section.chapter.id).url() + "#entry_" + e.id);
	}

}
