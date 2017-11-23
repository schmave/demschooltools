package models;

import java.util.*;
import javax.persistence.*;
import com.fasterxml.jackson.annotation.*;
import play.data.*;
import com.avaje.ebean.*;

@Entity
public class Institution extends Model {

    @Id
    public Integer id;

    @ManyToOne()
    public Organization organization;

    @OneToMany(mappedBy = "institution")
    @JsonIgnore
    public List<Account> accounts;

    public String name = "";

    public InstitutionType type;

    public String typeName() {
        return type.name();
    }

    public static Finder<Integer, Institution> find = new Finder<Integer, Institution>(
        Institution.class
    );

    public static List<Institution> all() {
        return find.where()
            .eq("organization", Organization.getByHost())
            .orderBy("name ASC")
            .findList();
    }

    public static Institution findById(Integer id) {
        return find.where().eq("organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }

    public static Institution create(Form<Institution> form) {
        Institution institution = form.get();
        institution.organization = Organization.getByHost();
        institution.save();
        Account.create(null, institution, AccountType.Cash);
        Account.create(null, institution, AccountType.Checking);
        return institution;
    }
}
