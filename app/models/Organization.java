package models;

import javax.persistence.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;
import play.libs.Json;
import play.mvc.Http.Context;


@Entity
public class Organization extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "organization_id_seq")
    public Integer id;

    public String name;

    public static Finder<Integer, Organization> find = new Finder(
        Integer.class, Organization.class
    );

    public static Organization getByHost() {
        String host = Context.current().request().host();

        String sql = "select organization_id from organization_hosts where host like :host";
        SqlQuery sqlQuery = Ebean.createSqlQuery(sql);
        sqlQuery.setParameter("host", host);

        // execute the query returning a List of MapBean objects
        SqlRow result = sqlQuery.findUnique();

        if (result != null) {
            return find.byId(result.getInteger("organization_id"));
        }

        return null;
    }
}
