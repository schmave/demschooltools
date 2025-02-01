package models;

import com.fasterxml.jackson.annotation.*;
import io.ebean.*;
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
public class RoleRecordMember extends Model {

  @ManyToOne() @JsonIgnore private RoleRecord record;

  @ManyToOne()
  @JoinColumn(name = "person_id")
  @JsonIgnore
  private Person person;

  private String personName;

  private RoleRecordMemberType type;

  @JsonInclude
  public Integer getPersonId() {
    return person != null ? person.getPersonId() : null;
  }

  public static RoleRecordMember create(
      RoleRecord record, Person person, String personName, RoleRecordMemberType type) {
    RoleRecordMember member = new RoleRecordMember();
    member.record = record;
    member.person = person;
    member.personName = personName;
    member.type = type;
    member.save();
    return member;
  }
}
