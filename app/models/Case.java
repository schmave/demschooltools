package models;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import controllers.Application;

import com.avaje.ebean.validation.NotNull;
import play.db.ebean.Model;
import play.db.ebean.Model.Finder;
import play.libs.Json;

@Entity
@Table(name="`case`")
public class Case extends Model implements Comparable<Case> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "case_id_seq")
    public Integer id;

    public String case_number;

    public String location = "";
    public String findings = "";

    public Date date;
    @NotNull
    public String time = "";

    @ManyToOne
    @JoinColumn(name="meeting_id")
    @JsonIgnore
    public Meeting meeting;

    @ManyToOne
    @JoinColumn(name="writer_id")
    public Person writer;

    @OneToMany(mappedBy="the_case")
    public List<TestifyRecord> testify_records;

    @OneToMany(mappedBy="the_case")
    @OrderBy("id ASC")
    public List<Charge> charges;

    static Set<String> names;

    public static Finder<Integer, Case> find = new Finder(
        Integer.class, Case.class
    );

    public static Case findById(Integer id) {
        return find.where().eq("meeting.organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }

    public static Case create(String number, Meeting m)
    {
        Case result = new Case();
        result.case_number = number;
        result.meeting = m;
        result.save();

        return result;
    }

    public String toJson() {
        ObjectMapper m = new ObjectMapper();
        m.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        try
        {
            return m.writeValueAsString(this);
        }
        catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public void edit(Map<String, String[]> query_string) {
        findings = query_string.get("findings")[0];
        location = query_string.get("location")[0];

        String date_string = query_string.get("date")[0];
        date = controllers.Application.getDateFromString(date_string);
        time = query_string.get("time")[0];

        if (query_string.containsKey("writer_id")) {
            writer = Person.find.ref(Integer.parseInt(query_string.get("writer_id")[0]));
        } else {
            writer = null;
        }
    }

	public boolean empty() {
		return findings.equals("") &&
			location.equals("") &&
			writer == null &&
			date == null &&
            time.equals("") &&
			testify_records.size() == 0 &&
			charges.size() == 0;
	}

    public void loadNames() {
        if (names != null) {
            return;
        }

        names = new HashSet<String>();

        try {
            BufferedReader r = new BufferedReader(new FileReader(play.Play.application().getFile("app/assets/names.txt")));
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

    public String getRedactedFindings(String keep_this_name) {
        loadNames();

        keep_this_name = keep_this_name.trim().toLowerCase();

        String[] words = findings.split("\\b");
        Map<String, String> replacement_names = new HashMap<String, String>();
        char next_replacement = 'A';

        for (String w : words) {
            w = w.toLowerCase();
            if (!w.equals(keep_this_name) && names.contains(w) &&
                replacement_names.get(w) == null) {
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
        return case_number.compareTo(other.case_number);
    }
}
