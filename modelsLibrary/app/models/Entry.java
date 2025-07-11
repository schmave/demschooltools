package models;

import com.fasterxml.jackson.annotation.*;
import io.ebean.*;
import io.ebean.FetchConfig;
import java.util.*;
import javax.persistence.*;
import javax.persistence.OrderBy;
import lombok.Getter;
import lombok.Setter;
import play.data.*;

@Getter
@Setter
@Entity
public class Entry extends Model implements Comparable<Entry> {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "entry_id_seq")
  private Integer id;

  private String title = "";
  private String num = "";

  @Column(columnDefinition = "TEXT")
  private String content = "";

  @ManyToOne() private Section section;

  private boolean deleted;

  @OneToMany(mappedBy = "rule")
  @JsonIgnore
  @OrderBy("id DESC")
  public List<Charge> charges;

  @JsonIgnore
  @OneToMany(mappedBy = "entry")
  @OrderBy("dateEntered ASC")
  public List<ManualChange> changes;

  private boolean isBreakingResPlan;

  public static Finder<Integer, Entry> find = new Finder<>(Entry.class);

  public static Entry findById(int id, Organization org) {
    return find.query().where().eq("section.chapter.organization", org).eq("id", id).findOne();
  }

  public static Entry findByIdWithJCData(int id, Organization org) {
    return find.query()
        .fetch("charges", FetchConfig.ofQuery())
        .fetch("charges.theCase", FetchConfig.ofQuery())
        .fetch("charges.theCase.meeting", FetchConfig.ofQuery())
        .fetch("charges.theCase.charges", FetchConfig.ofQuery())
        .fetch("charges.theCase.charges.person", FetchConfig.ofQuery())
        .fetch("charges.theCase.charges.rule", FetchConfig.ofQuery())
        .fetch("charges.theCase.charges.rule.section", FetchConfig.ofQuery())
        .fetch("charges.theCase.charges.rule.section.chapter", FetchConfig.ofQuery())
        .where()
        .eq("section.chapter.organization", org)
        .eq("id", id)
        .findOne();
  }

  public static Entry findBreakingResPlanEntry(Organization org) {
    return find.query()
        .where()
        .eq("section.chapter.organization", org)
        .eq("isBreakingResPlan", true)
        .findOne();
  }

  public static Integer findBreakingResPlanEntryId(Organization org) {
    Entry entry = findBreakingResPlanEntry(org);
    return entry != null ? entry.id : null;
  }

  @JsonIgnore
  public List<Charge> getThisYearCharges() {
    Date beginning_of_year = ModelUtils.getStartOfYear();

    List<Charge> result = new ArrayList<>();
    for (Charge c : charges) {
      if (c.getTheCase().getMeeting().getDate().after(beginning_of_year)) {
        result.add(c);
      }
    }

    return result;
  }

  public String getNumber() {
    return section.getNumber() + "." + num;
  }

  public int compareTo(Entry other) {
    return title.compareTo(other.title);
  }

  public static Comparator<Entry> SORT_NUMBER = Comparator.comparing(Entry::getNumber);
}
