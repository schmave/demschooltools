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
public class RoleRecord extends Model {

  @Id private Integer id;

  @ManyToOne()
  private Role role;

  @OneToMany(mappedBy = "record")
  @JsonIgnore
  public List<RoleRecordMember> members;

  private String roleName = "";
  
  @play.data.format.Formats.DateTime(pattern = "MM/dd/yyyy")
  private Date dateCreated = new Date();

  public static RoleRecord create(Role role) {
    RoleRecord record = new RoleRecord();
    record.role = role;
    record.roleName = role.getName();
    record.save();
    return record;
  }
}
