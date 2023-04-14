package models;

import io.ebean.*;
import controllers.Utils;
import play.data.Form;

import javax.persistence.*;
import java.util.List;

@Entity
public class AttendanceCode extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attendance_code_id_seq")
    public Integer id;

    @ManyToOne()
    public Organization organization;

    public String code = "";
    public String description = "";
    public String color="#000000";

    public boolean counts_toward_attendance = false;
    public boolean not_counted = false;

    public static Finder<Integer, AttendanceCode> find = new Finder<>(
            AttendanceCode.class
    );

    public static List<AttendanceCode> all(Organization org) {
        return find.query().where().eq("organization", org).findList();
    }

    public static AttendanceCode findById(Integer id) {
        return find.query().where()
            .eq("organization", OrgConfig.get().org)
            .eq("id", id)
            .findOne();
    }

    public static AttendanceCode create(Organization org)
    {
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
        counts_toward_attendance = Utils.getBooleanFromFormValue(form.field("counts_toward_attendance"));
        not_counted = Utils.getBooleanFromFormValue(form.field("not_counted"));

        this.update();
    }
}
