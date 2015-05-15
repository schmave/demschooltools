package controllers;

import java.io.*;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.markdown4j.Markdown4jProcessor;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.Expression;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;
import com.avaje.ebean.SqlUpdate;
import com.csvreader.CsvWriter;
import com.feth.play.module.pa.PlayAuthenticate;
import com.typesafe.plugin.*;

import models.*;

import play.*;
import play.data.*;
import play.libs.Json;
import play.mvc.*;
import play.mvc.Http.Context;

@Security.Authenticated(Secured.class)
public class Application extends Controller {

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

    public static Result viewSchoolMeetingDecisions() {
        List<Charge> the_charges =
            Charge.find.where()
                .eq("referred_to_sm", true)
                .eq("person.organization", Organization.getByHost())
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

        List<Person> people = CRM.getPeopleForTag(cur_student_tag.id);
        people.addAll(CRM.getPeopleForTag(staff_tag.id));
		return people;
	}

    public static Result index() {
        List<Meeting> meetings = Meeting.find
            .where().eq("organization", Organization.getByHost())
            .orderBy("date DESC").findList();

        List<Charge> sm_charges = getActiveSchoolMeetingReferrals();

        List<Person> people = allPeople();
        Collections.sort(people, Person.SORT_DISPLAY_NAME);

        List<Entry> entries = Entry.find
            .where().eq("section.chapter.organization", Organization.getByHost())
            .findList();
        List<Entry> entries_with_charges = new ArrayList<Entry>();
        for (Entry e : entries) {
            if (e.charges.size() > 0) {
                entries_with_charges.add(e);
            }
        }

        Collections.sort(entries_with_charges, Entry.SORT_NUMBER);

        return ok(views.html.jc_index.render(meetings, sm_charges, people,
            entries_with_charges));
    }

    public static Result viewMeeting(int meeting_id) {
        return ok(views.html.view_meeting.render(Meeting.findById(meeting_id)));
    }

    public static Result viewMeetingResolutionPlans(int meeting_id) {
        return ok(views.html.view_meeting_resolution_plans.render(Meeting.findById(meeting_id)));
    }

    public static Result downloadMeetingResolutionPlans(int meeting_id) {
        response().setHeader("Content-Type", "text/csv; charset=utf-8");
        response().setHeader("Content-Disposition", "attachment; filename=" + OrgConfig.get().str_res_plans + ".csv");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Charset charset = Charset.forName("UTF-8");
        CsvWriter writer = new CsvWriter(baos, ',', charset);

        try {
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
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Adding the BOM here causes Excel 2010 on Windows to realize
        // that the file is Unicode-encoded.
        return ok("\ufeff" + new String(baos.toByteArray(), charset));
    }

	public static Result viewManual() {
		return ok(views.html.view_manual.render(Chapter.all()));
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

	public static Result viewChapter(Integer id) {
		return ok(views.html.view_chapter.render(Chapter.findById(id)));
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

    public static Result getPersonHistory(Integer id) {
        Person p = Person.findById(id);
        return ok(views.html.person_history.render(p, new PersonHistory(p, false), getLastWeekCharges(p), false));
    }

    public static Result getRuleHistory(Integer id) {
        Entry r = Entry.findById(id);
        return ok(views.html.rule_history.render(r, new RuleHistory(r, false), getRecentResolutionPlans(r)));
    }

    public static Result viewPersonHistory(Integer id, Boolean redact_names) {
        Person p = Person.findById(id);
        return ok(views.html.view_person_history.render(p, new PersonHistory(p), getLastWeekCharges(p), redact_names));
    }

    public static Result viewRuleHistory(Integer id) {
        Entry r = Entry.findById(id);
        return ok(views.html.view_rule_history.render(r, new RuleHistory(r), getRecentResolutionPlans(r)));
	}

    public static Result thisWeekReport() {
        return viewWeeklyReport("");
    }

    public static Result viewWeeklyReport(String date_string) {
        Calendar start_date = new GregorianCalendar();

        try {
            Date parsed_date = new SimpleDateFormat("yyyy-M-d").parse(date_string);
            if (parsed_date != null) {
                start_date.setTime(parsed_date);
            }
        } catch (ParseException e) {
            System.out.println("Failed to parse given date (" + date_string + "), using current");
        }

        int dow = start_date.get(Calendar.DAY_OF_WEEK);
        if (dow >= Calendar.WEDNESDAY) {
            start_date.add(GregorianCalendar.DATE, -(dow - Calendar.WEDNESDAY));
        } else {
            start_date.add(GregorianCalendar.DATE, Calendar.WEDNESDAY - dow - 7);
        }

        Calendar end_date = (Calendar)start_date.clone();
        end_date.add(GregorianCalendar.DATE, 6);

        List<Charge> all_charges = Charge.find
            .where().eq("person.organization", Organization.getByHost())
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
                .orderBy("the_case.meeting.date DESC")
                .findList();

        if (charges.size() > 0) {
            Charge c = charges.get(0);
            return ok("" + charges.size() + " prior charges. Last " +
                OrgConfig.get().str_res_plan_short + " (from case #" +
                c.the_case.case_number + "): <u>" + c.resolution_plan + "</u>");
        }

        return ok("No previous charge.");
    }

    public static Date addWeek(Date d, int numWeeks) {
        return new Date(d.getTime() + numWeeks * 7 * 24 * 60 * 60 * 1000);
    }

    private static <K, V> V getOrDefault(Map<K,V> map, K key, V defaultValue) {
        return map.containsKey(key) ? map.get(key) : defaultValue;
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

	public static String yymmddDate(Date d ) {
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
}
