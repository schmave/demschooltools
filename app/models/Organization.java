package models;

import java.util.*;

import javax.persistence.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;

import com.fasterxml.jackson.annotation.*;

import play.cache.Cache;
import play.Logger;
import com.avaje.ebean.Model;
import com.avaje.ebean.Model.Finder;
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

    public static Finder<Integer, Organization> find = new Finder<>(
        Integer.class, Organization.class
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
            Logger.error("Unknown organization for host: " + host);
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
}

