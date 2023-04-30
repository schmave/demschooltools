package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.ebean.*;
import java.math.BigDecimal;
import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Organization extends Model {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "organization_id_seq")
  private Integer id;

  private String name;

  private String printerEmail;

  private Integer jcResetDay;
  private Boolean showLastModifiedInPrint;
  private Boolean showHistoryInPrint;

  private String shortName;
  private String custodiaPassword;
  private Boolean showCustodia;
  private Boolean showAttendance;
  private Boolean showElectronicSignin;
  private Boolean showAccounting;

  private Boolean enableCaseReferences;

  private Boolean attendanceEnableOffCampus;
  private Boolean attendanceShowReports;
  private Time attendanceReportLatestDepartureTime;
  private Integer attendanceReportLateFee;
  private Integer attendanceReportLateFeeInterval;
  private Boolean attendanceShowPercent;
  private Boolean attendanceShowWeightedPercent;
  private Boolean attendanceEnablePartialDays;
  private Time attendanceDayLatestStartTime;
  private Double attendanceDayMinHours;
  private BigDecimal attendancePartialDayValue;
  private String attendanceAdminPin;

  @OneToMany(mappedBy = "organization")
  @JsonIgnore
  public List<NotificationRule> notification_rules;

  public static Finder<Integer, Organization> find = new Finder<>(Organization.class);

  public String formatAttendanceLatestStartTime() {
    if (attendanceDayLatestStartTime == null) {
      return "";
    }
    DateFormat format = new SimpleDateFormat("h:mm a");
    return format.format(attendanceDayLatestStartTime.getTime());
  }

  public String formatAttendanceReportLatestDepartureTime() {
    if (attendanceReportLatestDepartureTime == null) {
      return "";
    }
    DateFormat format = new SimpleDateFormat("h:mm a");
    return format.format(attendanceReportLatestDepartureTime.getTime());
  }

  public void updateFromForm(Map<String, String[]> values, Organization org) {
    if (values.containsKey("jcResetDay")) {
      this.jcResetDay = Integer.parseInt(values.get("jcResetDay")[0]);
    }
    if (values.containsKey("case_reference_settings")) {
      if (values.containsKey("enableCaseReferences")) {
        this.enableCaseReferences =
            ModelUtils.getBooleanFromFormValue(values.get("enableCaseReferences")[0]);
      } else {
        this.enableCaseReferences = false;
      }
      if (values.containsKey("breaking_res_plan_entry_id")) {
        Entry entry1 = Entry.findBreakingResPlanEntry(this);
        if (entry1 != null) {
          entry1.setIsBreakingResPlan(false);
          entry1.save();
        }
        String idString = values.get("breaking_res_plan_entry_id")[0];
        if (!idString.isEmpty()) {
          Entry entry = Entry.findById(Integer.parseInt(idString), this);
          entry.setIsBreakingResPlan(true);
          entry.save();
        }
      }
    }
    if (values.containsKey("manual_settings")) {
      if (values.containsKey("showLastModifiedInPrint")) {
        this.showLastModifiedInPrint =
            ModelUtils.getBooleanFromFormValue(values.get("showLastModifiedInPrint")[0]);
      } else {
        this.showLastModifiedInPrint = false;
      }
      if (values.containsKey("showHistoryInPrint")) {
        this.showHistoryInPrint =
            ModelUtils.getBooleanFromFormValue(values.get("showHistoryInPrint")[0]);
      } else {
        this.showHistoryInPrint = false;
      }
    }
    if (values.containsKey("attendance_settings")) {
      if (values.containsKey("showAttendance")) {
        this.showAttendance = ModelUtils.getBooleanFromFormValue(values.get("showAttendance")[0]);
      } else {
        this.showAttendance = false;
      }
      if (values.containsKey("showElectronicSignin")) {
        this.showElectronicSignin =
            ModelUtils.getBooleanFromFormValue(values.get("showElectronicSignin")[0]);
      } else {
        this.showElectronicSignin = false;
      }
      if (values.containsKey("attendanceEnableOffCampus")) {
        this.attendanceEnableOffCampus =
            ModelUtils.getBooleanFromFormValue(values.get("attendanceEnableOffCampus")[0]);
      } else {
        this.attendanceEnableOffCampus = false;
      }
      if (values.containsKey("attendanceShowReports")) {
        this.attendanceShowReports =
            ModelUtils.getBooleanFromFormValue(values.get("attendanceShowReports")[0]);
      } else {
        this.attendanceShowReports = false;
      }
      if (values.containsKey("attendanceShowPercent")) {
        this.attendanceShowPercent =
            ModelUtils.getBooleanFromFormValue(values.get("attendanceShowPercent")[0]);
      } else {
        this.attendanceShowPercent = false;
      }
      if (values.containsKey("attendanceShowWeightedPercent")) {
        this.attendanceShowWeightedPercent =
            ModelUtils.getBooleanFromFormValue(values.get("attendanceShowWeightedPercent")[0]);
      } else {
        this.attendanceShowWeightedPercent = false;
      }
      if (values.containsKey("attendanceEnablePartialDays")) {
        this.attendanceEnablePartialDays =
            ModelUtils.getBooleanFromFormValue(values.get("attendanceEnablePartialDays")[0]);
      } else {
        this.attendanceEnablePartialDays = false;
      }
      if (values.containsKey("showCustodia")) {
        this.showCustodia = ModelUtils.getBooleanFromFormValue(values.get("showCustodia")[0]);
      } else {
        this.showCustodia = false;
      }
      if (!values.containsKey("attendanceDayMinHours")
          || values.get("attendanceDayMinHours")[0].isEmpty()) {
        this.attendanceDayMinHours = null;
      } else {
        this.attendanceDayMinHours = Double.parseDouble(values.get("attendanceDayMinHours")[0]);
      }
      if (!values.containsKey("attendancePartialDayValue")
          || values.get("attendancePartialDayValue")[0].isEmpty()) {
        this.attendancePartialDayValue = null;
      } else {
        this.attendancePartialDayValue = new BigDecimal(values.get("attendancePartialDayValue")[0]);
      }
      if (values.containsKey("attendanceDayLatestStartTime")) {
        this.attendanceDayLatestStartTime =
            AttendanceDay.parseTime(values.get("attendanceDayLatestStartTime")[0]);
      } else {
        this.attendanceDayLatestStartTime = null;
      }
      if (values.containsKey("attendanceReportLatestDepartureTime")) {
        this.attendanceReportLatestDepartureTime =
            AttendanceDay.parseTime(values.get("attendanceReportLatestDepartureTime")[0]);
      } else {
        this.attendanceReportLatestDepartureTime = null;
      }
      if (!values.containsKey("attendanceReportLateFee")
          || values.get("attendanceReportLateFee")[0].isEmpty()) {
        this.attendanceReportLateFee = null;
      } else {
        this.attendanceReportLateFee = Integer.parseInt(values.get("attendanceReportLateFee")[0]);
      }
      if (!values.containsKey("attendanceReportLateFeeInterval")
          || values.get("attendanceReportLateFeeInterval")[0].isEmpty()) {
        this.attendanceReportLateFeeInterval = null;
      } else {
        this.attendanceReportLateFeeInterval =
            Integer.parseInt(values.get("attendanceReportLateFeeInterval")[0]);
      }
    }
    if (values.containsKey("accounting_settings")) {
      if (values.containsKey("showAccounting")) {
        this.showAccounting = ModelUtils.getBooleanFromFormValue(values.get("showAccounting")[0]);
        if (this.showAccounting) {
          Account.createPersonalAccounts(org);
        }
      } else {
        this.showAccounting = false;
      }
    }
    this.save();
  }
}
