package controllers;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.markdown4j.Markdown4jProcessor;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.Expression;
import com.avaje.ebean.FetchConfig;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;
import com.avaje.ebean.SqlUpdate;
import com.csvreader.CsvWriter;
import com.feth.play.module.pa.PlayAuthenticate;

import models.*;

import play.*;
import play.data.*;
import play.libs.Json;
import play.mvc.*;
import play.mvc.Http.Context;

@With(DumpOnError.class)
@Secured.Auth(UserRole.ROLE_VIEW_JC)
public class Application extends Controller {

    public static final String CACHE_INDEX = "Application-index-";
    public static final String CACHE_MANUAL = "Application-viewManual-";

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
	
	public static Result viewSchoolMeetingReferrals() {
		return ok(views.html.view_sm_referrals.render(
			getActiveSchoolMeetingReferrals()));
	}

    public static Result viewSchoolMeetingDecisions() {
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
        Tag cur_student_tag = Tag.find.where()
            .eq("title", "Current Student")
            .eq("organization", Organization.getByHost())
            .findUnique();
        Tag staff_tag = Tag.find.where()
            .eq("title", "Staff")
            .eq("organization", Organization.getByHost())
            .findUnique();

        List<Person> people = new ArrayList<Person>(cur_student_tag.people);
        people.addAll(staff_tag.people);
		return people;
	}

