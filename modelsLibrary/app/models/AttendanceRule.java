package models;

import io.ebean.*;
import java.text.*;
import java.util.*;
import java.util.stream.*;
import java.math.*;
import javax.persistence.*;
import java.sql.Time;
import com.fasterxml.jackson.annotation.*;
import play.data.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class AttendanceRule extends Model {

    @Id
    private Integer id;

    @ManyToOne()
    private Organization organization;

    private String category;

    @ManyToOne()
    @JoinColumn(name="person_id")
    private Person person;

    @play.data.format.Formats.DateTime(pattern="MM/dd/yyyy")
    private Date startDate;

    @play.data.format.Formats.DateTime(pattern="MM/dd/yyyy")
    private Date endDate;

    private String notificationEmail;

    private boolean expired;

    private boolean monday;
    private boolean tuesday;
    private boolean wednesday;
    private boolean thursday;
    private boolean friday;

    private String absenceCode;

    private Double minHours;

    private Time latestStartTime;
    
    private boolean exemptFromFees;

    public static Finder<Integer, AttendanceRule> find = new Finder<>(AttendanceRule.class);

    public static AttendanceRule findById(Integer id, Organization org) {
        return find.query().where().eq("organization", org).eq("id", id).findOne();
    }

    public static List<AttendanceRule> all(Organization org) {
        return find.query().where().eq("organization", org).findList();
    }

    public static List<AttendanceRule> currentRules(Date date, Integer person_id, Organization org) {
        ExpressionList<models.AttendanceRule> result = find.query().where()
            .eq("organization", org)
            .or(
                Expr.eq("start_date", null),
                Expr.le("start_date", date)
            )
            .or(
                Expr.eq("end_date", null),
                Expr.ge("end_date", date)
            );

        if (person_id != null) {
            result = result.or(
                Expr.eq("person_id", null),
                Expr.eq("person_id", person_id)
            );
        }

        return result.findList();
    }

    public static List<AttendanceRule> futureRules(Date date, Organization org) {
        return find
            .query()
            .where()
            .eq("organization", org)
            .gt("start_date", date)
            .findList();
    }

    public static List<AttendanceRule> pastRules(Date date, Organization org) {
        return find
            .query()
            .where()
            .eq("organization", org)
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
                return r2.endDate.compareTo(r1.endDate);
            }
        });
    }

    public static void save(Form<AttendanceRule> form, Organization org) throws Exception {
        AttendanceRule rule_from_form = form.get();

        AttendanceRule rule = rule_from_form.id == null 
            ? rule_from_form 
            : AttendanceRule.findById(rule_from_form.id, org);

        rule.organization = org;

        String person_id = form.field("person_id").value();
        if (!person_id.isEmpty()) {
            rule.person = Person.findById(Integer.valueOf(person_id));
        } else {
            rule.person = null;
        }

        String _latest_start_time = form.field("_latest_start_time").value();
        if (!_latest_start_time.isEmpty()) {
            rule.latestStartTime = AttendanceDay.parseTime(_latest_start_time);
        } else {
            rule.latestStartTime = null;
        }

        rule.category = rule_from_form.category;
        rule.startDate = rule_from_form.startDate;
        rule.endDate = rule_from_form.endDate;
        rule.notificationEmail = rule_from_form.notificationEmail;
        rule.monday = rule_from_form.monday;
        rule.tuesday = rule_from_form.tuesday;
        rule.wednesday = rule_from_form.wednesday;
        rule.thursday = rule_from_form.thursday;
        rule.friday = rule_from_form.friday;
        rule.absenceCode = rule_from_form.absenceCode;
        rule.minHours = rule_from_form.minHours;
        rule.exemptFromFees = rule_from_form.exemptFromFees;

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
        AttendanceCode code = codes_map.get(absenceCode);
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
