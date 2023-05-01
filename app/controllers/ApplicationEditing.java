package controllers;

import io.ebean.DB;
import io.ebean.SqlUpdate;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import models.*;
import play.api.libs.mailer.MailerClient;
import play.data.Form;
import play.data.FormFactory;
import play.db.Database;
import play.i18n.MessagesApi;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.html.*;

public class ApplicationEditing extends Controller {

  private final Database mDatabase;
  MailerClient mMailer;
  final MessagesApi mMessagesApi;

  static ExecutorService sExecutor = Executors.newFixedThreadPool(2);
  static Set<Integer> sAlreadyEmailedCharges = new HashSet<>();
  FormFactory mFormFactory;

  @Inject
  public ApplicationEditing(
      Database db, FormFactory formFactory, MailerClient mailer, MessagesApi messagesApi) {
    this.mDatabase = db;
    this.mFormFactory = formFactory;
    mMailer = mailer;
    mMessagesApi = messagesApi;
  }

  @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
  public Result editTodaysMinutes(Http.Request request) {
    Meeting the_meeting =
        Meeting.find
            .query()
            .where()
            .eq("organization", Utils.getOrg(request))
            .eq("date", new Date())
            .findOne();
    if (the_meeting == null) {
      CachedPage.remove(CachedPage.JC_INDEX, Utils.getOrg(request));
      the_meeting = Meeting.create(new Date(), Utils.getOrg(request));
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
    if (!authToEdit(meeting.getDate(), request)) {
      return tooOldToEdit();
    }
    meeting.prepareForEditing(Utils.getOrg(request));

    return ok(edit_minutes.render(
            meeting,
            Case.getOpenCases(Utils.getOrg(request)),
            request,
            mMessagesApi.preferred(request)))
        .withHeader("Cache-Control", "max-age=0, no-cache, no-store")
        .withHeader("Pragma", "no-cache");
  }

  @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
  public Result editMeeting(int meeting_id, Http.Request request) {
    Meeting the_meeting = Meeting.findById(meeting_id, Utils.getOrg(request));
    return editMinutes(the_meeting, request);
  }

  // This method is synchronized so that there is no race condition
  // between the moment the new case number is generated and the moment
  // the new case is persisted. If not synchronized, it is possible
  // for two cases with the same ID to be generated, leading to an error when
  // the second case is persisted because of a violated unique constraint.
  @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
  public synchronized Result createCase(Integer meeting_id, Http.Request request) {
    Organization org = Utils.getOrg(request);
    Meeting m = Meeting.findById(meeting_id, org);

    String next_num = "" + (m.cases.size() + 1);
    if (next_num.length() == 1) {
      next_num = "0" + next_num;
    }
    String case_num = Utils.getOrgConfig(org).getCaseNumberPrefix(m) + next_num;

    Case new_case = Case.create(case_num, m);
    return ok("[" + new_case.getId() + ", \"" + new_case.getCaseNumber() + "\"]");
  }

  @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
  public Result continueCase(Integer meeting_id, Integer case_id, Http.Request request) {
    Meeting m = Meeting.findById(meeting_id, Utils.getOrg(request));
    Case c = Case.findById(case_id, Utils.getOrg(request));

    if (m == null || c == null || c.getDateClosed() != null || c.getMeeting() == m) {
      System.out.println("Error in continueCase -- illegal access");
      return unauthorized();
    }

    c.continueInMeeting(m);
    return ok(Utils.toJson(c));
  }

  @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
  public Result saveCase(Integer id, Http.Request request) {
    CachedPage.remove(CachedPage.JC_INDEX, Utils.getOrg(request));
    Case c = Case.findById(id, Utils.getOrg(request));

    c.edit(request.body().asFormUrlEncoded());
    c.save();

    return ok();
  }

  @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
  public Result addPersonAtMeeting(Integer meeting_id, Integer personId, Integer role) {
    Meeting m = Meeting.find.byId(meeting_id);

    PersonAtMeeting.create(m, Person.find.ref(personId), role);

    return ok();
  }

  @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
  public Result removePersonAtMeeting(Integer meeting_id, Integer personId, Integer role) {
    SqlUpdate update =
        DB.sqlUpdate(
            "DELETE from person_at_meeting where meeting_id = :meeting_id"
                + " and person_id = :person_id and role = :role");
    update.setParameter("meeting_id", meeting_id);
    update.setParameter("person_id", personId);
    update.setParameter("role", role);

    update.executeNow();

    return ok();
  }

  @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
  public Result addPersonAtCase(
      Integer case_id, Integer personId, Integer role, Http.Request request) {
    CachedPage.remove(CachedPage.JC_INDEX, Utils.getOrg(request));
    PersonAtCase.create(Case.find.ref(case_id), Person.find.ref(personId), role);
    return ok();
  }

  @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
  public Result removePersonAtCase(Integer case_id, Integer personId, Integer role) {
    DB.sqlUpdate(
            "DELETE from person_at_case where case_id = :case_id "
                + "and person_id = :person_id "
                + "and role = :role")
        .setParameter("case_id", case_id)
        .setParameter("person_id", personId)
        .setParameter("role", role)
        .executeNow();
    return ok();
  }

  @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
  public Result addReferencedCase(
      Integer case_id, Integer referenced_case_id, Http.Request request) {
    Case referencing_case = Case.findById(case_id, Utils.getOrg(request));
    Case referenced_case = Case.findById(referenced_case_id, Utils.getOrg(request));
    referencing_case.addReferencedCase(referenced_case);
    return ok(Utils.toJson(CaseReference.create(referencing_case, Utils.getOrg(request))));
  }

  @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
  public Result removeReferencedCase(
      Integer case_id, Integer referenced_case_id, Http.Request request) {
    Case referencing_case = Case.findById(case_id, Utils.getOrg(request));
    Case referenced_case = Case.findById(referenced_case_id, Utils.getOrg(request));
    referencing_case.removeReferencedCase(referenced_case);
    return ok(Utils.toJson(CaseReference.create(referencing_case, Utils.getOrg(request))));
  }

  @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
  public Result clearAllReferencedCases(Integer case_id, Http.Request request) {
    Case referencing_case = Case.findById(case_id, Utils.getOrg(request));
    for (Case referenced_case : new ArrayList<>(referencing_case.referenced_cases)) {
      referencing_case.removeReferencedCase(referenced_case);
    }
    return ok(Utils.toJson(CaseReference.create(referencing_case, Utils.getOrg(request))));
  }

  @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
  public Result getCaseReferencesJson(Integer case_id, Http.Request request) {
    Case referencing_case = Case.findById(case_id, Utils.getOrg(request));
    return ok(Utils.toJson(CaseReference.create(referencing_case, Utils.getOrg(request))));
  }

  @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
  public Result addChargeReferenceToCase(Integer case_id, Integer charge_id, Http.Request request) {
    Case referencing_case = Case.findById(case_id, Utils.getOrg(request));
    Charge charge = Charge.findById(charge_id, Utils.getOrg(request));
    if (!referencing_case.referenced_charges.contains(charge)) {
      referencing_case.referenced_charges.add(charge);
      referencing_case.save();
    }
    return ok();
  }

  @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
  public Result removeChargeReferenceFromCase(
      Integer case_id, Integer charge_id, Http.Request request) {
    Case referencing_case = Case.findById(case_id, Utils.getOrg(request));
    Charge charge = Charge.findById(charge_id, Utils.getOrg(request));
    referencing_case.referenced_charges.remove(charge);
    referencing_case.save();
    return ok();
  }

  @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
  public Result addCharge(Integer case_id) {
    Charge c = Charge.create(Case.find.ref(case_id));
    return ok("" + c.getId());
  }

  @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
  public Result generateChargeFromReference(
      Integer case_id, Integer referenced_charge_id, Http.Request request) {
    Charge charge =
        Charge.generateFromReference(
            Case.find.ref(case_id), Charge.find.ref(referenced_charge_id), Utils.getOrg(request));
    return ok(Utils.toJson(charge));
  }

  @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
  public Result saveCharge(int id, Http.Request request) {
    CachedPage.remove(CachedPage.JC_INDEX, Utils.getOrg(request));
    Organization org = Utils.getOrg(request);
    Charge c = Charge.findById(id, org);

    if (c == null) {
      return notFound();
    }

    boolean was_referred_to_sm = c.getReferredToSm();
    c.edit(request.queryString());
    if (!was_referred_to_sm
        && c.getReferredToSm()
        && !sAlreadyEmailedCharges.contains(c.getId())
        && !NotificationRule.findByType(NotificationRule.TYPE_SCHOOL_MEETING, org).isEmpty()) {
      final OrgConfig org_config = Utils.getOrgConfig(org);
      sExecutor.submit(
          () -> {
            try {
              Thread.sleep(1000 * 60 * 5);
              Charge c1 = Charge.find.byId(id);
              if (c1.getReferredToSm() && !sAlreadyEmailedCharges.contains(c1.getId())) {
                sAlreadyEmailedCharges.add(c1.getId());
                List<NotificationRule> rules =
                    NotificationRule.findByType(NotificationRule.TYPE_SCHOOL_MEETING, org);
                for (NotificationRule rule : rules) {
                  play.libs.mailer.Email mail = new play.libs.mailer.Email();
                  mail.addTo(rule.getEmail());
                  mail.setSubject(
                      c1.getPerson().getInitials()
                          + "'s charge"
                          + " referred to School Meeting in case #"
                          + c1.getTheCase().getCaseNumber());
                  mail.setFrom("DemSchoolTools <noreply@demschooltools.com>");
                  mail.setBodyText(
                      ""
                          + c1.getPerson().getDisplayName()
                          + " was charged with "
                          + c1.getRuleTitle()
                          + " and the charge was referred to School Meeting in case #"
                          + c1.getTheCase().getCaseNumber()
                          + ".\n\n"
                          + "For more information, view today's minutes:\n\n"
                          + org_config.people_url
                          + routes.Application.viewMeeting(c1.getTheCase().getMeeting().getId())
                              .toString()
                          + "\n\n");
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
  public Result setResolutionPlanComplete(
      Integer chargeId, Boolean complete, Http.Request request) {
    Charge c = Charge.findById(chargeId, Utils.getOrg(request));
    c.setRPComplete(complete);

    return ok();
  }

  @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
  public Result removeCharge(int id, Http.Request request) {
    Charge c = Charge.findById(id, Utils.getOrg(request));
    if (c == null) {
      return notFound();
    }

    c.delete();
    return ok();
  }

  @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
  public Result enterSchoolMeetingDecisions(Http.Request request) {
    return ok(
        enter_sm_decisions.render(
            Application.getActiveSchoolMeetingReferrals(Utils.getOrg(request)),
            request,
            mMessagesApi.preferred(request)));
  }

  @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
  public Result editSchoolMeetingDecision(Integer charge_id, Http.Request request) {
    Charge c = Charge.findById(charge_id, Utils.getOrg(request));

    if (!authToEdit(c.getSmDecisionDate(), request)) {
      return tooOldToEdit();
    }

    return ok(edit_sm_decision.render(c, request, mMessagesApi.preferred(request)));
  }

  @Secured.Auth(UserRole.ROLE_EDIT_7_DAY_JC)
  public Result saveSchoolMeetingDecisions(Http.Request request) {
    CachedPage.remove(CachedPage.JC_INDEX, Utils.getOrg(request));
    Map<String, String[]> form_data = request.body().asFormUrlEncoded();

    int charge_id = Integer.parseInt(form_data.get("charge_id")[0]);
    String decision = form_data.get("smDecision")[0];
    Date date = Application.getDateFromString(form_data.get("date")[0]);

    Charge c = Charge.findById(charge_id, Utils.getOrg(request));
    c.updateSchoolMeetingDecision(decision, date);
    c.save();

    return redirect(routes.ApplicationEditing.enterSchoolMeetingDecisions());
  }

  void onManualChange(Organization org) {
    CachedPage.remove(CachedPage.MANUAL_INDEX, org);
    try {
      Connection conn = mDatabase.getConnection();
      conn.prepareStatement("REFRESH MATERIALIZED VIEW entry_index WITH DATA").execute();
      conn.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
  public Result addChapter(Http.Request request) {
    Form<Chapter> form = mFormFactory.form(Chapter.class);
    return ok(edit_chapter.render(form, true, request, mMessagesApi.preferred(request)));
  }

  @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
  public Result editChapter(Integer id, Http.Request request) {
    Form<Chapter> filled_form =
        mFormFactory.form(Chapter.class).fill(Chapter.findById(id, Utils.getOrg(request)));
    return ok(edit_chapter.render(filled_form, false, request, mMessagesApi.preferred(request)));
  }

  @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
  public Result saveChapter(Http.Request request) {
    Form<Chapter> form = mFormFactory.form(Chapter.class).bindFromRequest(request);

    Chapter c;
    if (form.field("id").value().isPresent()) {
      c = Chapter.findById(Integer.parseInt(form.field("id").value().get()), Utils.getOrg(request));
      c.updateFromForm(form);
    } else {
      c = Chapter.create(form, Utils.getOrg(request));
    }

    onManualChange(Utils.getOrg(request));
    return redirect(routes.Application.viewChapter(c.getId()));
  }

  @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
  public Result addSection(Integer chapterId, Http.Request request) {
    Form<Section> form = mFormFactory.form(Section.class);
    Section section = new Section();
    section.setChapter(Chapter.findById(chapterId, Utils.getOrg(request)));
    form = form.fill(section);
    return ok(
        edit_section.render(
            form,
            Chapter.findById(chapterId, Utils.getOrg(request)),
            true,
            Chapter.all(Utils.getOrg(request)),
            request,
            mMessagesApi.preferred(request)));
  }

  @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
  public Result editSection(Integer id, Http.Request request) {
    Section existing_section = Section.findById(id, Utils.getOrg(request));
    Form<Section> filled_form = mFormFactory.form(Section.class).fill(existing_section);
    return ok(
        edit_section.render(
            filled_form,
            existing_section.getChapter(),
            false,
            Chapter.all(Utils.getOrg(request)),
            request,
            mMessagesApi.preferred(request)));
  }

  @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
  public Result saveSection(Http.Request request) {
    Form<Section> form = mFormFactory.form(Section.class).bindFromRequest(request);

    Section s;
    if (form.field("id").value().isPresent()) {
      s = Section.findById(Integer.parseInt(form.field("id").value().get()), Utils.getOrg(request));
      s.updateFromForm(form);
    } else {
      s = Section.create(form);
    }

    onManualChange(Utils.getOrg(request));
    return redirect(
        routes.Application.viewChapter(s.getChapter().getId()).url() + "#section_" + s.getId());
  }

  @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
  public Result addEntry(Integer sectionId, Http.Request request) {
    Form<Entry> form = mFormFactory.form(Entry.class);
    Entry entry = new Entry();
    entry.setSection(Section.findById(sectionId, Utils.getOrg(request)));
    form = form.fill(entry);
    return ok(
        edit_entry.render(
            form,
            Section.findById(sectionId, Utils.getOrg(request)),
            true,
            Chapter.all(Utils.getOrg(request)),
            request,
            mMessagesApi.preferred(request)));
  }

  @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
  public Result editEntry(Integer id, Http.Request request) {
    Entry e = Entry.findById(id, Utils.getOrg(request));
    Form<Entry> filled_form = mFormFactory.form(Entry.class).fill(e);
    return ok(
        edit_entry.render(
            filled_form,
            e.getSection(),
            false,
            Chapter.all(Utils.getOrg(request)),
            request,
            mMessagesApi.preferred(request)));
  }

  @Secured.Auth(UserRole.ROLE_EDIT_MANUAL)
  public Result saveEntry(Http.Request request) {
    Form<Entry> form = mFormFactory.form(Entry.class).bindFromRequest(request);

    Entry e;
    if (form.field("id").value().isPresent()) {
      e = Entry.findById(Integer.parseInt(form.field("id").value().get()), Utils.getOrg(request));
      e.updateFromForm(form);
    } else {
      e = Entry.create(form);
    }

    onManualChange(Utils.getOrg(request));
    return redirect(
        routes.Application.viewChapter(e.getSection().getChapter().getId()).url()
            + "#entry_"
            + e.getId());
  }
}
