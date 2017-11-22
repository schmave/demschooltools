package models;

import java.util.*;
import javax.persistence.*;
import play.data.*;
import com.avaje.ebean.Model;

@Entity
public class Institution extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "institution_id_seq")
    public Integer id;

    @ManyToOne()
    public Organization organization;

    public String name = "";

    public static Finder<Integer, Institution> find = new Finder<Integer, Institution>(
        Institution.class
    );

    public static Institution findById(Integer id) {
        return find.where().eq("organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }

    public static Institution create(Form<Institution> form) {
        Institution institution = form.get();
        institution.organization = Organization.getByHost();
        institution.save();
        return institution;
    }
}
