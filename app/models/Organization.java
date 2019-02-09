package models;

import java.util.*;
import java.math.*;
import java.text.*;
import java.sql.Time;

import javax.persistence.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;

import com.fasterxml.jackson.annotation.*;

import controllers.Utils;
import play.cache.Cache;
import play.Logger;
import com.avaje.ebean.Model;
import play.mvc.Http.Context;

@Entity
public class Organization extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "organization_id_seq")
    public Integer id;

    public String name;

    // Don't serialize this one by default because it is private.
    @JsonIgnore
    public String mailchimp_api_key;

    public Date mailchimp_last_sync_person_changes;

    public String mailchimp_updates_email;

    public String printer_email;

    public Integer jc_reset_day;
    public Boolean show_last_modified_in_print;
    public Boolean show_history_in_print;

    public String short_name;
    public String custodia_password;
    public Boolean show_custodia;
    public Boolean show_attendance;
    public Boolean show_accounting;

    public Boolean attendance_show_percent;
    public Boolean attendance_enable_partial_days;
    public Time attendance_day_latest_start_time;
    public Integer attendance_day_min_hours;
    public BigDecimal attendance_partial_day_value;

    @OneToMany(mappedBy="organization")
    @JsonIgnore
    public List<NotificationRule> notification_rules;

    public static Finder<Integer, Organization> find = new Finder<Integer, Organization>(
        Organization.class
    );

    public static Organization getByHost() {
        String host = Context.current().request().host();

        String cache_key = "Organization::getByHost::" + host;

        Object cached_val = Cache.get(cache_key);
        if (cached_val!= null) {
            return (Organization)cached_val;
        }

        String sql = "select organization_id from organization_hosts where host like :host";
        SqlQuery sqlQuery = Ebean.createSqlQuery(sql);
        sqlQuery.setParameter("host", host);

        // execute the query returning a List of MapBean objects
        SqlRow result = sqlQuery.findUnique();

        if (result != null) {
            Organization org_result = find.byId(result.getInteger("organization_id"));
            Cache.set(cache_key, org_result, 60); // cache for 1 minute
            return org_result;
        } else {
			if (host.matches("[a-z]")) {
				Logger.error("Unknown organization for host: " + host);
			}
        }

        return null;
    }

    public void setMailChimpApiKey(String key) {
        this.mailchimp_api_key = key;
        this.save();
    }

    public void setMailChimpUpdatesEmail(String email) {
        this.mailchimp_updates_email = email;
        this.save();
    }

    public void setLastMailChimpSyncTime(Date d) {
        this.mailchimp_last_sync_person_changes = d;
        this.save();
    }

    public void setPrinterEmail(String email) {
        this.printer_email = email;
        this.save();
    }

    public String formatAttendanceLatestStartTime() {
        if (attendance_day_latest_start_time == null) {
            return "";
        }
        DateFormat format = new SimpleDateFormat("HH:mm a");
        return format.format(attendance_day_latest_start_time.getTime());
    }

    public void updateFromForm(Map<String, String[]> values) {
        if (values.containsKey("jc_reset_day")) {
            this.jc_reset_day = Integer.parseInt(values.get("jc_reset_day")[0]);
        }
        if (values.containsKey("manual_settings")) {
            if (values.containsKey("show_last_modified_in_print")) {
                this.show_last_modified_in_print = Utils.getBooleanFromFormValue(values.get("show_last_modified_in_print")[0]);
            } else {
                this.show_last_modified_in_print = false;
            }
            if (values.containsKey("show_history_in_print")) {
                this.show_history_in_print = Utils.getBooleanFromFormValue(values.get("show_history_in_print")[0]);
            } else {
                this.show_history_in_print = false;
            }
        }
        if (values.containsKey("attendance_settings")) {
            if (values.containsKey("show_attendance")) {
                this.show_attendance = Utils.getBooleanFromFormValue(values.get("show_attendance")[0]);
            } else {
                this.show_attendance = false;
            }
            if (values.containsKey("attendance_show_percent")) {
                this.attendance_show_percent = Utils.getBooleanFromFormValue(values.get("attendance_show_percent")[0]);
            } else {
                this.attendance_show_percent = false;
            }
            if (values.containsKey("attendance_enable_partial_days")) {
                this.attendance_enable_partial_days = Utils.getBooleanFromFormValue(values.get("attendance_enable_partial_days")[0]);
            } else {
                this.attendance_enable_partial_days = false;
            }
            if (values.containsKey("show_custodia")) {
                this.show_custodia = Utils.getBooleanFromFormValue(values.get("show_custodia")[0]);
            } else {
                this.show_custodia = false;
            }
            if (!values.containsKey("attendance_day_min_hours") || values.get("attendance_day_min_hours")[0].isEmpty()) {
                this.attendance_day_min_hours = null;
            } else {
                this.attendance_day_min_hours = Integer.parseInt(values.get("attendance_day_min_hours")[0]);
            }
            if (!values.containsKey("attendance_partial_day_value") || values.get("attendance_partial_day_value")[0].isEmpty()) {
                this.attendance_partial_day_value = null;
            } else {
                this.attendance_partial_day_value = new BigDecimal(values.get("attendance_partial_day_value")[0]);
            }
            if (values.containsKey("attendance_day_latest_start_time")) {
                this.attendance_day_latest_start_time = AttendanceDay.parseTime(values.get("attendance_day_latest_start_time")[0]);
            } else {
                this.attendance_day_latest_start_time = null;
            }
        }
        if (values.containsKey("accounting_settings")) {
            if (values.containsKey("show_accounting")) {
                this.show_accounting = Utils.getBooleanFromFormValue(values.get("show_accounting")[0]);
                if (this.show_accounting) {
                    Account.createPersonalAccounts();
                }
            } else {
                this.show_accounting = false;
            }
        }
        this.save();
    }

}

