package models;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.*;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;
import play.libs.Json;


@Entity
@Table(name="`case`")
public class Case extends Model {
    @Id
    public String case_number;

    public String location;
    public String findings;

    public Date date;

    @ManyToOne
    @JoinColumn(name="meeting_id")
    public Meeting meeting;

    @ManyToOne
    @JoinColumn(name="writer_id")
    public Person writer;

    public static Finder<String, Case> find = new Finder(
        String.class, Case.class
    );

    public static Case create(String id, Meeting m)
    {
        Case result = new Case();
        result.case_number = id;
        result.meeting = m;
        result.save();

        return result;
    }

    public String toJson() {
        Map<String, String> result = new HashMap<String, String>();
        result.put("id", case_number);
        return Json.stringify(Json.toJson(result));
    }

    public void edit(Map<String, String[]> query_string) {
        findings = query_string.get("findings")[0];
        location = query_string.get("location")[0];

        String date_string = query_string.get("date")[0];
        if (!date_string.equals("")) {
            try
            {
                date = new SimpleDateFormat("yyyy-mm-dd").parse(date_string);
            } catch (ParseException e) {
                date = null;
            }
        } else {
            date = null;
        }

        if (query_string.containsKey("writer_id")) {
            writer = Person.find.ref(Integer.parseInt(query_string.get("writer_id")[0]));
        }
    }
}
