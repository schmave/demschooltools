package models;

import com.fasterxml.jackson.annotation.*;
import io.ebean.*;
import io.ebean.Query;
import java.math.*;
import java.text.*;
import java.util.*;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import play.data.*;
import java.text.SimpleDateFormat;

@Getter
@Setter
@Entity
public class Role extends Model {

  @Id private Integer id;

  @ManyToOne()
  @JsonIgnore
  private Organization organization;

  @OneToMany(mappedBy = "role")
  public List<RoleRecord> records;

  private boolean isActive;

  private RoleType type;
  private RoleEligibility eligibility;

  private String name = "";
  private String notes = "";
  private String description = "";

  public String getTypeName() {
    return type.toString(organization);
  }

  private static final Finder<Integer, Role> find = new Finder<>(Role.class);

  public static List<Role> all(Organization org) {
    return find.query()
      .fetch("records", FetchConfig.ofQuery())
      .fetch("records.members", FetchConfig.ofQuery())
      .where()
      .eq("organization", org)
      .eq("is_active", true)
      .findList();
  }

  public static Role findById(Integer id, Organization org) {
    return find.query().where().eq("organization", org).eq("id", id).findOne();
  }

  public static Role create(
    Organization org,
    RoleType type,
    RoleEligibility eligibility,
    String name,
    String notes,
    String description
  ) {
    Role role = new Role();
    role.organization = org;
    role.type = type;
    role.eligibility = eligibility;
    role.name = name;
    role.notes = notes;
    role.description = description;
    role.save();
    return role;
  }

  public void update(
    RoleEligibility eligibility,
    String name,
    String notes,
    String description,
    List<Map.Entry<Integer, String>> chairs,
    List<Map.Entry<Integer, String>> backups,
    List<Map.Entry<Integer, String>> members
  ) {
    this.eligibility = eligibility;
    this.name = name;
    this.notes = notes;
    this.description = description;
    save();
    RoleRecord record = findOrCreateCurrentRecord();
    record.addMembers(chairs, backups, members);
  }

  public void deactivate() {
    this.is_active = false;
    RoleRecord record = findOrCreateCurrentRecord();
    save();
  }

  private RoleRecord findOrCreateCurrentRecord() {
    if (!records.isEmpty()) {
      Collections.sort(records, Collections.reverseOrder());
      RoleRecord record = records.get(0);
      if (areSameDay(record.getDateCreated(), new Date())) {
        record.clearMembers();
        return record;
      }
    }
    return RoleRecord.create(this);
  }

  private static boolean areSameDay(Date a, Date b) {
    SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
    return fmt.format(a).equals(fmt.format(b));
  }
}
