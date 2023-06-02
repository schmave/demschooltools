package models;

import com.fasterxml.jackson.annotation.*;
import io.ebean.*;
import java.math.*;
import java.sql.Time;
import java.text.*;
import java.util.*;
import java.util.stream.*;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import play.data.*;

@Getter
@Setter
@Entity
public class AttendanceRule extends Model {

  @Id private Integer id;

  @ManyToOne() private Organization organization;

  private String category;

  @ManyToOne()
  @JoinColumn(name = "person_id")
  private Person person;

  @play.data.format.Formats.DateTime(pattern = "MM/dd/yyyy")
  private Date startDate;

  @play.data.format.Formats.DateTime(pattern = "MM/dd/yyyy")
  private Date endDate;

  private boolean monday;
  private boolean tuesday;
  private boolean wednesday;
  private boolean thursday;
  private boolean friday;

  private String absenceCode;

  private Double minHours;

  private Time latestStartTime;
  private Time earliestDepartureTime;

  private boolean exemptFromFees;

  public static Finder<Integer, AttendanceRule> find = new Finder<>(AttendanceRule.class);

  public static AttendanceRule findById(Integer id, Organization org) {
    return find.query().where().eq("organization", org).eq("id", id).findOne();
  }

  public static List<AttendanceRule> all(Organization org) {
    return find.query().where().eq("organization", org).findList();
  }

  public static List<AttendanceRule> currentRules(Date date, Integer person_id, Organization org) {
    ExpressionList<models.AttendanceRule> result =
        find.query()
            .where()
            .eq("organization", org)
            .or(Expr.eq("start_date", null), Expr.le("start_date", date))
            .or(Expr.eq("end_date", null), Expr.ge("end_date", date));

    if (person_id != null) {
      result = result.or(Expr.eq("person_id", null), Expr.eq("person_id", person_id));
    }

    return result.findList();
  }

  public static List<AttendanceRule> futureRules(Date date, Organization org) {
    return find.query().where().eq("organization", org).gt("start_date", date).findList();
  }

  public static List<AttendanceRule> pastRules(Date date, Organization org) {
    return find.query().where().eq("organization", org).lt("end_date", date).findList();
  }

  public static void sortCurrentRules(List<AttendanceRule> rules) {
    rules.sort(
        new Comparator<AttendanceRule>() {
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
    rules.sort(
        new Comparator<AttendanceRule>() {
          @Override
          public int compare(AttendanceRule r1, AttendanceRule r2) {
            return r2.endDate.compareTo(r1.endDate);
          }
        });
  }

  public static void save(Form<AttendanceRule> form, Organization org) throws Exception {
    AttendanceRule rule_from_form = form.get();

    AttendanceRule rule =
        rule_from_form.id == null
            ? rule_from_form
            : AttendanceRule.findById(rule_from_form.id, org);

    rule.setOrganization(org);

    String person_id = form.field("personId").value().get();
    if (!person_id.isEmpty()) {
      rule.setPerson(Person.findById(Integer.valueOf(person_id), org));
    } else {
      rule.setPerson(null);
    }

    if (form.field("_latestStartTime").value().isPresent()) {
      String latest_start_time = form.field("_latestStartTime").value().get();
      if (!latest_start_time.isEmpty()) {
        rule.setLatestStartTime(AttendanceDay.parseTime(latest_start_time));
      } else {
        rule.setLatestStartTime(null);
      }
    }

    if (form.field("_earliestDepartureTime").value().isPresent()) {
      String earliest_departure_time = form.field("_earliestDepartureTime").value().get();
      if (!earliest_departure_time.isEmpty()) {
        rule.setEarliestDepartureTime(AttendanceDay.parseTime(earliest_departure_time));
      } else {
        rule.setEarliestDepartureTime(null);
      }
    }

    rule.setCategory(rule_from_form.category);
    rule.setStartDate(rule_from_form.startDate);
    rule.setEndDate(rule_from_form.endDate);
    rule.setMonday(rule_from_form.monday);
    rule.setTuesday(rule_from_form.tuesday);
    rule.setWednesday(rule_from_form.wednesday);
    rule.setThursday(rule_from_form.thursday);
    rule.setFriday(rule_from_form.friday);
    rule.setAbsenceCode(rule_from_form.absenceCode);
    rule.setMinHours(rule_from_form.minHours);
    rule.setExemptFromFees(rule_from_form.exemptFromFees);

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

  public String getFormattedDate(Date d, boolean forDisplay, OrgConfig orgConfig) {
    if (d == null) {
      return forDisplay ? "-" : "";
    }
    if (orgConfig.euro_dates) {
      return new SimpleDateFormat("dd/MM/yyyy").format(d);
    }
    return new SimpleDateFormat("MM/dd/yyyy").format(d);
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
      return code.getColor();
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
