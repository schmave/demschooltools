package models;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;
import play.libs.Json;

@Entity
@Table(name="`case`")
public class Case extends Model {
    @Id
    public String case_number;

    public String location = "";
    public String findings = "";

    public Date date;

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

    public static Finder<String, Case> find = new Finder(
        String.class, Case.class
    );

    public static Case findById(String id) {
        return find.where().eq("meeting.organization", Organization.getByHost())
            .eq("case_number", id).findUnique();
    }

    public static Case create(String id, Meeting m)
    {
        Case result = new Case();
        result.case_number = id;
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
			testify_records.size() == 0 &&
			charges.size() == 0;
	}
}
