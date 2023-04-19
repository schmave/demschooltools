package controllers;

import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.ebean.Ebean;
import io.ebean.SqlUpdate;
import javax.inject.Inject;

import models.*;

import play.api.libs.mailer.MailerClient;
import play.db.Database;
import play.data.*;
import play.mvc.*;

@With(DumpOnError.class)
public class ApplicationEditing extends Controller {

    private final Database mDatabase;
    MailerClient mMailer;


    static ExecutorService sExecutor = Executors.newFixedThreadPool(2);
    static Set<Integer> sAlreadyEmailedCharges = new HashSet<>();
    FormFactory mFormFactory;

    @Inject
    public ApplicationEditing(Database db,
                              FormFactory formFactory,
                              MailerClient mailer) {
        this.mDatabase = db;
        this.mFormFactory = formFactory;
        mMailer = mailer;
    }

    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public Result editTodaysMinutes(Http.Request request) {
        Meeting the_meeting = Meeting.find.query().where()
            .eq("organization", Organization.getByHost(request))
            .eq("date", new Date()).findOne();
        if (the_meeting == null) {
            CachedPage.remove(CachedPage.JC_INDEX, Organization.getByHost(request));
            the_meeting = Meeting.create(new Date(), Organization.getByHost(request));
            the_meeting.save();
        }
        return editMinutes(the_meeting, request);
    }

    Result tooOldToEdit() {
        return unauthorized("This JC data is too old for you to edit.");
    }

    static boolean authToEdit(Date date, Http.Request request) {
        User u = Application.getCurrentUser(request);

        if (u.hasRole(UserRole.ROLE_EDIT_ALL_JC)) {
            return true;
        }

        Date now = new Date();
        long diff = now.getTime() - date.getTime();
        long oneDay = 24 * 60 * 60 * 1000;
        return (diff < 7 * oneDay && u.hasRole(UserRole.ROLE_EDIT_7_DAY_JC))
                || (diff < 31 * oneDay && u.hasRole(UserRole.ROLE_EDIT_31_DAY_JC));
    }

    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public Result editMinutes(Meeting meeting, Http.Request request) {
        if (!authToEdit(meeting.date, request)) {
            return tooOldToEdit();
        }
        meeting.prepareForEditing(Organization.getByHost(request));

        return ok(views.html.edit_minutes.render(OrgConfig.get(Organization.getByHost(request)), meeting, Case.getOpenCases(Organization.getByHost(request))))
                .withHeader("Cache-Control", "max-age=0, no-cache, no-store")
                .withHeader("Pragma", "no-cache");
    }

    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public Result editMeeting(int meeting_id, Http.Request request) {
        Meeting the_meeting = Meeting.findById(meeting_id, Organization.getByHost(request));
        return editMinutes(the_meeting, request);
    }

