package models;

import io.ebean.*;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import controllers.Application;
import controllers.Public;
import controllers.Utils;

import javax.persistence.*;
import javax.persistence.OrderBy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

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
    @JsonIgnore
    public List<Case> referenced_cases;

    @ManyToMany(mappedBy="referenced_cases")
    @JsonIgnore
    public List<Case> referencing_cases;

    @ManyToMany
    @JoinTable(name="charge_reference",
        joinColumns=@JoinColumn(name="referencing_case", referencedColumnName="id"),
        inverseJoinColumns=@JoinColumn(name="referenced_charge", referencedColumnName="id"))
    @JsonIgnore
    public List<Charge> referenced_charges;

    @Transient
    public String composite_findings;

    public static Finder<Integer, Case> find = new Finder<>(
            Case.class
    );

    public static Case findById(Integer id) {
        return find.query().where().eq("meeting.organization", Organization.getByHost())
            .eq("id", id).findOne();
    }

    public static List<Case> getOpenCases() {
        return find.query().where()
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

        names = new HashSet<>();

        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(
                    Public.sEnvironment.resourceAsStream("names.txt")));
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
        for(Person p : Application.jcPeople()) {
            names.add(p.first_name.trim().toLowerCase());
            names.add(p.getDisplayName().trim().toLowerCase());
        }
    }

    public String getRedactedFindings(Person keep_this_persons_name) {
        loadNames();

        String composite_findings = generateCompositeFindingsFromChargeReferences();

        String keep_first_name = keep_this_persons_name.first_name.trim().toLowerCase();
        String keep_display_name = keep_this_persons_name.getDisplayName().trim().toLowerCase();

        String[] words = composite_findings.split("\\b");
        Map<String, String> replacement_names = new HashMap<>();
        char next_replacement = 'A';

        for (String w : words) {
            w = w.toLowerCase();
            if (!w.equals(keep_first_name) && !w.equals(keep_display_name) &&
                names.contains(w) && replacement_names.get(w) == null) {
                replacement_names.put(w, next_replacement + "___");

                next_replacement++;
            }
        }

        String new_findings = composite_findings;
        for (String name : replacement_names.keySet()) {
            new_findings = new_findings.replaceAll("(?i)" + name, replacement_names.get(name));
        }

        return new_findings;
    }

    public int compareTo(Case other) {
        return meeting.date.compareTo(other.meeting.date);
    }

    public void addReferencedCase(Case referenced_case) {
        referenced_cases.add(referenced_case);
        // automatically reference all charges by default
        referenced_charges.addAll(referenced_case.charges);
        save();
    }

    public void removeReferencedCase(Case referenced_case) {
        referenced_cases.remove(referenced_case);
        for (Charge charge : referenced_case.charges) {
            referenced_charges.remove(charge);
        }
        for (Charge charge : charges) {
            if (charge.referenced_charge != null && charge.referenced_charge.the_case == referenced_case) {
                charge.referenced_charge = null;
            }
        }
        save();
    }

    // We use this method while editing minutes. At this time the user has picked case references, but is still working
    // on picking charge references, so we don't know which charges will be needed. As a result, we will include
    // information on all charges from the referenced cases.
    public String generateCompositeFindingsFromCaseReferences() {
        if (!OrgConfig.get().org.enable_case_references) {
            return findings;
        }
        return generateCompositeFindings(null, null);
    }

    // We use this method everywhere besides the edit minutes page. At this point the minutes have been finalized and the
    // charge references have been selected. We don't need to include information about charges that weren't referenced.
    public String generateCompositeFindingsFromChargeReferences() {
        if (!OrgConfig.get().org.enable_case_references) {
            return findings;
        }
        ArrayList<Charge> relevant_charges = new ArrayList<>();
        for (Charge charge : charges) {
            charge.buildChargeReferenceChain(relevant_charges);
        }
        return generateCompositeFindings(relevant_charges, null);
    }

    private String generateCompositeFindings(List<Charge> relevant_charges, List<Case> used_cases) {

        if (used_cases == null) {
            used_cases = new ArrayList<>();
        }
        used_cases.add(this);
        String result = "";

        referenced_cases.sort(Comparator.comparing(a -> a.case_number));

        for (Case c : referenced_cases) {
            if (used_cases.contains(c)) {
                continue;
            }
            if (!isCaseRelevant(c, relevant_charges)) {
                continue;
            }
            ArrayList<Case> _used_cases = new ArrayList<>(used_cases);
            boolean hasRelevantReferences = c.referenced_cases.stream()
                .anyMatch(rc -> isCaseRelevant(rc, relevant_charges) && !_used_cases.contains(rc));
            if (!hasRelevantReferences) {
                if (result.isEmpty()) {
                    result += "Per case ";
                } else {
                    result += " Then per case ";
                }
                result += c.case_number + ",";
            }
            if (!result.isEmpty()) {
                result += " ";
            }
            result += c.generateCompositeFindings(relevant_charges, used_cases);
            if (!result.endsWith(".")) {
                result += ".";
            }
            Map<String, List<Charge>> groups = groupChargesByRuleAndResolutionPlan(findRelevantCharges(c, relevant_charges));
            for (Map.Entry<String, List<Charge>> entry : groups.entrySet()) {
                List<Charge> group = entry.getValue();
                result += " " + group.get(0).person.getDisplayName();
                if (group.size() == 1) {
                    result += " was ";    
                } else {
                    if (group.size() > 2) {
                        for (Charge ch : group.subList(1, group.size() - 1)) {
                            result += ", " + ch.person.getDisplayName();
                        }
                        result += ",";
                    }
                    result += " and " + group.get(group.size() - 1).person.getDisplayName() + " were "; 
                }
                result += "charged with " + group.get(0).getRuleTitle();
                String resolution_plan = getResolutionPlanForCompositeFindings(group.get(0));
                if (resolution_plan.isEmpty()) {
                    result += ".";
                } else {
                    if (group.get(0).sm_decision != null && !group.get(0).sm_decision.isEmpty()) {
                        result += " and School Meeting decided on";
                    }
                    else if (group.size() == 1) {
                        result += " and was assigned";
                    }
                    else {
                        result += " and were each assigned";
                    }
                    result += " the " + OrgConfig.get().str_res_plan + " \"" + resolution_plan;
                    if (!result.endsWith(".")) {
                        result += ".";
                    }
                    result += "\"";
                }
            }  
        }
        if (!result.isEmpty()) {
            result += " Then per case " + case_number + ", ";
        }
        if (findings.isEmpty()) {
            findings = "the " + OrgConfig.get().str_res_plan + " was not completed.";
        }
        result += findings;
        return result;
    }

    private static List<Charge> findRelevantCharges(Case c, List<Charge> relevant_charges) {

        List<Charge> charges =
            Charge.find.query()
                .fetch("person")
                .fetch("rule")
                .fetch("rule.section")
                .fetch("rule.section.chapter")
                .where()
                .eq("case_id", c.id)
                .ne("person", null)
                .findList();

        return charges.stream()
            .filter(ch -> relevant_charges == null || relevant_charges.contains(ch))
            .collect(Collectors.toList());
    }

    private static Map<String, List<Charge>> groupChargesByRuleAndResolutionPlan(List<Charge> charges) {
        return charges.stream().collect(Collectors.groupingBy(ch -> ch.getRuleTitle() + getResolutionPlanForCompositeFindings(ch)));
    }

    private static boolean isCaseRelevant(Case c, List<Charge> relevant_charges) {
        return c.charges.stream().anyMatch(ch -> relevant_charges == null || relevant_charges.contains(ch));
    }

    private static String getResolutionPlanForCompositeFindings(Charge charge) {
        if (charge.sm_decision != null && !charge.sm_decision.isEmpty()) {
            return charge.sm_decision;
        }
        if (charge.referred_to_sm && charge.resolution_plan.isEmpty()) {
            return "[Referred to School Meeting]";
        }
        return charge.resolution_plan;
    }
}
