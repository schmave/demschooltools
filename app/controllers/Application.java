package controllers;

import com.csvreader.CsvWriter;
import com.feth.play.module.pa.PlayAuthenticate;
import com.typesafe.config.Config;
import io.ebean.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import models.*;
import org.markdown4j.Markdown4jProcessor;
import org.mindrot.jbcrypt.BCrypt;
import org.xhtmlrenderer.pdf.ITextRenderer;
import play.api.libs.mailer.MailerClient;
import play.i18n.MessagesApi;
import play.libs.Files.TemporaryFile;
import play.libs.Json;
import play.mvc.*;
import views.html.*;

@Singleton
@Secured.Auth(UserRole.ROLE_VIEW_JC)
public class Application extends Controller {

  PlayAuthenticate mAuth;
  MailerClient mMailer;
  private final MessagesApi mMessagesApi;

  // TODO: Remove this once we change the main template to not use it
  static Application sInstance = null;

  @Inject
  public Application(
      final PlayAuthenticate auth, final MailerClient mailerClient, MessagesApi messagesApi) {
    mAuth = auth;
    sInstance = this;
    mMailer = mailerClient;
    mMessagesApi = messagesApi;
  }

  static Set<String> names;

  public static void loadNames(Organization org) {
    if (names != null) {
      return;
    }

    names = new HashSet<>();

    try {
      BufferedReader r =
          new BufferedReader(
              new InputStreamReader(Public.sEnvironment.resourceAsStream("names.txt")));
      while (true) {
        String line = r.readLine();
        if (line == null) {
          break;
        }

        String n = line.trim().toLowerCase();
        if (n.length() > 2) {
          // don't add 2-letter names
          names.add(n);
        }
      }
    } catch (IOException e) {
      System.out.println("ERROR loading names file");
      e.printStackTrace();
    }

    // Add first and display names of all people in JC db
    for (Person p : jcPeople(org)) {
      names.add(p.getFirstName().trim().toLowerCase());
      names.add(p.getDisplayName().trim().toLowerCase());
    }
  }

  public static String getRedactedFindings(Case theCase, Person keep_this_persons_name) {
    loadNames(theCase.getMeeting().getOrganization());

    String compositeFindings = generateCompositeFindingsFromChargeReferences(theCase);

    String keep_first_name = keep_this_persons_name.getFirstName().trim().toLowerCase();
    String keep_display_name = keep_this_persons_name.getDisplayName().trim().toLowerCase();

    String[] words = compositeFindings.split("\\b");
    Map<String, String> replacement_names = new HashMap<>();
    char next_replacement = 'A';

    for (String w : words) {
      w = w.toLowerCase();
      if (!w.equals(keep_first_name)
          && !w.equals(keep_display_name)
          && names.contains(w)
          && replacement_names.get(w) == null) {
        replacement_names.put(w, next_replacement + "___");

        next_replacement++;
      }
    }

    String new_findings = compositeFindings;
    for (String name : replacement_names.keySet()) {
      new_findings = new_findings.replaceAll("(?i)" + name, replacement_names.get(name));
    }

    return new_findings;
  }

  // We use this method while editing minutes. At this time the user has picked case references, but
  // is still working
  // on picking charge references, so we don't know which charges will be needed. As a result, we
  // will include
  // information on all charges from the referenced cases.
  public static String generateCompositeFindingsFromCaseReferences(Case theCase) {
    if (!theCase.getMeeting().getOrganization().getEnableCaseReferences()) {
      return theCase.getFindings();
    }
    return generateCompositeFindings(theCase, null, null);
  }

  // We use this method everywhere besides the edit minutes page. At this point the minutes have
  // been finalized and the
  // charge references have been selected. We don't need to include information about charges that
  // weren't referenced.
  public static String generateCompositeFindingsFromChargeReferences(Case theCase) {
    if (!theCase.getMeeting().getOrganization().getEnableCaseReferences()) {
      return theCase.getFindings();
    }
    ArrayList<Charge> relevant_charges = new ArrayList<>();
    for (Charge charge : theCase.charges) {
      charge.buildChargeReferenceChain(relevant_charges);
    }
    return generateCompositeFindings(theCase, relevant_charges, null);
  }

  private static String generateCompositeFindings(
      Case theCase, List<Charge> relevant_charges, List<Case> used_cases) {

    if (used_cases == null) {
      used_cases = new ArrayList<>();
    }
    used_cases.add(theCase);
    String result = "";

    theCase.referenced_cases.sort(Comparator.comparing(a -> a.getCaseNumber()));

    for (Case c : theCase.referenced_cases) {
      if (used_cases.contains(c)) {
        continue;
      }
      if (!isCaseRelevant(c, relevant_charges)) {
        continue;
      }
      ArrayList<Case> _used_cases = new ArrayList<>(used_cases);
      boolean hasRelevantReferences =
          c.referenced_cases.stream()
              .anyMatch(rc -> isCaseRelevant(rc, relevant_charges) && !_used_cases.contains(rc));
      if (!hasRelevantReferences) {
        if (result.isEmpty()) {
          result += "Per case ";
        } else {
          result += " Then per case ";
        }
        result += c.getCaseNumber() + ",";
      }
      if (!result.isEmpty()) {
        result += " ";
      }
      result += generateCompositeFindings(c, relevant_charges, used_cases);
      if (!result.endsWith(".")) {
        result += ".";
      }
      Map<String, List<Charge>> groups =
          groupChargesByRuleAndResolutionPlan(findRelevantCharges(c, relevant_charges));
      for (Map.Entry<String, List<Charge>> entry : groups.entrySet()) {
        List<Charge> group = entry.getValue();
        result += " " + group.get(0).getPerson().getDisplayName();
        if (group.size() == 1) {
          result += " was ";
        } else {
          if (group.size() > 2) {
            for (Charge ch : group.subList(1, group.size() - 1)) {
              result += ", " + ch.getPerson().getDisplayName();
            }
            result += ",";
          }
          result += " and " + group.get(group.size() - 1).getPerson().getDisplayName() + " were ";
        }
        result += "charged with " + group.get(0).getRuleTitle();
        String resolutionPlan = getResolutionPlanForCompositeFindings(group.get(0));
        if (resolutionPlan.isEmpty()) {
          result += ".";
        } else {
          if (group.get(0).getSmDecision() != null && !group.get(0).getSmDecision().isEmpty()) {
            result += " and School Meeting decided on";
          } else if (group.size() == 1) {
            result += " and was assigned";
          } else {
            result += " and were each assigned";
          }
          result +=
              " the "
                  + Utils.getOrgConfig(theCase.getMeeting().getOrganization()).str_res_plan
                  + " \""
                  + resolutionPlan;
          if (!result.endsWith(".")) {
            result += ".";
          }
          result += "\"";
        }
      }
    }
    if (!result.isEmpty()) {
      result += " Then per case " + theCase.getCaseNumber() + ", ";
    }
    if (theCase.getFindings().isEmpty()) {
      theCase.setFindings(
          "the "
              + Utils.getOrgConfig(theCase.getMeeting().getOrganization()).str_res_plan
              + " was not completed.");
    }
    result += theCase.getFindings();
    return result;
  }

