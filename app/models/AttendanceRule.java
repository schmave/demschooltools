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

    public String category;

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

    public static List<AttendanceRule> currentRules(Date date, Integer person_id) {
        com.avaje.ebean.ExpressionList<models.AttendanceRule> result = find.where()
            .eq("organization", Organization.getByHost())
            .or(
                com.avaje.ebean.Expr.eq("start_date", null),
                com.avaje.ebean.Expr.le("start_date", date)
            )
            .or(
                com.avaje.ebean.Expr.eq("end_date", null),
                com.avaje.ebean.Expr.ge("end_date", date)
            );

        if (person_id != null) {
            result = result.or(
                com.avaje.ebean.Expr.eq("person_id", null),
                com.avaje.ebean.Expr.eq("person_id", person_id)
            );
        }

        return result.findList();
    }

    public static List<AttendanceRule> futureRules(Date date) {
        return find
            .where()
            .eq("organization", Organization.getByHost())
            .gt("start_date", date)
            .findList();
    }

    public static List<AttendanceRule> pastRules(Date date) {
        return find
            .where()
            .eq("organization", Organization.getByHost())
            .lt("end_date", date)
            .findList();
    }

    public static void sortCurrentRules(List<AttendanceRule> rules) {
        rules.sort(new Comparator<AttendanceRule>() {
            @Override
            public int compare(AttendanceRule r1, AttendanceRule r2) {
                int categoryComparison = r1.category.compareTo(r2.category);
                if (categoryComparison != 0) {
                    return categoryComparison;
                }
                if (r1.person == null && r2.person != null) {
                    return -1;
                }
                if (r1.person != null && r2.person == null) {
                    return 1;
                }
                if (r1.person != null && r2.person != null) {
                    int nameComparison = r1.person.getDisplayName().compareTo(r2.person.getDisplayName());
                    if (nameComparison != 0) {
                        return nameComparison;
                    }
                }
                return r1.id.compareTo(r2.id);
            }
        });
    }

    public static void sortPastRules(List<AttendanceRule> rules) {
        rules.sort(new Comparator<AttendanceRule>() {
            @Override
            public int compare(AttendanceRule r1, AttendanceRule r2) {
                return r2.end_date.compareTo(r1.end_date);
            }
        });
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
        } else {
            rule.latest_start_time = null;
        }

        rule.category = rule_from_form.category;
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
        if (result.size() > 0) {
            return String.join(",", result);
        }
        return "None";
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

    public boolean doesMatchDaysOfWeek(Date date) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        if (dayOfWeek == 2) return monday;
        if (dayOfWeek == 3) return tuesday;
        if (dayOfWeek == 4) return wednesday;
        if (dayOfWeek == 5) return thursday;
        if (dayOfWeek == 6) return friday;
        return false;
    }
}
