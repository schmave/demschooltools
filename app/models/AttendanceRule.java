package models;

import controllers.Application;
import java.text.*;
import java.util.*;
import java.util.stream.*;
import java.math.*;
import javax.persistence.*;
import java.sql.Time;
import com.fasterxml.jackson.annotation.*;
import play.data.*;
import com.avaje.ebean.*;

@Entity
public class AttendanceRule extends Model {

    @Id
    public Integer id;

    @ManyToOne()
    public Organization organization;

    @ManyToOne()
    @JoinColumn(name="person_id")
    public Person person;

    @play.data.format.Formats.DateTime(pattern="MM/dd/yyyy")
    public Date start_date;

    @play.data.format.Formats.DateTime(pattern="MM/dd/yyyy")
    public Date end_date;

    public String notification_email;

    public boolean expired;

    public boolean monday;
    public boolean tuesday;
    public boolean wednesday;
    public boolean thursday;
    public boolean friday;

    public String absence_code;

    public Double min_hours;

    public Time latest_start_time;
    
    public boolean exempt_from_fees;

    public static Finder<Integer, AttendanceRule> find = new Finder<Integer, AttendanceRule>(
        AttendanceRule.class
    );

    public static AttendanceRule findById(Integer id) {
        return find.where().eq("organization", Organization.getByHost()).eq("id", id).findUnique();
    }

    public static List<AttendanceRule> all() {
        return find.where().eq("organization", Organization.getByHost()).findList();
    }

    public static void save(Form<AttendanceRule> form) throws Exception {
        AttendanceRule rule_from_form = form.get();

        AttendanceRule rule = rule_from_form.id == null 
            ? rule_from_form 
            : AttendanceRule.findById(rule_from_form.id);

        rule.organization = Organization.getByHost();

        String person_id = form.field("person_id").value();
        if (!person_id.isEmpty()) {
            rule.person = Person.findById(Integer.valueOf(person_id));
        } else {
            rule.person = null;
        }

        String _latest_start_time = form.field("_latest_start_time").value();
        if (!_latest_start_time.isEmpty()) {
            rule.latest_start_time = AttendanceDay.parseTime(_latest_start_time);
        }

        rule.start_date = rule_from_form.start_date;
        rule.end_date = rule_from_form.end_date;
        rule.notification_email = rule_from_form.notification_email;
        rule.monday = rule_from_form.monday;
        rule.tuesday = rule_from_form.tuesday;
        rule.wednesday = rule_from_form.wednesday;
        rule.thursday = rule_from_form.thursday;
        rule.friday = rule_from_form.friday;
        rule.absence_code = rule_from_form.absence_code;
        rule.min_hours = rule_from_form.min_hours;
        rule.exempt_from_fees = rule_from_form.exempt_from_fees;

        rule.save();
    }

    public static void delete(Integer id) {
        AttendanceRule rule = find.ref(id);
        rule.delete();
    }

    public String getFormattedPerson() {
        if (person != null) {
            return person.getDisplayName();
        }
        return "Everyone";
    }

    public String getFormattedDate(Date d, boolean forDisplay) {
        if (d == null) {
            return forDisplay ? "-" : "";
        }
        return Application.formatDateMdy(d);
    }

    public String getFormattedDaysOfWeek() {
        List<String> result = new ArrayList<String>();
        if (monday) {
            result.add("M");
        }
        if (tuesday) {
            result.add("T");
        }
        if (wednesday) {
            result.add("W");
        }
        if (thursday) {
            result.add("Th");
        }
        if (friday) {
            result.add("F");
        }
        return String.join(",", result);
    }

    public String getFormattedTime(Time time) {
        if (time == null) {
            return "";
        }
        DateFormat format = new SimpleDateFormat("h:mm a");
        return format.format(time.getTime());
    }

    public String getFormattedBoolean(boolean b) {
        if (b) {
            return "Yes";
        }
        return "";
    }

    public String getCodeColor(Map<String, AttendanceCode> codes_map) {
        AttendanceCode code = codes_map.get(absence_code);
        if (code != null) {
            return code.color;
        }
        return "transparent";
    }
}