  private static List<Charge> findRelevantCharges(Case c, List<Charge> relevant_charges) {

    List<Charge> charges =
        Charge.find
            .query()
            .fetch("person")
            .fetch("rule")
            .fetch("rule.section")
            .fetch("rule.section.chapter")
            .where()
            .eq("case_id", c.getId())
            .ne("person", null)
            .findList();

    return charges.stream()
        .filter(ch -> relevant_charges == null || relevant_charges.contains(ch))
        .collect(Collectors.toList());
  }

  private static Map<String, List<Charge>> groupChargesByRuleAndResolutionPlan(
      List<Charge> charges) {
    return charges.stream()
        .collect(
            Collectors.groupingBy(
                ch -> ch.getRuleTitle() + getResolutionPlanForCompositeFindings(ch)));
  }

  private static boolean isCaseRelevant(Case c, List<Charge> relevant_charges) {
    return c.charges.stream()
        .anyMatch(ch -> relevant_charges == null || relevant_charges.contains(ch));
  }

  private static String getResolutionPlanForCompositeFindings(Charge charge) {
    if (charge.getSmDecision() != null && !charge.getSmDecision().isEmpty()) {
      return charge.getSmDecision();
    }
    if (charge.getReferredToSm() && charge.getResolutionPlan().isEmpty()) {
      return "[Referred to School Meeting]";
    }
    return charge.getResolutionPlan();
  }

  public Result viewPassword(Http.Request request) {
    return ok(
        view_password.render(
            request.flash().get("notice").orElse(null), request, mMessagesApi.preferred(request)));
  }

  public Result editPassword(Http.Request request) {
    final Map<String, String[]> values = request.body().asFormUrlEncoded();

    String password = values.get("password")[0];
    String confirmPassword = values.get("confirmPassword")[0];

    Result result = redirect(routes.Application.viewPassword());

    if (!password.equals(confirmPassword)) {
      result.flashing("notice", "The two passwords you entered did not match");
    } else if (password.length() < 8) {
      result.flashing("notice", "Please choose a password that is at least 8 characters");
    } else {
      User u = Application.getCurrentUser(request);
      u.setHashedPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
      u.save();
      play.libs.mailer.Email mail = new play.libs.mailer.Email();
      mail.setSubject("DemSchoolTools password changed");
      mail.addTo(u.getEmail());
      mail.setFrom("DemSchoolTools <noreply@demschooltools.com>");
      mail.setBodyText(
          "Hi "
              + u.getName()
              + ",\n\nYour DemSchoolTools password was changed today ("
              + Application.formatDateTimeLong(Utils.getOrgConfig(Utils.getOrg(request)))
              + "). \n\nIf it was not you who changed it, please investigate what is going on! "
              + "Feel free to contact Evan (schmave@gmail.com) for help.");
      mMailer.send(mail);
      result.flashing("notice", "Your password was changed");
    }

    return result;
  }

  public static Date getDateFromString(String date_string) {
    if (!date_string.equals("")) {
      try {
        return new SimpleDateFormat("yyyy-MM-dd").parse(date_string);
      } catch (ParseException e) {
        return null;
      }
    } else {
      return null;
    }
  }

  public static List<Charge> getActiveSchoolMeetingReferrals(Organization org) {
    return Charge.find
        .query()
        .where()
        .eq("referredToSm", true)
        .isNull("smDecision")
        .eq("person.organization", org)
        .orderBy("id DESC")
        .findList();
  }

  public Result viewSchoolMeetingReferrals(Http.Request request) {
    return ok(
        view_sm_referrals.render(
            getActiveSchoolMeetingReferrals(Utils.getOrg(request)),
            request,
            mMessagesApi.preferred(request)));
  }

  public Result viewSchoolMeetingDecisions(Http.Request request) {
    List<Charge> the_charges =
        Charge.find
            .query()
            .fetch("rule")
            .fetch("rule.section")
            .fetch("rule.section.chapter")
            .fetch("person")
            .fetch("theCase")
            .where()
            .eq("referredToSm", true)
            .eq("person.organization", Utils.getOrg(request))
            .gt("smDecisionDate", ModelUtils.getStartOfYear())
            .isNotNull("smDecision")
            .orderBy("id DESC")
            .findList();
    return ok(view_sm_decisions.render(the_charges, request, mMessagesApi.preferred(request)));
  }

  public static List<Person> jcPeople(Organization org) {
    return peopleByTagType("showInJc", org);
  }

  public static List<Person> attendancePeople(Organization org) {
    return peopleByTagType("showInAttendance", org);
  }

  public static String attendancePeopleJson(Organization org) {
    List<Map<String, String>> result = new ArrayList<>();
    List<Person> people = attendancePeople(org);
    for (Person p : people) {
      HashMap<String, String> values = new HashMap<>();
      values.put("label", p.getDisplayName());
      values.put("id", "" + p.getPersonId());
      result.add(values);
    }
    return Json.stringify(Json.toJson(result));
  }

  private static List<Person> peopleByTagType(String tag_type, Organization org) {
    List<Tag> tags = Tag.find.query().where().eq(tag_type, true).eq("organization", org).findList();

    Set<Person> people = new HashSet<>();
    for (Tag tag : tags) {
      people.addAll(tag.people);
    }
    return new ArrayList<>(people);
  }

  public Result index(Http.Request request) {
    return ok(
        cached_page.render(
            new CachedPage(
                CachedPage.JC_INDEX, "JC database", "jc", "jc_home", Utils.getOrg(request)) {
              @Override
              String render() {
                List<Meeting> meetings =
                    Meeting.find
                        .query()
                        .fetch("cases")
                        .where()
                        .eq("organization", Utils.getOrg(request))
                        .orderBy("date DESC")
                        .findList();

                List<Tag> tags =
                    Tag.find
                        .query()
                        .where()
                        .eq("showInJc", true)
                        .eq("organization", Utils.getOrg(request))
                        .findList();

                Set<Person> peopleSet =
                    Person.find
                        .query()
                        .fetch("charges")
                        .fetch("charges.theCase")
                        .fetch("charges.theCase.meeting")
                        .fetch("cases_involved_in", FetchConfig.ofQuery())
                        .fetch("cases_involved_in.theCase", FetchConfig.ofQuery())
                        .fetch("cases_involved_in.theCase.meeting", FetchConfig.ofQuery())
                        .where()
                        .in("tags", tags)
                        .findSet();

                List<Person> people = new ArrayList<>(peopleSet);
                people.sort(Person.SORT_DISPLAY_NAME);

                List<Entry> entries =
                    Entry.find
                        .query()
                        .fetch("charges")
                        .fetch("charges.theCase")
                        .fetch("charges.theCase.meeting")
                        .fetch("section")
                        .fetch("section.chapter")
                        .where()
                        .eq("section.chapter.organization", Utils.getOrg(request))
                        .eq("section.chapter.deleted", false)
                        .eq("section.deleted", false)
                        .eq("deleted", false)
                        .findList();
                List<Entry> entries_with_charges = new ArrayList<>();
                for (Entry e : entries) {
                  if (e.getThisYearCharges().size() > 0) {
                    entries_with_charges.add(e);
                  }
                }

                entries_with_charges.sort(Entry.SORT_NUMBER);

                return jc_index
                    .render(
                        meetings,
                        people,
                        entries_with_charges,
                        request,
                        mMessagesApi.preferred(request))
                    .toString();
              }
            },
            request,
            mMessagesApi.preferred(request)));
  }

