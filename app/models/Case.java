package models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.*;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import controllers.Application;
import controllers.Utils;

import com.avaje.ebean.Model;
import com.avaje.ebean.Model.Finder;
import play.libs.Json;

@Entity
@Table(name="`case`")
@JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
public class Case extends Model implements Comparable<Case> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "case_id_seq")
    public Integer id;

    public String case_number;

    public String location = "";
    public String findings = "";

    public Date date;
    public String time = "";

    @ManyToOne
    @JoinColumn(name="meeting_id")
    @JsonIgnore
    public Meeting meeting;

    @JsonIgnore
    @ManyToMany(mappedBy = "additional_cases")
    public List<Meeting> additional_meetings;

    @OneToMany(mappedBy="the_case")
    public List<PersonAtCase> people_at_case;

    @OneToMany(mappedBy="the_case")
    @OrderBy("id ASC")
    public List<Charge> charges;

    static Set<String> names;

    public Date date_closed;

    @ManyToMany
    @JoinTable(name="case_reference",
        joinColumns=@JoinColumn(name="referencing_case", referencedColumnName="id"),
        inverseJoinColumns=@JoinColumn(name="referenced_case", referencedColumnName="id"))
    public List<Case> referenced_cases;

    @ManyToMany(mappedBy="referenced_cases")
    @JsonIgnore
    public List<Case> referencing_cases;

    public static Finder<Integer, Case> find = new Finder<Integer, Case>(
        Case.class
    );

    public static Case findById(Integer id) {
        return find.where().eq("meeting.organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }

    public static List<Case> getOpenCases() {
        return find.where()
            .eq("meeting.organization", Organization.getByHost())
            .eq("date_closed", null)
            .order("case_number ASC")
            .findList();
    }

    public static Case create(String number, Meeting m)
    {
        Case result = new Case();
        result.case_number = number;
        result.meeting = m;
        result.date_closed = m.date; // closed by default
        result.save();

        return result;
    }

    public void continueInMeeting(Meeting m) {
        try {
            // remember the previous meeting
            this.meeting.additional_cases.add(this);
            this.meeting.save();
        }
        catch (PersistenceException pe) {
            // Throw the exception only if this is something other than a
            // "unique_violation", meaning that this CaseMeeting already exists.
            Utils.eatIfUniqueViolation(pe);
        }

        this.meeting = m;

        this.update();
    }

    public void edit(Map<String, String[]> query_string) {
        findings = query_string.get("findings")[0];
        location = query_string.get("location")[0];

        String date_string = query_string.get("date")[0];
        date = controllers.Application.getDateFromString(date_string);
        time = query_string.get("time")[0];

        System.out.println("closed: " + query_string.get("closed")[0]);
        if (query_string.get("closed")[0].equals("true")) {
            date_closed = meeting.date;
        } else {
            date_closed = null;
        }
        this.update();
    }

	public boolean empty() {
		return findings.equals("") &&
			location.equals("") &&
			date == null &&
            time.equals("") &&
			people_at_case.size() == 0 &&
			charges.size() == 0;
	}

    public void loadNames() {
        if (names != null) {
            return;
        }

        names = new HashSet<String>();

        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(
                play.Play.application().classloader().getResourceAsStream("names.txt")));
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
        for(Person p : Application.allPeople()) {
            names.add(p.first_name.trim().toLowerCase());
            names.add(p.getDisplayName().trim().toLowerCase());
        }
    }

    public String getRedactedFindings(Person keep_this_persons_name) {
        loadNames();

        String keep_first_name = keep_this_persons_name.first_name.trim().toLowerCase();
        String keep_display_name = keep_this_persons_name.getDisplayName().trim().toLowerCase();

        String[] words = findings.split("\\b");
        Map<String, String> replacement_names = new HashMap<String, String>();
        char next_replacement = 'A';

        for (String w : words) {
            w = w.toLowerCase();
            if (!w.equals(keep_first_name) && !w.equals(keep_display_name) &&
                names.contains(w) && replacement_names.get(w) == null) {
                replacement_names.put(w, next_replacement + "___");

                next_replacement++;
            }
        }

        String new_findings = this.findings;
        for (String name : replacement_names.keySet()) {
            new_findings = new_findings.replaceAll("(?i)" + name, replacement_names.get(name));
        }

        return new_findings;
    }

    public int compareTo(Case other) {
        return meeting.date.compareTo(other.meeting.date);
    }

    public List<String> getReferencedCasesText() {
        return referenced_cases.stream().map(c -> c.getReferencedCaseText()).collect(Collectors.toList());
    }

    private String getReferencedCaseText() {
        String result = case_number + ". " + findings;
        if (!findings.endsWith(".")) {
            result += ".";
        }
        for (Charge charge : charges) {
            if (charge.rule != null && charge.person != null) {
                result += " " + charge.person.getDisplayName() + " was charged with " + charge.rule.getNumber() + " " + charge.rule.title;
                if (!charge.rp_text.isEmpty()) {
                    result += ", " + OrgConfig.get().str_res_plan_short + ": " + charge.rp_text;
                    if (!charge.rp_text.endsWith(".")) {
                        result += ".";
                    }
                } else {
                    result += ".";
                }
            }
        }
        return result;
    }
}
