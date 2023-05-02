package models;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class OrgConfig {
  public String name;
  public String people_url;
  public String str_manual_title;
  public String str_manual_title_short;
  public String str_res_plan_short;
  public String str_res_plan;
  public String str_res_plan_cap;
  public String str_res_plans;
  public String str_res_plans_cap;
  public String str_jc_name = "Judicial Committee";
  public String str_jc_name_short = "JC";
  public String str_findings = "Findings";
  public String str_corporation = "Corporation";
  public String str_committee = "Committee";
  public String str_clerk = "Clerk";
  public String str_guilty = "Guilty";
  public String str_not_guilty = "Not Guilty";
  public String str_na = "N/A";

  public boolean show_no_contest_plea = false;
  public boolean show_na_plea = false;
  public boolean show_severity = false;
  public boolean use_minor_referrals = true;
  public boolean show_checkbox_for_res_plan = true;
  public boolean track_writer = true;
  public boolean filter_no_charge_cases = false;
  public boolean show_findings_in_rp_list = true;
  public boolean use_year_in_case_number = false;
  public boolean hide_location_for_print = false;
  public boolean euro_dates = false;

  public boolean enable_file_sharing = false;

  public Organization org;

  public TimeZone time_zone = TimeZone.getTimeZone("US/Eastern");

  public String getReferralDestination(Charge c) {
    return "School Meeting";
  }

  public String getCaseNumberPrefix(Meeting m) {
    String format;

    if (use_year_in_case_number) {
      format = euro_dates ? "dd-MM-YYYY-" : "YYYY-MM-dd-";
    } else {
      format = euro_dates ? "dd-MM-" : "MM-dd-";
    }
    return new SimpleDateFormat(format).format(m.getDate());
  }

  public String translatePlea(String plea) {
    if (plea.equals("Guilty")) {
      return this.str_guilty;
    } else if (plea.equals("Not Guilty")) {
      return this.str_not_guilty;
    } else if (plea.equals("N/A")) {
      return this.str_na;
    }

    return plea;
  }
}