    public static Result index() {
        return ok(views.html.cached_page.render(
            new CachedPage(CACHE_INDEX,
                "JC database",
                "jc",
                "jc_home") {
                @Override
                String render() {
        List<Meeting> meetings = Meeting.find
            .fetch("cases")
            .where().eq("organization", Organization.getByHost())
            .orderBy("date DESC").findList();

        Tag cur_student_tag = Tag.find.where()
            .eq("title", "Current Student")
            .eq("organization", Organization.getByHost())
            .findUnique();
        Tag staff_tag = Tag.find.where()
            .eq("title", "Staff")
            .eq("organization", Organization.getByHost())
            .findUnique();

        List<Person> people = Person.find
            .fetch("charges")
            .fetch("charges.the_case")
            .fetch("charges.the_case.meeting")
            .fetch("cases_involved_in", new FetchConfig().query())
            .fetch("cases_involved_in.the_case", new FetchConfig().query())
            .fetch("cases_involved_in.the_case.meeting", new FetchConfig().query())
            .where()
            .in("tags", cur_student_tag, staff_tag)
            .findList();

        Collections.sort(people, Person.SORT_DISPLAY_NAME);

        List<Entry> entries = Entry.find
            .fetch("charges")
            .fetch("charges.the_case")
            .fetch("charges.the_case.meeting")
            .fetch("section")
            .fetch("section.chapter")
            .where().eq("section.chapter.organization", Organization.getByHost())
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

    public static Result downloadCharges() throws IOException {
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
            writer.write(c.the_case.findings);

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

    public static Result viewMeeting(int meeting_id) {
        return ok(views.html.view_meeting.render(Meeting.findById(meeting_id)));
    }

    public static Result printMeeting(int meeting_id) throws Exception {
        response().setHeader("Content-Type", "application/pdf");
        return ok(
            renderToPDF(
                views.html.view_meeting.render(Meeting.findById(meeting_id)).toString()));
    }

    public static Result editResolutionPlanList() {
        response().setHeader("Cache-Control", "max-age=0, no-cache, no-store");
        response().setHeader("Pragma", "no-cache");

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
                .orderBy("rp_complete_date DESC")
                .setMaxRows(25).findList();

        return ok(views.html.edit_rp_list.render(active_rps, completed_rps));
    }

    public static Result viewMeetingResolutionPlans(int meeting_id) {
        return ok(views.html.view_meeting_resolution_plans.render(Meeting.findById(meeting_id)));
    }

    public static Result downloadMeetingResolutionPlans(int meeting_id) throws IOException {
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

	public static Result viewManual() {
        return ok(renderManualTOC());
	}

    public static Result viewManualChanges(String begin_date_string) {
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
            new SimpleDateFormat("yyyy-MM-dd").format(begin_date),
            changes_to_display));
    }

    public static Result printManual() {
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
            new CachedPage(CACHE_MANUAL,
                OrgConfig.get().str_manual_title,
                "manual",
                "toc") {
                @Override
                String render() {
                    return views.html.view_manual.render(Chapter.all()).toString();
                }
            });
    }

    public static Result printManualChapter(Integer id) throws Exception {
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

	public static Result viewChapter(Integer id) {
        Chapter c = Chapter.find
            .fetch("sections", new FetchConfig().query())
            .fetch("sections.entries", new FetchConfig().query())
            .where().eq("organization", Organization.getByHost())
            .eq("id", id).findUnique();

        System.out.println("going into render\n\n\n");

		return ok(views.html.view_chapter.render(c));
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

    public static Result getPersonRuleHistory(Integer personId, Integer ruleId) {
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

    public static Result getPersonHistory(Integer id) {
        Person p = Person.findByIdWithJCData(id);
        return ok(views.html.person_history.render(
			p,
			new PersonHistory(p, false, getStartOfYear(), null),
			getLastWeekCharges(p),
			false));
    }

    public static Result viewPersonHistory(Integer id, Boolean redact_names, String start_date_str, String end_date_str) {
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

    public static Result getRuleHistory(Integer id) {
        Entry r = Entry.findByIdWithJCData(id);
        return ok(views.html.rule_history.render(
			r,
			new RuleHistory(r, false, getStartOfYear(), null),
			getRecentResolutionPlans(r)));
    }

    public static Result viewRuleHistory(Integer id, String start_date_str, String end_date_str) {
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

    public static Result viewPersonsWriteups(Integer id) {
        Person p = Person.findByIdWithJCData(id);

		List<Case> cases_written_up = new ArrayList<Case>(p.getThisYearCasesWrittenUp());

		Collections.sort(cases_written_up);
		Collections.reverse(cases_written_up);

        return ok(views.html.view_persons_writeups.render(p, cases_written_up));
    }

    public static Result thisWeekReport() {
        return viewWeeklyReport("");
    }

    public static Result printWeeklyMinutes(String date_string) throws Exception {
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

    public static Result viewWeeklyReport(String date_string) {
        Calendar start_date = Utils.parseDateOrNow(date_string);
        Utils.adjustToPreviousDay(start_date, Calendar.WEDNESDAY);

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

    public static String jsonRules(String term) {
		term = term.toLowerCase();

        List<Entry> rules = Entry.find.where()
            .eq("section.chapter.organization", Organization.getByHost())
            .eq("deleted", false).orderBy("title ASC").findList();

        List<Map<String, String> > result = new ArrayList<Map<String, String> > ();
        for (Entry r : rules) {
            if (r.title.toLowerCase().contains(term)) {
                HashMap<String, String> values = new HashMap<String, String>();
                values.put("label", r.getNumber() + " " + r.title);
                values.put("id", "" + r.id);
                result.add(values);
            }
        }

        return Json.stringify(Json.toJson(result));
    }

    public static Result getLastRp(Integer personId, Integer ruleId) {
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
        return new SimpleDateFormat("MM/dd").format(d);
    }

    public static String formatMeetingDate(Date d) {
        return new SimpleDateFormat("EE--MMMM dd, yyyy").format(d);
    }

	public static String yymmddDate(Date d) {
		return new SimpleDateFormat("yyyy-M-d").format(d);
	}

    public static String yymmddDate() {
        return new SimpleDateFormat("yyyy-M-d").format(new Date());
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
            PlayAuthenticate.getUser(Context.current().session()));
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

    public static Result renderMarkdown() {
        Map<String, String[]> form_data = request().body().asFormUrlEncoded();

        String markdown = form_data.get("markdown")[0];
        return ok(markdown(markdown));
    }
}
