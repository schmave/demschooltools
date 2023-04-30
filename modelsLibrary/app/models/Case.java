package models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.ebean.*;
import java.util.*;
import javax.persistence.*;
import javax.persistence.OrderBy;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "`case`")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Case extends Model implements Comparable<Case> {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "case_id_seq")
  private Integer id;

  private String caseNumber;

  private String location = "";
  private String findings = "";

  private Date date;
  private String time = "";

  @ManyToOne
  @JoinColumn(name = "meeting_id")
  @JsonIgnore
  private Meeting meeting;

  @JsonIgnore
  @ManyToMany(mappedBy = "additional_cases")
  public List<Meeting> additional_meetings;

  @OneToMany(mappedBy = "theCase")
  public List<PersonAtCase> people_at_case;

  @OneToMany(mappedBy = "theCase")
  @OrderBy("id ASC")
  public List<Charge> charges;

  private Date dateClosed;

  @ManyToMany
  @JoinTable(
      name = "case_reference",
      joinColumns = @JoinColumn(name = "referencing_case", referencedColumnName = "id"),
      inverseJoinColumns = @JoinColumn(name = "referenced_case", referencedColumnName = "id"))
  @JsonIgnore
  public List<Case> referenced_cases;

  @ManyToMany
  @JoinTable(
      name = "charge_reference",
      joinColumns = @JoinColumn(name = "referencing_case", referencedColumnName = "id"),
      inverseJoinColumns = @JoinColumn(name = "referenced_charge", referencedColumnName = "id"))
  @JsonIgnore
  public List<Charge> referenced_charges;

  @Transient private String compositeFindings;

  public static Finder<Integer, Case> find = new Finder<>(Case.class);

  public static Case findById(Integer id, Organization org) {
    return find.query().where().eq("meeting.organization", org).eq("id", id).findOne();
  }

  public static List<Case> getOpenCases(Organization org) {
    return find.query()
        .where()
        .eq("meeting.organization", org)
        .eq("dateClosed", null)
        .order("caseNumber ASC")
        .findList();
  }

  public static Case create(String number, Meeting m) {
    Case result = new Case();
    result.caseNumber = number;
    result.meeting = m;
    result.dateClosed = m.getDate(); // closed by default
    result.save();

    return result;
  }

  public void continueInMeeting(Meeting m) {
    try {
      // remember the previous meeting
      this.meeting.additional_cases.add(this);
      this.meeting.save();
    } catch (PersistenceException pe) {
      // Throw the exception only if this is something other than a
      // "unique_violation", meaning that this CaseMeeting already exists.
      ModelUtils.eatIfUniqueViolation(pe);
    }

    this.meeting = m;

    this.update();
  }

  public void edit(Map<String, String[]> query_string) {
    findings = query_string.get("findings")[0];
    location = query_string.get("location")[0];

    String date_string = query_string.get("date")[0];
    date = ModelUtils.getDateFromString(date_string);
    time = query_string.get("time")[0];

    System.out.println("closed: " + query_string.get("closed")[0]);
    if (query_string.get("closed")[0].equals("true")) {
      dateClosed = meeting.getDate();
    } else {
      dateClosed = null;
    }
    this.update();
  }

  public boolean empty() {
    return findings.equals("")
        && location.equals("")
        && date == null
        && time.equals("")
        && people_at_case.size() == 0
        && charges.size() == 0;
  }

  public int compareTo(Case other) {
    return meeting.getDate().compareTo(other.meeting.getDate());
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
      if (charge.getReferencedCharge() != null
          && charge.getReferencedCharge().getTheCase() == referenced_case) {
        charge.setReferencedCharge(null);
      }
    }
    save();
  }
}
