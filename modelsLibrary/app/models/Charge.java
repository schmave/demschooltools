package models;

import com.fasterxml.jackson.annotation.*;
import io.ebean.*;
import java.util.*;
import java.util.Date;
import java.util.Map;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Setter;

@Entity
@JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
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
    public Case the_case;

    @ManyToOne()
    @JoinColumn(name="referenced_charge_id")
    @JsonIgnore
    public Charge referenced_charge;

    @OneToMany(mappedBy="referenced_charge")
    @JsonIgnore
    public List<Charge> referencing_charges;

    @ManyToMany(mappedBy="referenced_charges")
    @JsonIgnore
    public List<Case> referencing_cases;

    @Transient
    public boolean is_referenced;

    static final String EMPTY_PLEA = "<no plea>";
    public String plea = EMPTY_PLEA;
    public String resolution_plan = "";

    public boolean referred_to_sm;
    public String sm_decision;
    public Date sm_decision_date;

    public boolean rp_complete = false;

    @Setter(AccessLevel.NONE)
    private Date rpCompleteDate = null;

    public String severity = "";

    public String minor_referral_destination = "";

    public static Finder<Integer, Charge> find = new Finder<>(Charge.class);

    public static Charge findById(int id, Organization org) {
        return find.query().where().eq("the_case.meeting.organization", org)
            .eq("id", id).findOne();
    }

    public static Charge create(Case c)
    {
        Charge result = new Charge();
        result.the_case = c;
        result.save();

        return result;
    }

    public static Charge generateFromReference(Case c, Charge referenced_charge, Organization org)
    {
        Charge result = new Charge();
        result.the_case = c;
        result.person = referenced_charge.person;
        result.rule = Entry.findBreakingResPlanEntry(org);
        result.referenced_charge = referenced_charge;
        result.referred_to_sm = referenced_charge.referred_to_sm && referenced_charge.sm_decision == null;
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
			&& !this.referred_to_sm && !this.plea.equals("Not Guilty");
    }

    public void updateSchoolMeetingDecision(String decision, Date date) {
        decision = decision.trim();
        if (!decision.equals(this.sm_decision)) {
            this.rp_complete = false;
            this.rpCompleteDate = null;
        }

        this.sm_decision_date = date;

        if (!decision.equals("")) {
            this.sm_decision = decision;
            // If a decision is recorded, assume it was done today
            // if not otherwise specified.
            if (this.sm_decision_date == null) {
                this.sm_decision_date = new Date();
            }
        } else {
            this.sm_decision = null;
        }
    }

    public void setRPComplete(boolean complete) {
        this.rp_complete = complete;
        if (complete) {
            this.rpCompleteDate = new Date();
        } else {
            this.rpCompleteDate = null;
        }
        this.save();
    }

    public String getRuleTitle() {
        if (this.rule == null) {
            return "<< no rule >>";
        } else {
            return this.rule.getNumber() + " " + this.rule.title;
        }
    }

    @Override
    public int compareTo(@javax.annotation.Nonnull Charge c2) {
		if (the_case.meeting.date != null) {
			return the_case.meeting.date.compareTo(c2.the_case.meeting.date);
		}
		return 0;
    }

    public String getDayOfWeek() {
        if (sm_decision_date != null) {
            return ModelUtils.formatDayOfWeek(sm_decision_date) + "&mdash;SM";
        } else {
            if (the_case != null && the_case.meeting != null) {
                return ModelUtils.formatDayOfWeek(the_case.meeting.date);
            } else {
                return null;
            }
        }
    }

    public void buildChargeReferenceChain(ArrayList<Charge> chain) {
        if (referenced_charge != null) {
            referenced_charge.buildChargeReferenceChain(chain);
        }
        chain.add(this);
    }
}
