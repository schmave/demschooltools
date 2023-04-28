package models;

import com.fasterxml.jackson.annotation.*;
import io.ebean.*;
import java.util.*;
import java.util.Date;
import java.util.Map;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
public class Charge extends Model implements Comparable<Charge> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "charge_id_seq")
    private Integer id;

    @ManyToOne
    @JoinColumn(name="person_id")
    private Person person;

    @ManyToOne
    @JoinColumn(name="rule_id")
    private Entry rule;

    @ManyToOne
    @JoinColumn(name="case_id")
    private Case theCase;

    @ManyToOne()
    @JoinColumn(name="referenced_charge_id")
    @JsonIgnore
    private Charge referencedCharge;

    @OneToMany(mappedBy="referencedCharge")
    @JsonIgnore
    public List<Charge> referencing_charges;

    @ManyToMany(mappedBy="referenced_charges")
    @JsonIgnore
    public List<Case> referencing_cases;

    @Transient
    private boolean isReferenced;

    static final String EMPTY_PLEA = "<no plea>";
    private String plea = EMPTY_PLEA;
    private String resolutionPlan = "";

    private boolean referredToSm;
    private String smDecision;
    private Date smDecisionDate;

    private boolean rpComplete = false;

    private Date rpCompleteDate = null;

    private String severity = "";

    private String minorReferralDestination = "";

    public static Finder<Integer, Charge> find = new Finder<>(Charge.class);

    public static Charge findById(int id, Organization org) {
        return find.query().where().eq("theCase.meeting.organization", org)
            .eq("id", id).findOne();
    }

    public static Charge create(Case c)
    {
        Charge result = new Charge();
        result.theCase = c;
        result.save();

        return result;
    }

    public static Charge generateFromReference(Case c, Charge referencedCharge, Organization org)
    {
        Charge result = new Charge();
        result.theCase = c;
        result.person = referencedCharge.person;
        result.rule = Entry.findBreakingResPlanEntry(org);
        result.referencedCharge = referencedCharge;
        result.referredToSm = referencedCharge.referredToSm && referencedCharge.smDecision == null;
        result.save();
        return result;
    }

    public void edit(Map<String, String[]> query_string) {
        resolutionPlan = query_string.get("resolutionPlan")[0];

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

        if (query_string.containsKey("personId")) {
            person = Person.find.ref(Integer.parseInt(query_string.get("personId")[0]));
        } else {
            person = null;
        }

        if (query_string.containsKey("rule_id")) {
            rule = Entry.find.ref(Integer.parseInt(query_string.get("rule_id")[0]));
        } else {
            rule = null;
        }

        referredToSm = Boolean.parseBoolean(query_string.get("referredToSm")[0]);

        if (query_string.containsKey("minorReferralDestination")) {
            minorReferralDestination = query_string.get("minorReferralDestination")[0];
        }
    }

    public boolean displayInResolutionPlanList() {
        return this.person != null && this.rule != null
			&& !this.referredToSm && !this.plea.equals("Not Guilty");
    }

    public void updateSchoolMeetingDecision(String decision, Date date) {
        decision = decision.trim();
        if (!decision.equals(this.smDecision)) {
            this.rpComplete = false;
            this.rpCompleteDate = null;
        }

        this.smDecisionDate = date;

        if (!decision.equals("")) {
            this.smDecision = decision;
            // If a decision is recorded, assume it was done today
            // if not otherwise specified.
            if (this.smDecisionDate == null) {
                this.smDecisionDate = new Date();
            }
        } else {
            this.smDecision = null;
        }
    }

    public void setRPComplete(boolean complete) {
        this.rpComplete = complete;
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
            return this.rule.getNumber() + " " + this.rule.getTitle();
        }
    }

    @Override
    public int compareTo(@javax.annotation.Nonnull Charge c2) {
		if (theCase.getMeeting().getDate() != null) {
			return theCase.getMeeting().getDate().compareTo(c2.theCase.getMeeting().getDate());
		}
		return 0;
    }

    public String getDayOfWeek() {
        if (smDecisionDate != null) {
            return ModelUtils.formatDayOfWeek(smDecisionDate) + "&mdash;SM";
        } else {
            if (theCase != null && theCase.getMeeting() != null) {
                return ModelUtils.formatDayOfWeek(theCase.getMeeting().getDate());
            } else {
                return null;
            }
        }
    }

    public void buildChargeReferenceChain(ArrayList<Charge> chain) {
        if (referencedCharge != null) {
            referencedCharge.buildChargeReferenceChain(chain);
        }
        chain.add(this);
    }
}