  public Result downloadCharges(Http.Request request) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Charset charset = StandardCharsets.UTF_8;
    CsvWriter writer = new CsvWriter(baos, ',', charset);

    final Organization org = Utils.getOrg(request);
    final OrgConfig org_config = Utils.getOrgConfig(org);

    writer.write("Name");
    writer.write("Age");
    writer.write("Gender");
    writer.write("Date of event");
    writer.write("Time of event");
    writer.write("Location of event");
    writer.write("Date of meeting");
    writer.write("Case #");
    writer.write(org_config.str_findings);
    writer.write("Rule");
    writer.write("Plea");
    writer.write(org_config.str_res_plan_cap);
    writer.write(org_config.str_res_plan_cap + " complete?");
    if (org_config.show_severity) {
      writer.write("Severity");
    }
    if (org_config.use_minor_referrals) {
      writer.write("Referred to");
    }
    writer.write("Referred to SM?");
    writer.write("SM decision");
    writer.write("SM decision date");
    writer.endRecord();

    List<Charge> charges =
        Charge.find
            .query()
            .fetch("theCase")
            .fetch("person")
            .fetch("rule")
            .fetch("theCase.meeting", FetchConfig.ofQuery())
            .where()
            .eq("person.organization", org)
            .ge("theCase.meeting.date", ModelUtils.getStartOfYear())
            .findList();
    for (Charge c : charges) {
      if (c.getPerson() != null) {
        writer.write(c.getPerson().getDisplayName());
      } else {
        writer.write("");
      }

      if (c.getPerson().getDob() != null) {
        writer.write("" + c.getPerson().calcAge());
      } else {
        writer.write("");
      }

      writer.write(c.getPerson().getGender());
      writer.write(
          yymmddDate(
              org_config,
              c.getTheCase().getDate() != null
                  ? c.getTheCase().getDate()
                  : c.getTheCase().getMeeting().getDate()));
      writer.write(c.getTheCase().getTime());
      writer.write(c.getTheCase().getLocation());
      writer.write(yymmddDate(org_config, c.getTheCase().getMeeting().getDate()));

      // Adding a space to the front of the case number prevents MS Excel
      // from misinterpreting it as a date.
      writer.write(" " + c.getTheCase().getCaseNumber(), true);
      writer.write(generateCompositeFindingsFromChargeReferences(c.getTheCase()));

      if (c.getRule() != null) {
        writer.write(c.getRule().getNumber() + " " + c.getRule().getTitle());
      } else {
        writer.write("");
      }
      writer.write(org_config.translatePlea(c.getPlea()));
      writer.write(c.getResolutionPlan());
      writer.write("" + c.getRpComplete());
      if (org_config.show_severity) {
        writer.write(c.getSeverity());
      }

      if (org_config.use_minor_referrals) {
        writer.write(c.getMinorReferralDestination());
      }
      writer.write("" + c.getReferredToSm());
      if (c.getSmDecision() != null) {
        writer.write(c.getSmDecision());
      } else {
        writer.write("");
      }
      if (c.getSmDecisionDate() != null) {
        writer.write(Application.yymmddDate(org_config, c.getSmDecisionDate()));
      } else {
        writer.write("");
      }

      writer.endRecord();
    }
    writer.close();

