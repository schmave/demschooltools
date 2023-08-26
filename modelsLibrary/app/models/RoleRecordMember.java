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
public class RoleRecordMember extends Model {

  @Id private Integer id;

  @ManyToOne()
  private RoleRecord record;

  @ManyToOne()
  @JoinColumn(name = "person_id")
  private Person person;

  private String personName;

  private RoleRecordMemberType type;

  public static RoleRecordMember create(RoleRecord record, Person person, RoleRecordMemberType type) {
    RoleRecordMember member = new RoleRecordMember();
    member.record = record;
    member.person = person;
    member.type = type;
    member.save();
    return member;
  }
}
