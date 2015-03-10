package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.*;

import com.avaje.ebean.validation.NotNull;
import com.fasterxml.jackson.annotation.*;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;

@Entity
public class Charge extends Model implements Comparable<Charge> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "charge_id_seq")
    public Integer id;

    @ManyToOne
    @JoinColumn(name="person_id")
    public Person person;

    @ManyToOne
    @JoinColumn(name="rule_id")
    public Entry rule;

    @ManyToOne
    @JoinColumn(name="case_id")
    @JsonIgnore
    public Case the_case;

    static final String EMPTY_PLEA = "<no plea>";
    public String plea = EMPTY_PLEA;
    public String resolution_plan = "";

    public boolean referred_to_sm;
    public String sm_decision;
    public Date sm_decision_date;

    @NotNull
    public String severity = "";

    @NotNull
    public String minor_referral_destination = "";

    public static Finder<Integer, Charge> find = new Finder(
        Integer.class, Charge.class
    );

    public static Charge findById(int id) {
        return find.where().eq("the_case.meeting.organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }

    public static Charge create(Case c)
    {
        Charge result = new Charge();
        result.the_case = c;
        result.save();

        return result;
    }

    public void edit(Map<String, String[]> query_string) {
        resolution_plan = query_string.get("resolution_plan")[0];

        if (query_string.containsKey("plea")) {
            plea = query_string.get("plea")[0];
        } else {
            plea = "";
        }

        if (plea.equals("")) {
            plea = EMPTY_PLEA;
        }

		if (query_string.containsKey("severity")) {
            severity = query_string.get("severity")[0];
        }

        if (query_string.containsKey("person_id")) {
            person = Person.find.ref(Integer.parseInt(query_string.get("person_id")[0]));
        } else {
            person = null;
        }

        if (query_string.containsKey("rule_id")) {
            rule = Entry.find.ref(Integer.parseInt(query_string.get("rule_id")[0]));
        } else {
            rule = null;
        }

        referred_to_sm = Boolean.parseBoolean(query_string.get("referred_to_sm")[0]);

        if (query_string.containsKey("minor_referral_destination")) {
            minor_referral_destination = query_string.get("minor_referral_destination")[0];
        }
    }

    public boolean displayInResolutionPlanList() {
        return this.person != null && this.rule != null
			&& !this.referred_to_sm
			&& !(this.resolution_plan.trim().toLowerCase().equals("warning") ||
				 this.resolution_plan.trim().toLowerCase().equals("warning."))
			&& !this.plea.equals("Not Guilty");
    }

    public void updateSchoolMeetingDecision(String decision, Date date) {
        decision = decision.trim();
        if (!decision.equals("")) {
            this.sm_decision = decision;
        }
        this.sm_decision_date = date;
    }

    @Override
    public int compareTo(Charge c2) {
		if (the_case.meeting.date != null) {
			return the_case.meeting.date.compareTo(c2.the_case.meeting.date);
		}
		return 0;
    }
}
