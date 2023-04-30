package models;

import io.ebean.*;
import play.data.Form;

import javax.persistence.*;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class AttendanceCode extends Model {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attendance_code_id_seq")
  private Integer id;

  @ManyToOne() private Organization organization;

  private String code = "";
  private String description = "";
  private String color = "#000000";

  private boolean countsTowardAttendance = false;
  private boolean notCounted = false;

  public static Finder<Integer, AttendanceCode> find = new Finder<>(AttendanceCode.class);

  public static List<AttendanceCode> all(Organization org) {
    return find.query().where().eq("organization", org).findList();
  }

  public static AttendanceCode findById(Integer id, Organization org) {
    return find.query().where().eq("organization", org).eq("id", id).findOne();
  }

  public static AttendanceCode create(Organization org) {
    AttendanceCode result = new AttendanceCode();
    result.organization = org;
    result.save();

    return result;
  }

  public void edit(Form<AttendanceCode> form) {
    if (form.field("delete").value().isPresent()) {
      this.delete();
      return;
    }
    description = form.field("description").value().get();
    code = form.field("code").value().get();
    color = form.field("color").value().get();
    countsTowardAttendance =
        ModelUtils.getBooleanFromFormValue(form.field("countsTowardAttendance"));
    notCounted = ModelUtils.getBooleanFromFormValue(form.field("notCounted"));

    this.update();
  }
}
