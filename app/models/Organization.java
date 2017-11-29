package models;

import java.util.*;

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
            if (values.containsKey("show_custodia")) {
                this.show_custodia = Utils.getBooleanFromFormValue(values.get("show_custodia")[0]);
            } else {
                this.show_custodia = false;
            }
        }
        if (values.containsKey("accounting_settings")) {
            if (values.containsKey("show_accounting")) {
                this.show_accounting = Utils.getBooleanFromFormValue(values.get("show_accounting")[0]);
                if (this.show_accounting) {
                    this.createPersonalAccounts();
                }
            } else {
                this.show_accounting = false;
            }
        }
        this.save();
    }

    public void createPersonalAccounts() {
        for (Person person : Person.all()) {
            if (!person.hasAccount(AccountType.Cash)) {
                Account.create(AccountType.Cash, "", person);
            }
            if (!person.hasAccount(AccountType.PersonalChecking)) {
                Account.create(AccountType.PersonalChecking, "", person);
            }
        }
    }
}

