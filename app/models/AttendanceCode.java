package models;

import java.io.*;
import java.sql.Time;
import java.util.*;

import javax.persistence.*;

import com.avaje.ebean.Model;
import com.avaje.ebean.Model.Finder;

import controllers.Application;
import controllers.Utils;

import play.data.Form;
import play.libs.Json;

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

    public static Finder<Integer, AttendanceCode> find = new Finder<Integer, AttendanceCode>(
        AttendanceCode.class
    );

    public static List<AttendanceCode> all(Organization org) {
        return find.where().eq("organization", org).findList();
    }

    public static AttendanceCode findById(Integer id) {
        return find.where()
            .eq("organization", OrgConfig.get().org)
            .eq("id", id)
            .findUnique();
    }

    public static AttendanceCode create(Organization org)
    {
        AttendanceCode result = new AttendanceCode();
        result.organization = org;
        result.save();

        return result;
    }

    public void edit(Form<AttendanceCode> form) {
        if (form.field("delete").value() != null) {
            this.delete();
            return;
        }
        description = form.field("description").value();
        code = form.field("code").value();
        color = form.field("color").value();
        counts_toward_attendance = Utils.getBooleanFromFormValue(form.field("counts_toward_attendance").value());

        this.update();
    }
}
