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
public class RoleRecord extends Model implements Comparable<RoleRecord> {

  @Id private Integer id;

  @ManyToOne()
  @JsonIgnore
  private Role role;

  @OneToMany(mappedBy = "record")
  public List<RoleRecordMember> members = new ArrayList<RoleRecordMember>();

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

  public void clearMembers() {
    String sql = "DELETE FROM role_record_member WHERE record_id=:id";
    SqlUpdate update = DB.sqlUpdate(sql);
    update.setParameter("id", id);
    update.execute();
    members = new ArrayList<RoleRecordMember>();
  }

  public void addMembers(
    List<Map.Entry<Integer, String>> chairs,
    List<Map.Entry<Integer, String>> backups,
    List<Map.Entry<Integer, String>> members
  ) {
    // usedIds prevents us from adding a person multiple times with different member types.
    // More important member types are done first so they take precedence.
    List<Integer> usedIds = new ArrayList<Integer>();
    usedIds.addAll(addMembersOfType(chairs, RoleRecordMemberType.Chair, usedIds));
    usedIds.addAll(addMembersOfType(backups, RoleRecordMemberType.Backup, usedIds));
    addMembersOfType(members, RoleRecordMemberType.Member, usedIds);
  }

  private List<Integer> addMembersOfType(List<Map.Entry<Integer, String>> members, RoleRecordMemberType type, List<Integer> usedIds) {
    List<Integer> personIds = new ArrayList<Integer>();
    for (Map.Entry<Integer, String> item : members) {
      Integer personId = item.getKey();
      if (usedIds.contains(personId)) {
        continue;
      }
      String personName = item.getValue();
      Person person = Person.findById(personId, role.getOrganization());
      if (person != null) {
        personName = person.getDisplayName();
        personIds.add(personId);
      }
      RoleRecordMember.create(this, person, personName, type);
    }
    return personIds;
  }

  @Override
  public int compareTo(RoleRecord r) {
    return getDateCreated().compareTo(r.getDateCreated());
  }
}