    // This method is synchronized so that there is no race condition
    // between the moment the new case number is generated and the moment
    // the new case is persisted. If not synchronized, it is possible
    // for two cases with the same ID to be generated, leading to an error when
    // the second case is persisted because of a violated unique constraint.
    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public synchronized Result createCase(Integer meeting_id, Http.Request request) {
        Organization org = Organization.getByHost(request);
        Meeting m = Meeting.findById(meeting_id, org);

        String next_num = "" + (m.cases.size() + 1);
        if (next_num.length() == 1) {
            next_num = "0" + next_num;
        }
        String case_num = OrgConfig.get(org).getCaseNumberPrefix(m) + next_num;

        Case new_case = Case.create(case_num, m);
        return ok("[" + new_case.id + ", \"" + new_case.case_number + "\"]");
    }

    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public Result continueCase(Integer meeting_id, Integer case_id, Http.Request request) {
        Meeting m = Meeting.findById(meeting_id, Organization.getByHost(request));
        Case c = Case.findById(case_id, Organization.getByHost(request));

        if (m == null || c == null || c.date_closed != null || c.meeting == m) {
            System.out.println("Error in continueCase -- illegal access");
            return unauthorized();
        }

        c.continueInMeeting(m);
        return ok(Utils.toJson(c));
    }

    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public Result saveCase(Integer id, Http.Request request) {
        CachedPage.remove(CachedPage.JC_INDEX, Organization.getByHost(request));
        Case c = Case.findById(id, Organization.getByHost(request));

        c.edit(request.body().asFormUrlEncoded());
        c.save();

        return ok();
    }

    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public Result addPersonAtMeeting(Integer meeting_id, Integer person_id,
        Integer role) {
        Meeting m = Meeting.find.byId(meeting_id);

        PersonAtMeeting.create(m, Person.find.ref(person_id), role);

        return ok();
    }

    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public Result removePersonAtMeeting(Integer meeting_id, Integer person_id,
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

    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public Result addPersonAtCase(Integer case_id, Integer person_id, Integer role)
    {
        CachedPage.remove(CachedPage.JC_INDEX, Organization.getByHost(request));
        PersonAtCase.create(Case.find.ref(case_id), Person.find.ref(person_id), role);
        return ok();
    }

    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public Result removePersonAtCase(Integer case_id, Integer person_id, Integer role)
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

    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public Result addReferencedCase(Integer case_id, Integer referenced_case_id, Http.Request request)
    {
        Case referencing_case = Case.findById(case_id, Organization.getByHost(request));
        Case referenced_case = Case.findById(referenced_case_id, Organization.getByHost(request));
        referencing_case.addReferencedCase(referenced_case);
        return ok(Utils.toJson(CaseReference.create(referencing_case, Organization.getByHost(request))));
    }

    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public Result removeReferencedCase(Integer case_id, Integer referenced_case_id, Http.Request request)
    {
        Case referencing_case = Case.findById(case_id, Organization.getByHost(request));
        Case referenced_case = Case.findById(referenced_case_id, Organization.getByHost(request));
        referencing_case.removeReferencedCase(referenced_case);
        return ok(Utils.toJson(CaseReference.create(referencing_case, Organization.getByHost(request))));
    }

    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public Result clearAllReferencedCases(Integer case_id, Http.Request request)
    {
        Case referencing_case = Case.findById(case_id, Organization.getByHost(request));
        for (Case referenced_case : new ArrayList<>(referencing_case.referenced_cases)) {
            referencing_case.removeReferencedCase(referenced_case);
        }
        return ok(Utils.toJson(CaseReference.create(referencing_case, Organization.getByHost(request))));
    }

    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public Result getCaseReferencesJson(Integer case_id, Http.Request request)
    {
        Case referencing_case = Case.findById(case_id, Organization.getByHost(request));
        return ok(Utils.toJson(CaseReference.create(referencing_case, Organization.getByHost(request))));
    }

    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public Result addChargeReferenceToCase(Integer case_id, Integer charge_id, Http.Request request)
    {
        Case referencing_case = Case.findById(case_id, Organization.getByHost(request));
        Charge charge = Charge.findById(charge_id, Organization.getByHost(request));
        if (!referencing_case.referenced_charges.contains(charge)) {
            referencing_case.referenced_charges.add(charge);
            referencing_case.save();
        }
        return ok();
    }

    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public Result removeChargeReferenceFromCase(Integer case_id, Integer charge_id, Http.Request request)
    {
        Case referencing_case = Case.findById(case_id, Organization.getByHost(request));
        Charge charge = Charge.findById(charge_id, Organization.getByHost(request));
        referencing_case.referenced_charges.remove(charge);
        referencing_case.save();
        return ok();
    }

    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public Result addCharge(Integer case_id)
    {
        Charge c = Charge.create(Case.find.ref(case_id));
        return ok("" + c.id);
    }

    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public Result generateChargeFromReference(Integer case_id, Integer referenced_charge_id, Http.Request request)
    {
        Charge charge = Charge.generateFromReference(Case.find.ref(case_id), Charge.find.ref(referenced_charge_id), Organization.getByHost(request));
        return ok(Utils.toJson(charge));
    }

    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public Result saveCharge(int id, Http.Request request) {
        CachedPage.remove(CachedPage.JC_INDEX, Organization.getByHost(request));
        Organization org = Organization.getByHost(request);
        Charge c = Charge.findById(id, org);

        if (c == null) {
            return notFound();
        }

        boolean was_referred_to_sm = c.referred_to_sm;
        c.edit(request.queryString());
        if (!was_referred_to_sm && c.referred_to_sm &&
                !sAlreadyEmailedCharges.contains(c.id) &&
                !NotificationRule.findByType(NotificationRule.TYPE_SCHOOL_MEETING, org).isEmpty()) {
            final OrgConfig org_config = OrgConfig.get(org);
            sExecutor.submit(() -> {
                try {
                    Thread.sleep(1000 * 60 * 5);
                    Charge c1 = Charge.find.byId(id);
                    if (c1.referred_to_sm && !sAlreadyEmailedCharges.contains(c1.id)) {
                        sAlreadyEmailedCharges.add(c1.id);
                        List<NotificationRule> rules = NotificationRule.findByType(
                                NotificationRule.TYPE_SCHOOL_MEETING, org);
                        for (NotificationRule rule : rules) {
                            play.libs.mailer.Email mail = new play.libs.mailer.Email();
                            mail.addTo(rule.email);
                            mail.setSubject(c1.person.getInitials() + "'s charge"
                                    + " referred to School Meeting in case #" + c1.the_case.case_number);
                            mail.setFrom("DemSchoolTools <noreply@demschooltools.com>");
                            mail.setBodyText(
                                "" + c1.person.getDisplayName()
                                + " was charged with " + c1.getRuleTitle()
                                + " and the charge was referred to School Meeting in case #" + c1.the_case.case_number + ".\n\n"
                                + "For more information, view today's minutes:\n\n"
                                + org_config.people_url + routes.Application.viewMeeting(c1.the_case.meeting.id).toString()
                                + "\n\n"
                            );
                            mMailer.send(mail);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        c.save();

        return ok();
    }

    @Secured.Auth(UserRole.ROLE_EDIT_RESOLUTION_PLANS)
    public Result setResolutionPlanComplete(Integer chargeId, Boolean complete, Http.Request request) {
        Charge c = Charge.findById(chargeId, Organization.getByHost(request));
        c.setRPComplete(complete);

        return ok();
    }

    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public Result removeCharge(int id, Http.Request request) {
        Charge c = Charge.findById(id, Organization.getByHost(request));
        if (c == null) {
            return notFound();
        }

        c.delete();
        return ok();
    }

    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public Result enterSchoolMeetingDecisions(Http.Request request) {
        return ok(views.html.enter_sm_decisions.render(OrgConfig.get(Organization.getByHost(request)), Application.getActiveSchoolMeetingReferrals(
                Organization.getByHost(request))));
    }

    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public Result editSchoolMeetingDecision(Integer charge_id, Http.Request request) {
        Charge c = Charge.findById(charge_id, Organization.getByHost(request));

        if (!authToEdit(c.sm_decision_date, request)) {
            return tooOldToEdit();
        }

        return ok(views.html.edit_sm_decision.render(OrgConfig.get(Organization.getByHost(request)), c));
    }

    @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
    public Result saveSchoolMeetingDecisions(Http.Request request) {
        CachedPage.remove(CachedPage.JC_INDEX, Organization.getByHost(request));
        Map<String, String[]> form_data = request.body().asFormUrlEncoded();

        int charge_id = Integer.parseInt(form_data.get("charge_id")[0]);
        String decision = form_data.get("sm_decision")[0];
        Date date = Application.getDateFromString(form_data.get("date")[0]);

        Charge c = Charge.findById(charge_id, Organization.getByHost(request));
        c.updateSchoolMeetingDecision(decision, date);
        c.save();

        return redirect(routes.ApplicationEditing.enterSchoolMeetingDecisions());
    }

    void onManualChange() {
        CachedPage.remove(CachedPage.MANUAL_INDEX, Organization.getByHost(request));
        try {
            Connection conn = mDatabase.getConnection();
            conn.prepareStatement("REFRESH MATERIALIZED VIEW entry_index WITH DATA").execute();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
    public Result addChapter() {
        Form<Chapter> form = mFormFactory.form(Chapter.class);
        return ok(views.html.edit_chapter.render(OrgConfig.get(Organization.getByHost(request)), form, true));
    }

    @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
    public Result editChapter(Integer id, Http.Request request) {
        Form<Chapter> filled_form = mFormFactory.form(Chapter.class).fill(Chapter.findById(id, Organization.getByHost(request)));
        return ok(views.html.edit_chapter.render(OrgConfig.get(Organization.getByHost(request)), filled_form, false));
    }

    @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
    public Result saveChapter(Http.Request request) {
        Form<Chapter> form = mFormFactory.form(Chapter.class).bindFromRequest(request);

        Chapter c;
        if (form.field("id").value().isPresent()) {
            c = Chapter.findById(Integer.parseInt(form.field("id").value().get()), Organization.getByHost(request));
            c.updateFromForm(form);
        } else {
            c = Chapter.create(form, Organization.getByHost(request));
        }

        onManualChange();
        return redirect(routes.Application.viewChapter(c.id));
    }

    @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
    public Result addSection(Integer chapterId, Http.Request request) {
        Form<Section> form = mFormFactory.form(Section.class);
        Map<String, String> map = new HashMap<>();
        map.put("chapter.id", "" + chapterId);
        form = form.bind(map, "chapter.id");
        return ok(views.html.edit_section.render(OrgConfig.get(Organization.getByHost(request)), form, Chapter.findById(chapterId, Organization.getByHost(request)), true, Chapter.all(Organization.getByHost(request))));
    }

    @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
    public Result editSection(Integer id, Http.Request request) {
        Section existing_section = Section.findById(id, Organization.getByHost(request));
        Form<Section> filled_form = mFormFactory.form(Section.class).fill(existing_section);
        return ok(views.html.edit_section.render(OrgConfig.get(Organization.getByHost(request)), filled_form, existing_section.chapter, false, Chapter.all(Organization.getByHost(request))));
    }

    @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
    public Result saveSection(Http.Request request) {
        Form<Section> form = mFormFactory.form(Section.class).bindFromRequest(request);

        Section s;
        if (form.field("id").value().isPresent()) {
            s = Section.findById(Integer.parseInt(form.field("id").value().get()), Organization.getByHost(request));
            s.updateFromForm(form);
        } else {
            s = Section.create(form);
        }

        onManualChange();
        return redirect(routes.Application.viewChapter(s.chapter.id).url() + "#section_" + s.id);
    }

    @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
    public Result addEntry(Integer sectionId, Http.Request request) {
        Form<Entry> form = mFormFactory.form(Entry.class);
        Map<String, String> map = new HashMap<>();
        map.put("section.id", "" + sectionId);
        form = form.bind(map, "section.id");
        return ok(views.html.edit_entry.render(OrgConfig.get(Organization.getByHost(request)), form, Section.findById(sectionId, Organization.getByHost(request)), true, Chapter.all(Organization.getByHost(request))));
    }

    @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
    public Result editEntry(Integer id, Http.Request request) {
        Entry e = Entry.findById(id, Organization.getByHost(request));
        Form<Entry> filled_form = mFormFactory.form(Entry.class).fill(e);
        return ok(views.html.edit_entry.render(OrgConfig.get(Organization.getByHost(request)), filled_form, e.section, false, Chapter.all(Organization.getByHost(request))));
    }

    @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
    public Result saveEntry(Http.Request request) {
        Form<Entry> form = mFormFactory.form(Entry.class).bindFromRequest(request);

        Entry e;
        if (form.field("id").value().isPresent()) {
            e = Entry.findById(Integer.parseInt(form.field("id").value().get()), Organization.getByHost(request));
            e.updateFromForm(form);
        } else {
            e = Entry.create(form);
        }

        onManualChange();
        return redirect(routes.Application.viewChapter(e.section.chapter.id).url() + "#entry_" + e.id);
    }

}