    // Adding the BOM here causes Excel 2010 on Windows to realize
    // that the file is Unicode-encoded.
    return ok("\ufeff" + new String(baos.toByteArray(), charset))
        .as("text/csv; charset=utf-8")
        .withHeader("Content-Disposition", "attachment; filename=All charges.csv");
  }

  public Result viewMeeting(int meeting_id, Http.Request request) {
    return ok(
        view_meeting.render(
            Meeting.findById(meeting_id, Utils.getOrg(request)),
            request,
            mMessagesApi.preferred(request)));
  }

  public Result printMeeting(int meeting_id, Http.Request request) throws Exception {
    return renderToPDF(
        view_meeting
            .render(
                Meeting.findById(meeting_id, Utils.getOrg(request)),
                request,
                mMessagesApi.preferred(request))
            .toString());
  }

  public Result editResolutionPlanList(Http.Request request) {
    final Organization org = Utils.getOrg(request);
    List<Integer> referenced_charge_ids = getReferencedChargeIds(org);
    List<Charge> active_rps = getActiveResolutionPlans(referenced_charge_ids, org);

    List<Charge> completed_rps =
        Charge.find
            .query()
            .fetch("theCase")
            .fetch("theCase.meeting")
            .fetch("person")
            .fetch("rule")
            .fetch("rule.section")
            .fetch("rule.section.chapter")
            .where()
            .eq("person.organization", Utils.getOrg(request))
            .isNotNull("person")
            .eq("rpComplete", true)
            .not(Expr.in("id", referenced_charge_ids))
            .orderBy("rp_complete_date DESC")
            .setMaxRows(25)
            .findList();

    List<Charge> nullified_rps =
        Charge.find
            .query()
            .fetch("theCase")
            .fetch("theCase.meeting")
            .fetch("person")
            .fetch("rule")
            .fetch("rule.section")
            .fetch("rule.section.chapter")
            .where()
            .eq("person.organization", Utils.getOrg(request))
            .isNotNull("person")
            .in("id", referenced_charge_ids)
            .orderBy("id DESC")
            .setMaxRows(10)
            .findList();

    List<Charge> all = new ArrayList<>(active_rps);
    all.addAll(completed_rps);
    all.addAll(nullified_rps);

    for (Charge charge : all) {
      Case c = charge.getTheCase();
      if (c.getCompositeFindings() == null) {
        c.setCompositeFindings(generateCompositeFindingsFromChargeReferences(c));
      }
    }

    return ok(edit_rp_list.render(
            active_rps, completed_rps, nullified_rps, request, mMessagesApi.preferred(request)))
        .withHeader("Cache-Control", "max-age=0, no-cache, no-store")
        .withHeader("Pragma", "no-cache");
  }

  private List<Integer> getReferencedChargeIds(Organization org) {
    return Charge.find
        .query()
        .where()
        .eq("person.organization", org)
        .isNotNull("referenced_charge_id")
        .select("referencedCharge")
        .findList()
        .stream()
        .map(c -> c.getReferencedCharge().getId())
        .collect(Collectors.toList());
  }

  private List<Charge> getActiveResolutionPlans(
      List<Integer> referenced_charge_ids, Organization org) {
    return Charge.find
        .query()
        .fetch("theCase")
        .fetch("theCase.meeting")
        .fetch("person")
        .fetch("rule")
        .fetch("rule.section")
        .fetch("rule.section.chapter")
        .where()
        .eq("person.organization", org)
        .or(Expr.ne("plea", "Not Guilty"), Expr.isNotNull("smDecision"))
        .isNotNull("person")
        .eq("rpComplete", false)
        .not(Expr.in("id", referenced_charge_ids))
        .orderBy("id DESC")
        .findList();
  }

  public Result viewSimpleResolutionPlans(Http.Request request) throws Exception {
    final Organization org = Utils.getOrg(request);
    List<Integer> referenced_charge_ids = getReferencedChargeIds(org);
    List<Charge> charges = getActiveResolutionPlans(referenced_charge_ids, org);

    HashMap<String, List<Charge>> groups = new HashMap<>();

    for (Charge charge : charges) {
      if (charge.getPerson() != null) {
        String name = charge.getPerson().getDisplayName();
        if (!groups.containsKey(name)) {
          List<Charge> list = new ArrayList<>();
          list.add(charge);
          groups.put(name, list);
        } else {
          groups.get(name).add(charge);
        }
      }
    }
    return renderToPDF(
        view_simple_rps.render(groups, request, mMessagesApi.preferred(request)).toString());
  }

  public Result viewMeetingResolutionPlans(int meeting_id, Http.Request request) {
    return ok(
        view_meeting_resolution_plans.render(
            Meeting.findById(meeting_id, Utils.getOrg(request)),
            request,
            mMessagesApi.preferred(request)));
  }

  public Result downloadMeetingResolutionPlans(int meeting_id, Http.Request request)
      throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Charset charset = StandardCharsets.UTF_8;
    CsvWriter writer = new CsvWriter(baos, ',', charset);

    Organization org = Utils.getOrg(request);
    writer.write("Person");
    writer.write("Case #");
    writer.write("Rule");
    writer.write(Utils.getOrgConfig(org).str_res_plan_cap);
    writer.endRecord();

    Meeting m = Meeting.findById(meeting_id, org);
    for (Case c : m.cases) {
      for (Charge charge : c.charges) {
        if (charge.displayInResolutionPlanList() && !charge.getReferredToSm()) {
          writer.write(charge.getPerson().getDisplayName());

          // In case it's needed in the future, adding a space to
          // the front of the case number prevents MS Excel from
          // misinterpreting it as a date.
          //
          // writer.write(" " + charge.getTheCase().getCaseNumber(),
          // true);

          writer.write(
              charge.getTheCase().getCaseNumber()
                  + " ("
                  + (charge.getSmDecisionDate() != null
                      ? Application.formatDayOfWeek(charge.getSmDecisionDate()) + "--SM"
                      : Application.formatDayOfWeek(charge.getTheCase().getMeeting().getDate()))
                  + ")");
          writer.write(charge.getRule().getTitle());
          writer.write(charge.getResolutionPlan());
          writer.endRecord();
        }
      }
    }
    writer.close();

    // Adding the BOM here causes Excel 2010 on Windows to realize
    // that the file is Unicode-encoded.
    return ok("\ufeff" + new String(baos.toByteArray(), charset))
        .as("text/csv; charset=utf-8")
        .withHeader(
            "Content-Disposition",
            "attachment; filename=" + Utils.getOrgConfig(org).str_res_plans + ".csv");
  }

  public Result viewManual(Http.Request request) {
    return ok(renderManualTOC(Utils.getOrg(request), request));
  }

  public Result viewManualChanges(String begin_date_string, Http.Request request) {
    Date begin_date;

    try {
      begin_date = new SimpleDateFormat("yyyy-M-d").parse(begin_date_string);
    } catch (ParseException e) {
      begin_date = new Date(new Date().getTime() - 1000 * 60 * 60 * 24 * 7);
      begin_date.setHours(0);
      begin_date.setMinutes(0);
    }

    List<ManualChange> changes =
        ManualChange.find
            .query()
            .where()
            .gt("dateEntered", begin_date)
            .eq("entry.section.chapter.organization", Utils.getOrg(request))
            .findList();

    changes.sort(ManualChange.SORT_NUM_DATE);

    return ok(
        view_manual_changes.render(
            forDateInput(begin_date), changes, request, mMessagesApi.preferred(request)));
  }

  public Result printManual(Http.Request request) {
    return ok(
        print_manual.render(
            Chapter.all(Utils.getOrg(request)), request, mMessagesApi.preferred(request)));
  }

  static Path print_temp_dir;

  public static void copyPrintingAssetsToTempDir() throws IOException {
    if (print_temp_dir == null) {
      print_temp_dir = Files.createTempDirectory("dst");
    }

    if (!Files.exists(print_temp_dir.resolve("stylesheets"))) {
      Files.createDirectory(print_temp_dir.resolve("stylesheets"));
    }

    String[] names =
        new String[] {
          "main.css",
        };

    for (String name : names) {
      Files.copy(
          Public.sEnvironment.resourceAsStream("public/stylesheets/" + name),
          print_temp_dir.resolve("stylesheets").resolve(name),
          StandardCopyOption.REPLACE_EXISTING);
    }
  }

  public static File prepareTempHTML(String orig_html) throws IOException {
    copyPrintingAssetsToTempDir();

    File html_file = Files.createTempFile(print_temp_dir, "chapter", ".xhtml").toFile();

    OutputStreamWriter writer =
        new OutputStreamWriter(Files.newOutputStream(html_file.toPath()), StandardCharsets.UTF_8);

    // XML 1.0 only allows the following characters
    // #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
    String xml10pattern =
        "[^" + "\u0009\r\n" + "\u0020-\uD7FF" + "\uE000-\uFFFD" + "\ud800\udc00-\udbff\udfff" + "]";

    orig_html = orig_html.replaceAll(xml10pattern, "");

    orig_html = orig_html.replaceAll("/assets/", "");
    // XHTML can't handle HTML entities without some extra incantations,
    // none of which I can get to work right now, so hence this ugliness.
    orig_html = orig_html.replaceAll("&ldquo;", "\"");
    orig_html = orig_html.replaceAll("&rdquo;", "\"");
    orig_html = orig_html.replaceAll("&ndash;", "\u2013");
    orig_html = orig_html.replaceAll("&mdash;", "\u2014");
    orig_html = orig_html.replaceAll("&nbsp;", " ");
    orig_html = orig_html.replaceAll("&hellip;", "\u2026");
    writer.write(orig_html);
    writer.close();

    return html_file;
  }

  public static Result renderToPDF(String orig_html) throws Exception {
    File html_file = null;
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ITextRenderer renderer = new ITextRenderer();

      html_file = prepareTempHTML(orig_html);
      renderer.setDocument(html_file);
      renderer.layout();
      renderer.createPDF(baos);

      return ok(baos.toByteArray()).as("application/pdf");
    } finally {
      if (html_file != null) {
        html_file.delete();
      }
    }
  }

  public static Result renderToPDF(List<String> orig_htmls) throws Exception {
    File html_file = null;
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ITextRenderer renderer = new ITextRenderer();

      boolean first = true;
      for (String orig_html : orig_htmls) {
        html_file = prepareTempHTML(orig_html);
        renderer.setDocument(html_file);
        renderer.layout();
        if (first) {
          renderer.createPDF(baos, false);
        } else {
          renderer.writeNextDocument();
        }
        html_file.delete();
        html_file = null;

        first = false;
      }

      renderer.finishPDF();
      return ok(baos.toByteArray()).as("application/pdf");
    } finally {
      if (html_file != null) {
        html_file.delete();
      }
    }
  }

  play.twirl.api.Html renderManualTOC(Organization org, Http.Request request) {
    return cached_page.render(
        new CachedPage(
            CachedPage.MANUAL_INDEX,
            Utils.getOrgConfig(org).str_manual_title,
            "manual",
            "toc",
            org) {
          @Override
          String render() {
            return view_manual
                .render(Chapter.all(org), request, mMessagesApi.preferred(request))
                .toString();
          }
        },
        request,
        mMessagesApi.preferred(request));
  }

  public Result printManualChapter(Integer id, Http.Request request) throws Exception {

    Organization org = Utils.getOrg(request);
    if (id == -1) {
      ArrayList<String> documents = new ArrayList<>();
      // render TOC
      documents.add(renderManualTOC(org, request).toString());
      // then render all chapters
      for (Chapter chapter : Chapter.all(org)) {
        documents.add(
            view_chapter.render(chapter, request, mMessagesApi.preferred(request)).toString());
      }
      return renderToPDF(documents);
    } else {
      if (id == -2) {
        return renderToPDF(renderManualTOC(org, request).toString());

      } else {
        Chapter chapter = Chapter.findById(id, org);
        return renderToPDF(
            view_chapter.render(chapter, request, mMessagesApi.preferred(request)).toString());
      }
    }
  }

  public Result viewChapter(Integer id, Http.Request request) {
    Chapter c =
        Chapter.find
            .query()
            .fetch("sections", FetchConfig.ofQuery())
            .fetch("sections.entries", FetchConfig.ofQuery())
            .where()
            .eq("organization", Utils.getOrg(request))
            .eq("id", id)
            .findOne();

    return ok(view_chapter.render(c, request, mMessagesApi.preferred(request)));
  }

  public Result searchManual(String searchString, Http.Request request) {
    Map<String, Object> scopes = new HashMap<>();
    scopes.put("searchString", searchString);

    String sql =
        "SELECT ei.id "
            + "FROM entry_index ei "
            + "WHERE ei.document @@ plainto_tsquery(:searchString) "
            + "and ei.organization_id=:orgId "
            + "ORDER BY ts_rank(ei.document, plainto_tsquery(:searchString), 0) DESC";

    OrgConfig orgConfig = Utils.getOrgConfig(Utils.getOrg(request));
    SqlQuery sqlQuery = DB.sqlQuery(sql);
    sqlQuery.setParameter("orgId", orgConfig.org.getId());
    sqlQuery.setParameter("searchString", searchString);

    List<SqlRow> result = sqlQuery.findList();
    List<Integer> entryIds = new ArrayList<>();
    for (SqlRow sqlRow : result) {
      entryIds.add(sqlRow.getInteger("id"));
    }

    Map<Integer, String> entryToHeadline = new HashMap<>();
    if (entryIds.size() > 0) {
      sql =
          "SELECT e.id, ts_headline(e.content, plainto_tsquery(:searchString), 'MaxFragments=5') as"
              + " headline FROM entry e WHERE e.id IN (:entryIds)";
      sqlQuery = DB.sqlQuery(sql);
      sqlQuery.setParameter("searchString", searchString);
      sqlQuery.setParameter("entryIds", entryIds);
      List<SqlRow> headlines = sqlQuery.findList();
      for (SqlRow headline : headlines) {
        entryToHeadline.put(headline.getInteger("id"), headline.getString("headline"));
      }
    }

    Map<Integer, Entry> entries =
        Entry.find
            .query()
            .fetch("section", FetchConfig.ofQuery())
            .fetch("section.chapter", FetchConfig.ofQuery())
            .where()
            .in("id", entryIds)
            .findMap();

    ArrayList<Map<String, Object>> entriesList = new ArrayList<>();
    for (SqlRow sqlRow : result) {
      Map<String, Object> entryInfo = new HashMap<>();
      int entryId = sqlRow.getInteger("id");
      entryInfo.put("entry", entries.get(entryId));
      entryInfo.put("headline", entryToHeadline.get(entryId));
      entriesList.add(entryInfo);
    }
    scopes.put("entries", entriesList);

    return ok(
        main_with_mustache.render(
            "Manual Search: " + searchString,
            "manual",
            "",
            "manual_search.html",
            scopes,
            request,
            mMessagesApi.preferred(request)));
  }

  static List<Charge> getLastWeekCharges(Person p) {
    List<Charge> last_week_charges = new ArrayList<>();

    Date now = new Date();

    Collections.sort(p.charges);
    Collections.reverse(p.charges);
    for (Charge c : p.charges) {
      // Include if <= 7 days ago
      if (now.getTime() - c.getTheCase().getMeeting().getDate().getTime()
          < 1000 * 60 * 60 * 24 * 7.5) {
        last_week_charges.add(c);
      }
    }

    return last_week_charges;
  }

  static Collection<String> getRecentResolutionPlans(Entry r) {
    Set<String> rps = new HashSet<>();

    Collections.sort(r.charges);
    Collections.reverse(r.charges);

    for (Charge c : r.charges) {
      if (!c.getResolutionPlan().equalsIgnoreCase("warning") && !c.getResolutionPlan().equals("")) {
        rps.add(c.getResolutionPlan());
      }
      if (rps.size() > 9) {
        break;
      }
    }

    return rps;
  }

  public Result getPersonRuleHistory(Integer personId, Integer ruleId, Http.Request request) {
    Person p = Person.findByIdWithJCData(personId, Utils.getOrg(request));
    Entry r = Entry.findById(ruleId, Utils.getOrg(request));

    PersonHistory history = new PersonHistory(p, false, ModelUtils.getStartOfYear(), null);
    PersonHistory.Record rule_record = null;
    for (PersonHistory.Record record : history.rule_records) {
      if (record.rule != null && record.rule.equals(r)) {
        rule_record = record;
        break;
      }
    }

    return ok(
        person_rule_history.render(
            p,
            r,
            rule_record,
            history.charges_by_rule.get(r),
            history,
            request,
            mMessagesApi.preferred(request)));
  }

  public Result getPersonHistory(Integer id, Http.Request request) {
    Person p = Person.findByIdWithJCData(id, Utils.getOrg(request));
    return ok(
        person_history.render(
            p,
            new PersonHistory(p, false, ModelUtils.getStartOfYear(), null),
            getLastWeekCharges(p),
            false,
            request,
            mMessagesApi.preferred(request)));
  }

  private static Date restrictStartDate(Date d, User current_user) {
    Date soy = ModelUtils.getStartOfYear();
    long soy_millis = soy.getTime();
    long d_millis = d.getTime();
    if ((soy_millis - d_millis > 1000 * 60) && current_user == null) {
      return soy;
    }
    return d;
  }

  Result addRestrictStartDateMessage(Result result, OrgConfig orgConfig) {
    return result.flashing(
        "notice",
        "You must be logged in to view info prior to "
            + Application.yymmddDate(orgConfig, ModelUtils.getStartOfYear())
            + ".");
  }

  public Result viewPersonHistory(
      Integer id,
      Boolean redact_names,
      String start_date_str,
      String end_date_str,
      Http.Request request) {
    Date start_date = ModelUtils.getStartOfYear();
    Date end_date = null;

    try {
      start_date = new SimpleDateFormat("yyyy-M-d").parse(start_date_str);
      end_date = new SimpleDateFormat("yyyy-M-d").parse(end_date_str);
    } catch (ParseException e) {
    }

    Date restricted_start_date = restrictStartDate(start_date, getCurrentUser(request));
    Person p = Person.findByIdWithJCData(id, Utils.getOrg(request));
    Result result =
        ok(
            view_person_history.render(
                p,
                new PersonHistory(p, true, start_date, end_date),
                getLastWeekCharges(p),
                redact_names,
                request,
                mMessagesApi.preferred(request)));

    if (restricted_start_date != start_date) {
      result = addRestrictStartDateMessage(result, Utils.getOrgConfig(Utils.getOrg(request)));
    }
    return result;
  }

  public Result getRuleHistory(Integer id, Http.Request request) {
    Entry r = Entry.findByIdWithJCData(id, Utils.getOrg(request));
    return ok(
        rule_history.render(
            r,
            new RuleHistory(r, false, ModelUtils.getStartOfYear(), null),
            getRecentResolutionPlans(r),
            request,
            mMessagesApi.preferred(request)));
  }

  public Result viewRuleHistory(
      Integer id, String start_date_str, String end_date_str, Http.Request request) {
    Date start_date = ModelUtils.getStartOfYear();
    Date end_date = null;

    try {
      start_date = new SimpleDateFormat("yyyy-M-d").parse(start_date_str);
      end_date = new SimpleDateFormat("yyyy-M-d").parse(end_date_str);
    } catch (ParseException e) {
    }

    Date restricted_start_date = restrictStartDate(start_date, getCurrentUser(request));

    Entry r = Entry.findByIdWithJCData(id, Utils.getOrg(request));
    Result result =
        ok(
            view_rule_history.render(
                r,
                new RuleHistory(r, true, start_date, end_date),
                getRecentResolutionPlans(r),
                request,
                mMessagesApi.preferred(request)));

    if (restricted_start_date != start_date) {
      result = addRestrictStartDateMessage(result, Utils.getOrgConfig(Utils.getOrg(request)));
    }
    return result;
  }

  public Result viewPersonsWriteups(Integer id, Http.Request request) {
    Person p = Person.findByIdWithJCData(id, Utils.getOrg(request));

    List<Case> cases_written_up = new ArrayList<>(p.getThisYearCasesWrittenUp());

    Collections.sort(cases_written_up);
    Collections.reverse(cases_written_up);

    return ok(
        view_persons_writeups.render(
            p, cases_written_up, request, mMessagesApi.preferred(request)));
  }

  public Result thisWeekReport(Http.Request request) {
    return viewWeeklyReport("", request);
  }

  public Result printWeeklyMinutes(String date_string, Http.Request request) throws Exception {
    Calendar start_date = new GregorianCalendar();
    try {
      Date parsed_date = new SimpleDateFormat("yyyy-M-d").parse(date_string);
      if (parsed_date != null) {
        start_date.setTime(parsed_date);
      }
    } catch (ParseException e) {
      System.out.println("Failed to parse given date (" + date_string + "), using current");
    }

    Calendar end_date = (Calendar) start_date.clone();
    end_date.add(GregorianCalendar.DATE, 6);

    List<Meeting> meetings =
        Meeting.find
            .query()
            .where()
            .eq("organization", Utils.getOrg(request))
            .le("date", end_date.getTime())
            .ge("date", start_date.getTime())
            .findList();

    return renderToPDF(
        multi_meetings.render(meetings, request, mMessagesApi.preferred(request)).toString());
  }

  public Result viewWeeklyReport(String date_string, Http.Request request) {
    Calendar start_date = Utils.parseDateOrNow(date_string);
    Organization org = Utils.getOrg(request);
    Utils.adjustToPreviousDay(start_date, org.getJcResetDay() + 1);

    Calendar end_date = (Calendar) start_date.clone();
    end_date.add(GregorianCalendar.DATE, 6);

    List<Charge> all_charges =
        Charge.find
            .query()
            .fetch("theCase")
            .fetch("person")
            .fetch("rule")
            .fetch("theCase.meeting", FetchConfig.ofQuery())
            .where()
            .eq("person.organization", org)
            .ge("theCase.meeting.date", ModelUtils.getStartOfYear(start_date.getTime()))
            .findList();
    WeeklyStats result = new WeeklyStats();
    result.rule_counts = new TreeMap<>();
    result.person_counts = new TreeMap<>();

    for (Charge c : all_charges) {
      long case_millis = c.getTheCase().getMeeting().getDate().getTime();
      long diff = end_date.getTime().getTime() - case_millis;

      if (c.getRule() != null && c.getPerson() != null) {
        if (diff >= 0 && diff < 6.5 * 24 * 60 * 60 * 1000) {
          result.rule_counts.put(c.getRule(), 1 + result.rule_counts.getOrDefault(c.getRule(), 0));
          result.person_counts.put(
              c.getPerson(),
              result
                  .person_counts
                  .getOrDefault(c.getPerson(), new WeeklyStats.PersonCounts())
                  .addThisPeriod());
          result.num_charges++;
        }
        if (diff >= 0 && diff < 27.5 * 24 * 60 * 60 * 1000) {
          result.person_counts.put(
              c.getPerson(),
              result
                  .person_counts
                  .getOrDefault(c.getPerson(), new WeeklyStats.PersonCounts())
                  .addLast28Days());
        }
        if (diff >= 0) {
          result.person_counts.put(
              c.getPerson(),
              result
                  .person_counts
                  .getOrDefault(c.getPerson(), new WeeklyStats.PersonCounts())
                  .addAllTime());
        }
      }
    }

    List<Case> all_cases = new ArrayList<>();

    List<Meeting> meetings =
        Meeting.find
            .query()
            .where()
            .eq("organization", org)
            .le("date", end_date.getTime())
            .ge("date", start_date.getTime())
            .findList();

    for (Meeting m : meetings) {
      for (Case c : m.cases) {
        result.num_cases++;

        all_cases.add(c);
      }
    }

    result.uncharged_people = jcPeople(org);
    for (Map.Entry<Person, WeeklyStats.PersonCounts> entry : result.person_counts.entrySet()) {
      if (entry.getValue().this_period > 0) {
        result.uncharged_people.remove(entry.getKey());
      }
    }

    ArrayList<String> referral_destinations = new ArrayList<>();
    for (Case c : all_cases) {
      for (Charge ch : c.charges) {
        if (!ch.getMinorReferralDestination().equals("")
            && !referral_destinations.contains(ch.getMinorReferralDestination())) {
          referral_destinations.add(ch.getMinorReferralDestination());
        }
      }
    }

    return ok(
        jc_weekly_report.render(
            start_date.getTime(),
            end_date.getTime(),
            result,
            all_cases,
            referral_destinations,
            request,
            mMessagesApi.preferred(request)));
  }

  public static String jcPeople(String term, Organization org) {
    List<Person> people = jcPeople(org);
    people.sort(Person.SORT_DISPLAY_NAME);

    term = term.toLowerCase();

    List<Map<String, String>> result = new ArrayList<>();
    for (Person p : people) {
      if (p.searchStringMatches(term)) {
        HashMap<String, String> values = new HashMap<>();
        values.put("label", p.getDisplayName());
        values.put("id", "" + p.getPersonId());
        result.add(values);
      }
    }

    return Json.stringify(Json.toJson(result));
  }

  public static String jsonRules(Boolean includeBreakingResPlan, Organization org) {
    ExpressionList<Entry> expr = Entry.find.query().where().eq("section.chapter.organization", org);
    if (!includeBreakingResPlan) {
      expr = expr.eq("isBreakingResPlan", false);
    }
    List<Entry> rules =
        expr.eq("deleted", false)
            .eq("section.deleted", false)
            .eq("section.chapter.deleted", false)
            .orderBy("title ASC")
            .findList();

    List<Map<String, String>> result = new ArrayList<>();
    for (Entry r : rules) {
      HashMap<String, String> values = new HashMap<>();
      values.put("label", r.getNumber() + " " + r.getTitle());
      values.put("id", "" + r.getId());
      result.add(values);
    }

    return Json.stringify(Json.toJson(result));
  }

  public static String jsonCases(String term, Organization org) {
    term = term.toLowerCase();

    List<Case> cases =
        Case.find
            .query()
            .where()
            .eq("meeting.organization", org)
            .ge("meeting.date", ModelUtils.getStartOfYear())
            .like("caseNumber", term + "%")
            .orderBy("caseNumber ASC")
            .findList();

    List<Map<String, String>> result = new ArrayList<>();
    for (Case c : cases) {
      HashMap<String, String> values = new HashMap<>();
      values.put("label", c.getCaseNumber());
      values.put("id", "" + c.getId());
      result.add(values);
    }

    return Json.stringify(Json.toJson(result));
  }

  public static String jsonBreakingResPlanEntry(Organization org) {
    return Utils.toJson(Entry.findBreakingResPlanEntry(org));
  }

  public Result getLastRp(Integer personId, Integer ruleId, Http.Request request) {
    Date now = new Date();

    // Look up person using findById to guarantee that the current user
    // has access to that organization.
    Person p = Person.findById(personId, Utils.getOrg(request));
    List<Charge> charges =
        Charge.find
            .query()
            .where()
            .eq("person", p)
            .eq("rule_id", ruleId)
            .lt("theCase.meeting.date", now)
            .ge("theCase.meeting.date", ModelUtils.getStartOfYear())
            .orderBy("theCase.meeting.date DESC")
            .findList();

    if (charges.size() > 0) {
      Charge c = charges.get(0);
      return ok(last_rp.render(charges.size(), c, request, mMessagesApi.preferred(request)));
    }

    return ok("No previous charge.");
  }

  public static Date addWeek(Date d, int numWeeks) {
    return new Date(d.getTime() + numWeeks * 7 * 24 * 60 * 60 * 1000);
  }

  public static String formatDayOfWeek(Date d) {
    return new SimpleDateFormat("EE").format(d);
  }

  public static String formatDateShort(OrgConfig orgConfig, Date d) {
    if (orgConfig.euro_dates) {
      return new SimpleDateFormat("dd/MM").format(d);
    }
    return new SimpleDateFormat("MM/dd").format(d);
  }

  public static String formatDateMdy(OrgConfig orgConfig, Date d) {
    if (orgConfig.euro_dates) {
      return new SimpleDateFormat("dd/MM/yyyy").format(d);
    }
    return new SimpleDateFormat("MM/dd/yyyy").format(d);
  }

  public static String formatMeetingDate(Date d) {
    return new SimpleDateFormat("EE--MMMM dd, yyyy").format(d);
  }

  public static String forDateInput(Date d) {
    return new SimpleDateFormat("yyyy-MM-dd").format(d);
  }

  public static String yymmddDate(OrgConfig orgConfig, Date d) {
    if (orgConfig.euro_dates) {
      return new SimpleDateFormat("dd-M-yyyy").format(d);
    }
    return new SimpleDateFormat("yyyy-M-dd").format(d);
  }

  public static String yymmddDate(OrgConfig orgConfig) {
    return yymmddDate(orgConfig, new Date());
  }

  public static String formatDateTimeLong(OrgConfig orgConfig) {
    return new SimpleDateFormat("EEEE, MMMM d, h:mm a").format(Utils.localNow(orgConfig));
  }

  public static Optional<String> currentUsername(Http.RequestHeader request) {
    return request.attrs().getOptional(Security.USERNAME);
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static boolean isUserEditor(Optional<String> username) {
    return username.isPresent() && username.get().contains("@");
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static boolean isCurrentUserLoggedIn(Optional<String> currentUsername) {
    return isUserEditor(currentUsername);
  }

  public static String getRemoteIp(Http.Request request) {
    Config conf = Public.sConfig.getConfig("school_crm");

    if (conf.getBoolean("heroku_ips")) {
      Optional<String> header = request.header("X-Forwarded-For");
      if (!header.isPresent()) {
        return "unknown-ip";
      }
      String[] splits = header.get().split("[, ]");
      return splits[splits.length - 1];
    } else {
      return request.remoteAddress();
    }
  }

  public static User getCurrentUser(Http.RequestHeader request) {
    return User.findByAuthUserIdentity(sInstance.mAuth.getUser(request.session()));
  }

  public static String markdown(String input) {
    if (input == null) {
      return "";
    }
    try {
      return new Markdown4jProcessor().process(input);
    } catch (IOException e) {
      return e + "<br><br>" + input;
    }
  }

  public Result renderMarkdown(Http.Request request) {
    Map<String, String[]> form_data = request.body().asFormUrlEncoded();

    if (form_data == null || form_data.get("markdown") == null) {
      return ok("");
    }

    String markdown = form_data.get("markdown")[0];
    return ok(markdown(markdown));
  }

  @Secured.Auth(UserRole.ROLE_ALL_ACCESS)
  public Result fileSharing(Http.Request request) {
    Map<String, Object> scopes = new HashMap<>();

    ArrayList<String> existingFiles = new ArrayList<>();
    File[] files = getSharedFileDirectory(Utils.getOrg(request)).listFiles();
    for (File f : files) {
      existingFiles.add(f.getName());
    }

    scopes.put("existing_files", existingFiles);

    scopes.put("printerEmail", Utils.getOrg(request).getPrinterEmail());
    return ok(
        main_with_mustache.render(
            "File Sharing config",
            "misc",
            "file_sharing",
            "file_sharing.html",
            scopes,
            request,
            mMessagesApi.preferred(request)));
  }

  @Secured.Auth(UserRole.ROLE_ALL_ACCESS)
  public Result saveFileSharingSettings(Http.Request request) {
    final Map<String, String[]> values = request.body().asFormUrlEncoded();

    if (values.containsKey("printerEmail")) {
      Organization organization = Utils.getOrg(request);
      String email = values.get("printerEmail")[0];
      organization.setPrinterEmail(email);
      organization.save();
    }

    return redirect(routes.Application.fileSharing());
  }

  @Secured.Auth(UserRole.ROLE_ALL_ACCESS)
  public Result uploadFileShare(Http.Request request) throws IOException {
    Http.MultipartFormData<TemporaryFile> body = request.body().asMultipartFormData();
    Http.MultipartFormData.FilePart<TemporaryFile> pdf = body.getFile("pdf_upload");
    if (pdf != null) {
      String fileName = pdf.getFilename();
      if (!fileName.equals("")) {
        File outputFile = new File(getSharedFileDirectory(Utils.getOrg(request)), fileName);
        pdf.getRef().copyTo(outputFile);
      }
    }
    return redirect(routes.Application.fileSharing());
  }

  private static File getSharedFileDirectory(Organization org) {
    File result =
        new File(
            Public.sConfig.getConfig("school_crm").getString("shared_files_path"),
            "" + org.getId());
    if (!result.exists()) {
      result.mkdir();
    }
    return result;
  }

  @Secured.Auth(UserRole.ROLE_ALL_ACCESS)
  public Result deleteFile(Http.Request request) {
    final Map<String, String[]> values = request.body().asFormUrlEncoded();

    if (values.containsKey("filename")) {
      File the_file =
          new File(getSharedFileDirectory(Utils.getOrg(request)), values.get("filename")[0]);
      if (the_file.exists()) {
        the_file.delete();
      }
    }

    return redirect(routes.Application.fileSharing());
  }

  public Result viewFiles(Http.Request request) {
    Map<String, Object> scopes = new HashMap<>();

    ArrayList<String> existingFiles = new ArrayList<>();
    final Organization org = Utils.getOrg(request);
    File[] files = getSharedFileDirectory(org).listFiles();
    for (File f : files) {
      existingFiles.add(f.getName());
    }

    scopes.put("existing_files", existingFiles);

    scopes.put("printerEmail", org.getPrinterEmail());
    return ok(
        main_with_mustache.render(
            "DemSchoolTools shared files",
            "misc",
            "view_files",
            "view_files.html",
            scopes,
            request,
            mMessagesApi.preferred(request)));
  }

  public Result emailFile(Http.Request request) {
    final Map<String, String[]> values = request.body().asFormUrlEncoded();

    if (values.containsKey("filename")) {
      File the_file =
          new File(getSharedFileDirectory(Utils.getOrg(request)), values.get("filename")[0]);
      if (the_file.exists()) {
        play.libs.mailer.Email mail = new play.libs.mailer.Email();
        mail.setSubject("Print from DemSchoolTools");
        mail.addTo(Utils.getOrg(request).getPrinterEmail());
        mail.setFrom("DemSchoolTools <noreply@demschooltools.com>");
        mail.addAttachment(the_file.getName(), the_file);
        mMailer.send(mail);
      }
    }

    return redirect(routes.Application.viewFiles());
  }

  public Result sendFeedbackEmail(Http.Request request) {
    final Map<String, String[]> values = request.body().asFormUrlEncoded();

    play.libs.mailer.Email mail = new play.libs.mailer.Email();

    String source = "a DemSchoolTools user";
    if (values.containsKey("name")) {
      source = values.get("name")[0];
      if (source.length() > 30) {
        source = source.substring(0, 30);
      }
    }
    mail.setSubject("DST Feedback from " + source);
    mail.addTo("schmave@gmail.com");
    mail.setFrom("DemSchoolTools <noreply@demschooltools.com>");

    StringBuilder body = new StringBuilder();
    if (values.containsKey("message")) {
      body.append(values.get("message")[0]).append("\n---\n");
    }
    if (values.containsKey("name")) {
      body.append(values.get("name")[0]).append("\n");
    }
    if (values.containsKey("email")) {
      body.append(values.get("email")[0]).append("\n");
    }

    try {
      body.append("\n\n=========================\n");
      body.append(Utils.toJson(request.getHeaders()));
    } catch (Exception e) {
      e.printStackTrace();
    }

    mail.setBodyText(body.toString());
    mMailer.send(mail);

    return ok();
  }

  public static void copyFileUsingStream(File source, File dest) throws IOException {
    try (InputStream is = Files.newInputStream(source.toPath());
        OutputStream os = Files.newOutputStream(dest.toPath())) {
      byte[] buffer = new byte[1024];
      int length;
      while ((length = is.read(buffer)) > 0) {
        os.write(buffer, 0, length);
      }
    }
  }
}
