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

@Getter
@Setter
@Entity
public class Role extends Model {

  @Id private Integer id;

  @ManyToOne()
  private Organization organization;

  @OneToMany(mappedBy = "role")
  @JsonIgnore
  public List<RoleRecord> records;

  private RoleType type;
  private RoleEligibility eligibility;

  private String name = "";
  private String notes = "";
  private String description = "";

  private boolean isActive;

  public String getTypeName() {
    return type.toString(organization);
  }

  private static final Finder<Integer, Role> find = new Finder<>(Role.class);

  public static List<Role> all(Organization org) {
    return find.query().where().eq("organization", org).findList();
  }

  public static Role findById(Integer id, Organization org) {
    return find.query().where().eq("organization", org).eq("id", id).findOne();
  }

  public static Role create(Organization org, RoleType type, RoleEligibility eligibility, String name, String notes, String description) {
    Role role = new Role();
    role.organization = org;
    role.type = type;
    role.eligibility = eligibility;
    role.name = name;
    role.notes = notes;
    role.description = description;
    role.isActive = true;
    role.save();
    return role;
  }
}
