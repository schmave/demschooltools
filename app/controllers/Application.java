package controllers;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.*;
import javax.inject.Singleton;

import org.markdown4j.Markdown4jProcessor;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.FetchConfig;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;
import com.csvreader.CsvWriter;
import com.feth.play.module.pa.PlayAuthenticate;
import com.google.inject.Inject;

import models.*;

import play.*;
import play.libs.Json;
import play.mvc.*;
import play.mvc.Http.Context;

@Singleton
@With(DumpOnError.class)
@Secured.Auth(UserRole.ROLE_VIEW_JC)
public class Application extends Controller {

    PlayAuthenticate mAuth;

    // TODO: Remove this once we change the main template to not use it
    static Application sInstance = null;

    @Inject
    public Application(final PlayAuthenticate auth) {
        mAuth = auth;
        sInstance = this;
    }

    public static Date getDateFromString(String date_string) {
        if (!date_string.equals("")) {
            try
            {
                return new SimpleDateFormat("yyyy-MM-dd").parse(date_string);
            } catch (ParseException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    public static List<Charge> getActiveSchoolMeetingReferrals() {
        return Charge.find.where()
            .eq("referred_to_sm", true)
            .eq("sm_decision", null)
            .eq("person.organization", Organization.getByHost())
            .orderBy("id DESC").findList();
    }

    public Result viewSchoolMeetingReferrals() {
        return ok(views.html.view_sm_referrals.render(
            getActiveSchoolMeetingReferrals()));
    }

    public Result viewSchoolMeetingDecisions() {
        List<Charge> the_charges =
            Charge.find
                .fetch("rule")
                .fetch("rule.section")
                .fetch("rule.section.chapter")
                .fetch("person")
                .fetch("the_case")
                .where()
                .eq("referred_to_sm", true)
                .eq("person.organization", Organization.getByHost())
                .gt("sm_decision_date", Application.getStartOfYear())
                .isNotNull("sm_decision")
                .orderBy("id DESC").findList();
        return ok(views.html.view_sm_decisions.render(the_charges));
    }

    public static List<Person> allPeople() {
        List<Tag> tags = Tag.find.where()
            .eq("show_in_jc", true)
            .eq("organization", Organization.getByHost())
            .findList();

        Set<Person> people = new HashSet<>();
        for (Tag tag : tags) {
            people.addAll(tag.people);
        }
        return new ArrayList<>(people);
    }

    public Result index() {
        return ok(views.html.cached_page.render(
            new CachedPage(CachedPage.JC_INDEX,
                "JC database",
                "jc",
                "jc_home") {
                @Override
                String render() {
        List<Meeting> meetings = Meeting.find
            .fetch("cases")
            .where().eq("organization", Organization.getByHost())
            .orderBy("date DESC").findList();

        List<Tag> tags = Tag.find.where()
            .eq("show_in_jc", true)
            .eq("organization", Organization.getByHost())
            .findList();

        Set<Person> peopleSet = Person.find
            .fetch("charges")
            .fetch("charges.the_case")
            .fetch("charges.the_case.meeting")
            .fetch("cases_involved_in", new FetchConfig().query())
            .fetch("cases_involved_in.the_case", new FetchConfig().query())
            .fetch("cases_involved_in.the_case.meeting", new FetchConfig().query())
            .where()
            .in("tags", tags)
            .findSet();

        List<Person> people = new ArrayList<>(peopleSet);
        Collections.sort(people, Person.SORT_DISPLAY_NAME);

        List<Entry> entries = Entry.find
            .fetch("charges")
            .fetch("charges.the_case")
            .fetch("charges.the_case.meeting")
            .fetch("section")
            .fetch("section.chapter")
            .where().eq("section.chapter.organization", Organization.getByHost())
            .eq("section.chapter.deleted", false)
            .eq("section.deleted", false)
            .eq("deleted", false)
            .findList();
        List<Entry> entries_with_charges = new ArrayList<Entry>();
        for (Entry e : entries) {
            if (e.getThisYearCharges().size() > 0) {
                entries_with_charges.add(e);
            }
        }

        Collections.sort(entries_with_charges, Entry.SORT_NUMBER);

        return views.html.jc_index.render(meetings, people,
            entries_with_charges).toString();
                }}));
    }

    public Result downloadCharges() throws IOException {
        response().setHeader("Content-Type", "text/csv; charset=utf-8");
        response().setHeader("Content-Disposition",
            "attachment; filename=All charges.csv");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Charset charset = Charset.forName("UTF-8");
        CsvWriter writer = new CsvWriter(baos, ',', charset);

        writer.write("Name");
        writer.write("Age");
        writer.write("Gender");
        writer.write("Date of event");
        writer.write("Time of event");
        writer.write("Location of event");
        writer.write("Date of meeting");
        writer.write("Case #");
        writer.write(OrgConfig.get().str_findings);
        writer.write("Rule");
        writer.write("Plea");
        writer.write(OrgConfig.get().str_res_plan_cap);
        writer.write(OrgConfig.get().str_res_plan_cap + " complete?");
        if (OrgConfig.get().show_severity) {
            writer.write("Severity");
        }
        if (OrgConfig.get().use_minor_referrals) {
            writer.write("Referred to");
        }
        writer.write("Referred to SM?");
        writer.write("SM decision");
        writer.write("SM decision date");
        writer.endRecord();

        List<Charge> charges = Charge.find
            .fetch("the_case")
            .fetch("person")
            .fetch("rule")
            .fetch("the_case.meeting", new FetchConfig().query())
            .where().eq("person.organization", Organization.getByHost())
                .ge("the_case.meeting.date", getStartOfYear())
            .findList();
        for (Charge c : charges) {
            if (c.person != null) {
                writer.write(c.person.getDisplayName());
            } else {
                writer.write("");
            }

            if (c.person.dob != null) {
                writer.write("" + CRM.calcAge(c.person));
            } else {
                writer.write("");
            }

            writer.write(c.person.gender);
            writer.write(yymmddDate(
                c.the_case.date != null ? c.the_case.date : c.the_case.meeting.date));
            writer.write(c.the_case.time);
            writer.write(c.the_case.location);
            writer.write(yymmddDate(c.the_case.meeting.date));

            // Adding a space to the front of the case number prevents MS Excel
            // from misinterpreting it as a date.
            writer.write(" " + c.the_case.case_number, true);
            writer.write(c.the_case.generateCompositeFindingsFromChargeReferences());

            if (c.rule != null) {
                writer.write(c.rule.getNumber() + " " + c.rule.title);
            } else {
                writer.write("");
            }
            writer.write(c.plea);
            writer.write(c.resolution_plan);
            writer.write("" + c.rp_complete);
            if (OrgConfig.get().show_severity) {
                writer.write(c.severity);
            }

            if (OrgConfig.get().use_minor_referrals) {
                writer.write(c.minor_referral_destination);
            }
            writer.write("" + c.referred_to_sm);
            if (c.sm_decision != null) {
                writer.write(c.sm_decision);
            } else {
                writer.write("");
            }
            if (c.sm_decision_date != null) {
                writer.write(Application.yymmddDate(c.sm_decision_date));
            } else {
                writer.write("");
            }

            writer.endRecord();
        }
        writer.close();

        // Adding the BOM here causes Excel 2010 on Windows to realize
        // that the file is Unicode-encoded.
        return ok("\ufeff" + new String(baos.toByteArray(), charset));
    }

    public Result viewMeeting(int meeting_id) {
        return ok(views.html.view_meeting.render(Meeting.findById(meeting_id)));
    }

    public Result printMeeting(int meeting_id) throws Exception {
        response().setHeader("Content-Type", "application/pdf");
        return ok(
            renderToPDF(
                views.html.view_meeting.render(Meeting.findById(meeting_id)).toString()));
    }

    public Result editResolutionPlanList() {
        response().setHeader("Cache-Control", "max-age=0, no-cache, no-store");
        response().setHeader("Pragma", "no-cache");

        List<Integer> referenced_charge_ids = Charge.find
            .where()
            .eq("person.organization", Organization.getByHost())
            .isNotNull("referenced_charge_id")
            .select("referenced_charge")
            .findList()
            .stream()
            .map(c -> c.referenced_charge.id)
            .collect(Collectors.toList());

        List<Charge> active_rps =
            Charge.find
                .fetch("the_case")
                .fetch("the_case.meeting")
                .fetch("person")
                .fetch("rule")
                .fetch("rule.section")
                .fetch("rule.section.chapter")
                .where()
                .eq("person.organization", Organization.getByHost())
                .or(Expr.ne("plea", "Not Guilty"),
                    Expr.isNotNull("sm_decision"))
                .ne("person", null)
                .eq("rp_complete", false)
                .not(Expr.in("id", referenced_charge_ids))
                .orderBy("id DESC").findList();

        List<Charge> completed_rps =
            Charge.find
                .fetch("the_case")
                .fetch("the_case.meeting")
                .fetch("person")
                .fetch("rule")
                .fetch("rule.section")
                .fetch("rule.section.chapter")
                .where()
                .eq("person.organization", Organization.getByHost())
                .ne("person", null)
                .eq("rp_complete", true)
                .not(Expr.in("id", referenced_charge_ids))
                .orderBy("rp_complete_date DESC")
                .setMaxRows(25).findList();

        List<Charge> nullified_rps =
            Charge.find
                .fetch("the_case")
                .fetch("the_case.meeting")
                .fetch("person")
                .fetch("rule")
                .fetch("rule.section")
                .fetch("rule.section.chapter")
                .where()
                .eq("person.organization", Organization.getByHost())
                .ne("person", null)
                .in("id", referenced_charge_ids)
                .orderBy("id DESC")
                .setMaxRows(10).findList();

        List<Charge> all = new ArrayList<Charge>(active_rps);
        all.addAll(completed_rps);
        all.addAll(nullified_rps);

        for (Charge charge : all) {
            Case c = charge.the_case;
            if (c.composite_findings == null) {
                c.composite_findings = c.generateCompositeFindingsFromChargeReferences();
            }
        }

        return ok(views.html.edit_rp_list.render(active_rps, completed_rps, nullified_rps));
    }

    public Result viewMeetingResolutionPlans(int meeting_id) {
        return ok(views.html.view_meeting_resolution_plans.render(Meeting.findById(meeting_id)));
    }

    public Result downloadMeetingResolutionPlans(int meeting_id) throws IOException {
        response().setHeader("Content-Type", "text/csv; charset=utf-8");
        response().setHeader("Content-Disposition", "attachment; filename=" + OrgConfig.get().str_res_plans + ".csv");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Charset charset = Charset.forName("UTF-8");
        CsvWriter writer = new CsvWriter(baos, ',', charset);

        writer.write("Person");
        writer.write("Case #");
        writer.write("Rule");
        writer.write(OrgConfig.get().str_res_plan_cap);
        writer.endRecord();

        Meeting m = Meeting.findById(meeting_id);
        for (Case c : m.cases) {
            for (Charge charge : c.charges) {
                if (charge.displayInResolutionPlanList() && !charge.referred_to_sm) {
                    writer.write(charge.person.getDisplayName());

                    // In case it's needed in the future, adding a space to
                    // the front of the case number prevents MS Excel from
                    // misinterpreting it as a date.
                    //
                    // writer.write(" " + charge.the_case.case_number,
                    // true);

                    writer.write(charge.the_case.case_number + " (" +
                        (charge.sm_decision_date != null
                            ? Application.formatDayOfWeek(charge.sm_decision_date) + "--SM"
                            : Application.formatDayOfWeek(charge.the_case.meeting.date))
                        + ")");
                    writer.write(charge.rule.title);
                    writer.write(charge.resolution_plan);
                    writer.endRecord();
                }
            }
        }
        writer.close();

        // Adding the BOM here causes Excel 2010 on Windows to realize
        // that the file is Unicode-encoded.
        return ok("\ufeff" + new String(baos.toByteArray(), charset));
    }

    public Result viewManual() {
        return ok(renderManualTOC());
    }

    public Result viewManualChanges(String begin_date_string) {
        Date begin_date = null;

        try {
            begin_date = new SimpleDateFormat("yyyy-M-d").parse(begin_date_string);
        } catch (ParseException e) {
            begin_date = new Date(new Date().getTime() - 1000 * 60 * 60 * 24 * 7);
            begin_date.setHours(0);
            begin_date.setMinutes(0);
        }

        List<ManualChange> changes =
            ManualChange.find.where()
                .gt("date_entered", begin_date)
                .eq("entry.section.chapter.organization", Organization.getByHost())
                .orderBy("entry.id, date_entered ASC")
                .findList();

        List<ManualChange> changes_to_display = new ArrayList<ManualChange>();
        int last_id = -1;
        for (ManualChange c : changes) {
            if (c.entry.id != last_id) {
                changes_to_display.add(c);
            }
            last_id = c.entry.id;
        }

        Collections.sort(changes_to_display, ManualChange.SORT_NUM_DATE);

        return ok(views.html.view_manual_changes.render(
            forDateInput(begin_date),
            changes_to_display));
    }

    public Result printManual() {
        return ok(views.html.print_manual.render(Chapter.all()));
    }

    static Path print_temp_dir;

    public static void copyPrintingAssetsToTempDir() throws IOException {
        if (print_temp_dir == null) {
            print_temp_dir = Files.createTempDirectory("dst");
        }

        if (!Files.exists(print_temp_dir.resolve("stylesheets"))) {
            Files.createDirectory(print_temp_dir.resolve("stylesheets"));
        }

        String names[] = new String[] {
            "main.css",
        };

        for (String name : names) {
            Files.copy(Play.application().resourceAsStream("public/stylesheets/" + name),
                print_temp_dir.resolve("stylesheets").resolve(name),
                StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static File prepareTempHTML(String orig_html) throws IOException {
        copyPrintingAssetsToTempDir();

        File html_file =
            Files.createTempFile(print_temp_dir, "chapter", ".xhtml").toFile();

        OutputStreamWriter writer = new OutputStreamWriter(
            new FileOutputStream(html_file),
            Charset.forName("UTF-8"));

        // XML 1.0 only allows the following characters
        // #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
        String xml10pattern = "[^"
                            + "\u0009\r\n"
                            + "\u0020-\uD7FF"
                            + "\uE000-\uFFFD"
                            + "\ud800\udc00-\udbff\udfff"
                            + "]";

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

    public static byte[] renderToPDF(String orig_html) throws Exception {
        File html_file = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();

            html_file = prepareTempHTML(orig_html);
            renderer.setDocument(html_file);
            renderer.layout();
            renderer.createPDF(baos);

            return baos.toByteArray();
        } finally {
            if (html_file != null) {
                html_file.delete();
            }
        }
    }

    public static byte[] renderToPDF(List<String> orig_htmls) throws Exception {
        File html_file = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();

            boolean first = true;
            for(String orig_html : orig_htmls) {
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
            return baos.toByteArray();
        } finally {
            if (html_file != null) {
                html_file.delete();
            }
        }
    }

    static play.twirl.api.Html renderManualTOC() {
        return views.html.cached_page.render(
            new CachedPage(CachedPage.MANUAL_INDEX,
                OrgConfig.get().str_manual_title,
                "manual",
                "toc") {
                @Override
                String render() {
                    return views.html.view_manual.render(Chapter.all()).toString();
                }
            });
    }

    public Result printManualChapter(Integer id) throws Exception {
        response().setHeader("Content-Type", "application/pdf");

        if (id == -1) {
            ArrayList<String> documents = new ArrayList<String>();
            // render TOC
            documents.add(renderManualTOC().toString());
            // then render all chapters
            for (Chapter chapter : Chapter.all()) {
                documents.add(views.html.view_chapter.render(chapter).toString());
            }
            return ok(renderToPDF(documents));
        } else {
            if (id == -2) {
                return ok(renderToPDF(renderManualTOC().toString()));
            } else {
                Chapter chapter = Chapter.findById(id);
                return ok(renderToPDF(
                    views.html.view_chapter.render(chapter).toString()));
            }
        }
    }

    public Result viewChapter(Integer id) {
        Chapter c = Chapter.find
            .fetch("sections", new FetchConfig().query())
            .fetch("sections.entries", new FetchConfig().query())
            .where().eq("organization", Organization.getByHost())
            .eq("id", id).findUnique();

        return ok(views.html.view_chapter.render(c));
    }

    public Result searchManual(String searchString) {
        Map<String, Object> scopes = new HashMap<String, Object>();
        scopes.put("searchString", searchString);

        String sql =
            "SELECT ei.id " +
            "FROM entry_index ei " +
            "WHERE ei.document @@ plainto_tsquery(:searchString) " +
            "and ei.organization_id=:orgId " +
            "ORDER BY ts_rank(ei.document, plainto_tsquery(:searchString), 0) DESC";

        OrgConfig orgConfig = OrgConfig.get();
        SqlQuery sqlQuery = Ebean.createSqlQuery(sql);
        sqlQuery.setParameter("orgId", orgConfig.org.id);
        sqlQuery.setParameter("searchString", searchString);

        List<SqlRow> result = sqlQuery.findList();
        List<Integer> entryIds = new ArrayList<Integer>();
        for (int i = 0; i < result.size(); i++) {
            entryIds.add(result.get(i).getInteger("id"));
        }

        Map<Integer, String> entryToHeadline = new HashMap<Integer, String>();
        if (entryIds.size() > 0) {
            sql = "SELECT e.id, ts_headline(e.content, plainto_tsquery(:searchString), 'MaxFragments=5') as headline " +
                "FROM entry e WHERE e.id IN (:entryIds)";
            sqlQuery = Ebean.createSqlQuery(sql);
            sqlQuery.setParameter("searchString", searchString);
            sqlQuery.setParameter("entryIds", entryIds);
            List<SqlRow> headlines = sqlQuery.findList();
            for (int i = 0; i < headlines.size(); i++) {
                entryToHeadline.put(headlines.get(i).getInteger("id"),
                    headlines.get(i).getString("headline"));
            }
        }

        Map<Integer, Entry> entries = Entry.find
            .fetch("section", new FetchConfig().query())
            .fetch("section.chapter", new FetchConfig().query())
            .where().in("id", entryIds).findMap();

        ArrayList<Map<String, Object>> entriesList = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < result.size(); i++) {
            Map<String, Object> entryInfo = new HashMap<String, Object>();
            int entryId = result.get(i).getInteger("id");
            entryInfo.put("entry", entries.get(entryId));
            entryInfo.put("headline", entryToHeadline.get(entryId));
            entriesList.add(entryInfo);
        }
        scopes.put("entries", entriesList);

        return ok(views.html.main_with_mustache.render(
            "Manual Search: " + searchString,
            "manual",
            "",
            "manual_search.html",
            scopes));
    }

    static List<Charge> getLastWeekCharges(Person p) {
        List<Charge> last_week_charges = new ArrayList<Charge>();

        Date now = new Date();

        Collections.sort(p.charges);
        Collections.reverse(p.charges);
        for (Charge c : p.charges) {
            // Include if <= 7 days ago
            if (now.getTime() - c.the_case.meeting.date.getTime() <
                1000 * 60 * 60 * 24 * 7.5) {
                last_week_charges.add(c);
            }
        }

        return last_week_charges;
    }

    static Collection<String> getRecentResolutionPlans(Entry r) {
        Set<String> rps = new HashSet<String>();

        Collections.sort(r.charges);
        Collections.reverse(r.charges);

        for (Charge c : r.charges) {
            if (!c.resolution_plan.toLowerCase().equals("warning") &&
                !c.resolution_plan.equals("")) {
                rps.add(c.resolution_plan);
            }
            if (rps.size() > 9) {
                break;
            }
        }

        return rps;
    }

    public Result getPersonRuleHistory(Integer personId, Integer ruleId) {
        Person p = Person.findByIdWithJCData(personId);
        Entry r = Entry.findById(ruleId);

        PersonHistory history = new PersonHistory(p, false, getStartOfYear(), null);
        PersonHistory.Record rule_record = null;
        for (PersonHistory.Record record : history.rule_records) {
            if (record.rule != null && record.rule.equals(r)) {
                rule_record = record;
                break;
            }
        }

        return ok(views.html.person_rule_history.render(
            p, r, rule_record, history.charges_by_rule.get(r), history));
    }

    public Result getPersonHistory(Integer id) {
        Person p = Person.findByIdWithJCData(id);
        return ok(views.html.person_history.render(
            p,
            new PersonHistory(p, false, getStartOfYear(), null),
            getLastWeekCharges(p),
            false));
    }

    public Result viewPersonHistory(Integer id, Boolean redact_names, String start_date_str, String end_date_str) {
        Date start_date = getStartOfYear();
        Date end_date = null;

        try {
            start_date = new SimpleDateFormat("yyyy-M-d").parse(start_date_str);
            end_date = new SimpleDateFormat("yyyy-M-d").parse(end_date_str);
        } catch (ParseException e) {
        }

        Person p = Person.findByIdWithJCData(id);
        return ok(views.html.view_person_history.render(
            p,
            new PersonHistory(p, true, start_date, end_date),
            getLastWeekCharges(p),
            redact_names));
    }

    public Result getRuleHistory(Integer id) {
        Entry r = Entry.findByIdWithJCData(id);
        return ok(views.html.rule_history.render(
            r,
            new RuleHistory(r, false, getStartOfYear(), null),
            getRecentResolutionPlans(r)));
    }

    public Result viewRuleHistory(Integer id, String start_date_str, String end_date_str) {
        Date start_date = getStartOfYear();
        Date end_date = null;

        try {
            start_date = new SimpleDateFormat("yyyy-M-d").parse(start_date_str);
            end_date = new SimpleDateFormat("yyyy-M-d").parse(end_date_str);
        } catch (ParseException e) {
        }

        Entry r = Entry.findByIdWithJCData(id);
        return ok(views.html.view_rule_history.render(
            r,
            new RuleHistory(r, true, start_date, end_date),
            getRecentResolutionPlans(r)));
    }

    public Result viewPersonsWriteups(Integer id) {
        Person p = Person.findByIdWithJCData(id);

        List<Case> cases_written_up = new ArrayList<Case>(p.getThisYearCasesWrittenUp());

        Collections.sort(cases_written_up);
        Collections.reverse(cases_written_up);

        return ok(views.html.view_persons_writeups.render(p, cases_written_up));
    }

    public Result thisWeekReport() {
        return viewWeeklyReport("");
    }

    public Result printWeeklyMinutes(String date_string) throws Exception {
        Calendar start_date = new GregorianCalendar();
        try {
            Date parsed_date = new SimpleDateFormat("yyyy-M-d").parse(date_string);
            if (parsed_date != null) {
                start_date.setTime(parsed_date);
            }
        } catch (ParseException e) {
            System.out.println("Failed to parse given date (" + date_string + "), using current");
        }

        Calendar end_date = (Calendar)start_date.clone();
        end_date.add(GregorianCalendar.DATE, 6);

        response().setHeader("Content-Type", "application/pdf");
        ArrayList<String> documents = new ArrayList<String>();

        List<Meeting> meetings = Meeting.find.where()
            .eq("organization", Organization.getByHost())
            .le("date", end_date.getTime())
            .ge("date", start_date.getTime()).findList();
        for (Meeting m : meetings) {
            documents.add(views.html.view_meeting.render(m).toString());
        }

        return ok(renderToPDF(documents));
    }

    public Result viewWeeklyReport(String date_string) {
        Calendar start_date = Utils.parseDateOrNow(date_string);
        Utils.adjustToPreviousDay(start_date, OrgConfig.get().org.jc_reset_day + 1);

        Calendar end_date = (Calendar)start_date.clone();
        end_date.add(GregorianCalendar.DATE, 6);

        List<Charge> all_charges = Charge.find
            .fetch("the_case")
            .fetch("person")
            .fetch("rule")
            .fetch("the_case.meeting", new FetchConfig().query())
            .where().eq("person.organization", Organization.getByHost())
                .ge("the_case.meeting.date", getStartOfYear(start_date.getTime()))
            .findList();
        WeeklyStats result = new WeeklyStats();
        result.rule_counts = new TreeMap<Entry, Integer>();
        result.person_counts = new TreeMap<Person, WeeklyStats.PersonCounts>();

        for (Charge c : all_charges) {
            long case_millis = c.the_case.meeting.date.getTime();
            long diff = end_date.getTime().getTime() - case_millis;

            if (c.rule != null && c.person != null) {
                if (diff >= 0 &&
                    diff < 6.5 * 24 * 60 * 60 * 1000) {
                    result.rule_counts.put(c.rule, 1 + getOrDefault(result.rule_counts, c.rule, 0));
                    result.person_counts.put(c.person, getOrDefault(result.person_counts, c.person, new WeeklyStats.PersonCounts()).addThisPeriod());
                    result.num_charges++;
                }
                if (diff >= 0 &&
                    diff < 27.5 * 24 * 60 * 60 * 1000) {
                    result.person_counts.put(c.person, getOrDefault(result.person_counts, c.person, new WeeklyStats.PersonCounts()).addLast28Days());
                }
                if (diff >= 0) {
                    result.person_counts.put(c.person, getOrDefault(result.person_counts, c.person, new WeeklyStats.PersonCounts()).addAllTime());
                }
            }
        }

        List<Case> all_cases = new ArrayList<Case>();

        List<Meeting> meetings = Meeting.find.where()
            .eq("organization", Organization.getByHost())
            .le("date", end_date.getTime())
            .ge("date", start_date.getTime()).findList();

        for (Meeting m : meetings) {
            for (Case c : m.cases) {
                result.num_cases++;

                all_cases.add(c);
            }
        }

        result.uncharged_people = allPeople();
        for (Map.Entry<Person, WeeklyStats.PersonCounts> entry : result.person_counts.entrySet()) {
            if (entry.getValue().this_period > 0) {
                result.uncharged_people.remove(entry.getKey());
            }
        }

        ArrayList<String> referral_destinations = new ArrayList<String>();
        for (Case c : all_cases) {
            for (Charge ch : c.charges) {
                if (!ch.minor_referral_destination.equals("") &&
                    !referral_destinations.contains(ch.minor_referral_destination)) {
                    referral_destinations.add(ch.minor_referral_destination);
                }
            }
        }

        return ok(views.html.jc_weekly_report.render(
            start_date.getTime(),
            end_date.getTime(),
            result,
            all_cases,
            referral_destinations));
    }

    public static String jcPeople(String term) {
        List<Person> people = allPeople();
        Collections.sort(people, Person.SORT_DISPLAY_NAME);

        term = term.toLowerCase();

        List<Map<String, String> > result = new ArrayList<Map<String, String> > ();
        for (Person p : people) {
            if (p.searchStringMatches(term)) {
                HashMap<String, String> values = new HashMap<String, String>();
                values.put("label", p.getDisplayName());
                values.put("id", "" + p.person_id);
                result.add(values);
            }
        }

        return Json.stringify(Json.toJson(result));
    }

    public static String jsonRules(Boolean includeBreakingResPlan) {

        ExpressionList expr = Entry.find.where().eq("section.chapter.organization", Organization.getByHost());
        if (!includeBreakingResPlan) {
            expr = expr.eq("is_breaking_res_plan", false);
        }
        List<Entry> rules = expr
            .eq("deleted", false)
            .eq("section.deleted", false)
            .eq("section.chapter.deleted", false)
            .orderBy("title ASC").findList();

        List<Map<String, String> > result = new ArrayList<Map<String, String> > ();
        for (Entry r : rules) {
            HashMap<String, String> values = new HashMap<String, String>();
            values.put("label", r.getNumber() + " " + r.title);
            values.put("id", "" + r.id);
            result.add(values);
        }

        return Json.stringify(Json.toJson(result));
    }

    public static String jsonCases(String term) {
        term = term.toLowerCase();

        List<Case> cases = Case.find.where()
            .eq("meeting.organization", Organization.getByHost())
            .ge("meeting.date", Application.getStartOfYear())
            .like("case_number", term + "%")
            .orderBy("case_number ASC").findList();

        List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        for (Case c : cases) {
            HashMap<String, String> values = new HashMap<String, String>();
            values.put("label", c.case_number);
            values.put("id", "" + c.id);
            result.add(values);
        }

        return Json.stringify(Json.toJson(result));
    }

    public static String jsonBreakingResPlanEntry() {
        return Utils.toJson(Entry.findBreakingResPlanEntry());
    }

    public Result getLastRp(Integer personId, Integer ruleId) {
        Date now = new Date();

        // Look up person using findById to guarantee that the current user
        // has access to that organization.
        Person p = Person.findById(personId);
        List<Charge> charges = Charge.find.where().eq("person", p)
                .eq("rule_id", ruleId)
                .lt("the_case.meeting.date", now)
                .ge("the_case.meeting.date", Application.getStartOfYear())
                .orderBy("the_case.meeting.date DESC")
                .findList();

        if (charges.size() > 0) {
            Charge c = charges.get(0);
            return ok(views.html.last_rp.render(charges.size(), c));
        }

        return ok("No previous charge.");
    }

    public static Date addWeek(Date d, int numWeeks) {
        return new Date(d.getTime() + numWeeks * 7 * 24 * 60 * 60 * 1000);
    }

    private static <K, V> V getOrDefault(Map<K,V> map, K key, V defaultValue) {
        return map.containsKey(key) ? map.get(key) : defaultValue;
    }

    public static Date getStartOfYear() {
        return getStartOfYear(new Date());
    }

    public static Date getStartOfYear(Date d) {
        Date result = (Date)d.clone();

        if (result.getMonth() < 7) { // july or earlier
            result.setYear(result.getYear() - 1);
        }
        result.setMonth(7);
        result.setDate(1);

        return result;
    }

    public static String formatDayOfWeek(Date d) {
        return new SimpleDateFormat("EE").format(d);
    }

    public static String formatDateShort(Date d) {
        if (OrgConfig.get().euro_dates) {
            return new SimpleDateFormat("dd/MM").format(d);
        }
        return new SimpleDateFormat("MM/dd").format(d);
    }

    public static String formatDateMdy(Date d) {
        if (OrgConfig.get().euro_dates) {
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

    public static String yymmddDate(Date d) {
        if (OrgConfig.get().euro_dates) {
            return new SimpleDateFormat("dd-MM-yyyy").format(d);
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(d);
    }

    public static String yymmddDate() {
        Date d = new Date();
        if (OrgConfig.get().euro_dates) {
            return new SimpleDateFormat("dd-MM-yyyy").format(d);
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(d);
    }

    public static String formatDateTimeLong() {
        return new SimpleDateFormat("EEEE, MMMM d, h:mm a").format(Utils.localNow());
    }

    public static String yymdDate(Date d) {
        if (OrgConfig.get().euro_dates) {
            return new SimpleDateFormat("d-M-yyyy").format(d);
        }
        return new SimpleDateFormat("yyyy-M-d").format(d);
    }

    public static String currentUsername() {
        return Context.current().request().username();
    }

    public static boolean isUserEditor(String username) {
        return username != null && username.contains("@");
    }

    public static boolean isCurrentUserEditor() {
        return isUserEditor(currentUsername());
    }

    public static String getRemoteIp() {
        Context ctx = Context.current();
        Configuration conf = getConfiguration();

        if (conf.getBoolean("heroku_ips")) {
            String header = ctx.request().getHeader("X-Forwarded-For");
            if (header == null) {
                return "unknown-ip";
            }
            String splits[] = header.split("[, ]");
            return splits[splits.length - 1];
        } else {
            return ctx.request().remoteAddress();
        }
    }

    public static User getCurrentUser() {
        return User.findByAuthUserIdentity(
            sInstance.mAuth.getUser(Context.current().session()));
    }

    public static Configuration getConfiguration() {
        return Play.application().configuration().getConfig("school_crm");
    }

    public static String markdown(String input) {
        if (input == null) {
            return "";
        }
        try {
            return new Markdown4jProcessor().process(input);
        } catch (IOException e) {
            return e.toString() + "<br><br>" + input;
        }
    }

    public Result renderMarkdown() {
        Map<String, String[]> form_data = request().body().asFormUrlEncoded();

        if (form_data == null || form_data.get("markdown") == null) {
            return ok("");
        }

        String markdown = form_data.get("markdown")[0];
        return ok(markdown(markdown));
    }

    @Secured.Auth(UserRole.ROLE_ALL_ACCESS)
    public Result fileSharing() {
        Map<String, Object> scopes = new HashMap<String, Object>();

        ArrayList<String> existingFiles = new ArrayList<>();
        File files[] = getSharedFileDirectory().listFiles();
        for (File f : files) {
            existingFiles.add(f.getName());
        }

        scopes.put("existing_files", existingFiles);

        scopes.put("printer_email", OrgConfig.get().org.printer_email);
        return ok(views.html.main_with_mustache.render(
            "File Sharing config",
            "misc",
            "file_sharing",
            "file_sharing.html",
            scopes));
    }

    @Secured.Auth(UserRole.ROLE_ALL_ACCESS)
    public Result saveFileSharingSettings() {
        final Map<String, String[]> values = request().body().asFormUrlEncoded();

        if (values.containsKey("printer_email")) {
            OrgConfig.get().org.setPrinterEmail(values.get("printer_email")[0]);
        }

        return redirect(routes.Application.fileSharing());
    }

    @Secured.Auth(UserRole.ROLE_ALL_ACCESS)
    public Result uploadFileShare() throws IOException {
        Http.MultipartFormData<File> body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart<File> pdf = body.getFile("pdf_upload");
        if (pdf != null) {
            String fileName = pdf.getFilename();
            if (!fileName.equals("")) {
                File outputFile = new File(getSharedFileDirectory(), fileName);
                copyFileUsingStream(pdf.getFile(), outputFile);
            }
        }
        return redirect(routes.Application.fileSharing());
    }

    private static File getSharedFileDirectory() {
        File result = new File("/www-dst", "" + OrgConfig.get().org.id);
        if (!result.exists()) {
            result.mkdir();
        }
        return result;
    }

    @Secured.Auth(UserRole.ROLE_ALL_ACCESS)
    public Result deleteFile() {
        final Map<String, String[]> values = request().body().asFormUrlEncoded();

        if (values.containsKey("filename")) {
            File the_file = new File(getSharedFileDirectory(), values.get("filename")[0]);
            if (the_file.exists()) {
                the_file.delete();
            }
        }

        return redirect(routes.Application.fileSharing());
    }

    public Result viewFiles() {
        Map<String, Object> scopes = new HashMap<String, Object>();

        ArrayList<String> existingFiles = new ArrayList<>();
        File files[] = getSharedFileDirectory().listFiles();
        for (File f : files) {
            existingFiles.add(f.getName());
        }

        scopes.put("existing_files", existingFiles);

        scopes.put("printer_email", OrgConfig.get().org.printer_email);
        return ok(views.html.main_with_mustache.render(
            "DemSchoolTools shared files",
            "misc",
            "view_files",
            "view_files.html",
            scopes));
    }

    public Result emailFile() {
        final Map<String, String[]> values = request().body().asFormUrlEncoded();

        if (values.containsKey("filename")) {
            File the_file = new File(getSharedFileDirectory(), values.get("filename")[0]);
            if (the_file.exists()) {
                play.libs.mailer.Email mail = new play.libs.mailer.Email();
                mail.setSubject("Print from DemSchoolTools");
                mail.addTo(OrgConfig.get().org.printer_email);
                mail.setFrom("DemSchoolTools <noreply@demschooltools.com>");
                mail.addAttachment(the_file.getName(), the_file);
                play.libs.mailer.MailerPlugin.send(mail);
            }
        }

        return redirect(routes.Application.viewFiles());
    }

    public Result sendFeedbackEmail() {
        final Map<String, String[]> values = request().body().asFormUrlEncoded();

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
            body.append(values.get("message")[0] + "\n---\n");
        }
        if (values.containsKey("name")) {
            body.append(values.get("name")[0] + "\n");
        }
        if (values.containsKey("email")) {
            body.append(values.get("email")[0] + "\n");
        }

        try {
            body.append("\n\n=========================\n");
            body.append(Utils.toJson(request().headers()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        mail.setBodyText(body.toString());
        play.libs.mailer.MailerPlugin.send(mail);

        return ok();
    }

    public static void copyFileUsingStream(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }

}
