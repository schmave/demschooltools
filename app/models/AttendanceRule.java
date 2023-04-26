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

    public static AttendanceRule create(Form<AttendanceRule> form) throws Exception {
        AttendanceRule rule = form.get();

        String person_id = form.field("person_id").value();
        if (!person_id.isEmpty()) {
            rule.person = Person.findById(Integer.valueOf(person_id));
        }

        String _latest_start_time = form.field("_latest_start_time").value();
        if (!_latest_start_time.isEmpty()) {
            rule.latest_start_time = AttendanceDay.parseTime(_latest_start_time);
        }

        rule.organization = Organization.getByHost();

        rule.save();
        return rule;
    }

    public static void delete(Integer id) {
        AttendanceRule rule = find.ref(id);
        rule.delete();
    }

    public void updateFromForm(Form<AttendanceRule> form) throws Exception {
        save();
    }

    public String getFormattedPerson() {
        if (person != null) {
            return person.getDisplayName();
        }
        return "Everyone";
    }

    public String getFormattedDate(Date d) {
        if (d == null) {
            return "-";
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
        return "No";
    }

    public String getCodeColor(Map<String, AttendanceCode> codes_map) {
        AttendanceCode code = codes_map.get(absence_code);
        if (code != null) {
            return code.color;
        }
        return "transparent";
    }
}
