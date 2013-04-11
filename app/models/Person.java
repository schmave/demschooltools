package models;

import java.util.*;

import javax.persistence.*;

import play.data.*;
import play.data.validation.Constraints.*;
import play.db.ebean.*;

@Entity
public class Person extends Model {
    @Id
    public Integer person_id;

    public String first_name, last_name;

    @Column(columnDefinition = "TEXT")
    public String notes;

    // phone number references

    public String address;
    public String city;
    public String state;
    public String zip;
    public String neighborhood;

    // email address
    public String email;

    public Date dob;
    public Date approximate_dob;

    // tags

    // comment references

    // is_family is true if this Person object represents
    // a family, not a single person.
    public boolean is_family;

    // family ID
    @ManyToOne(fetch=FetchType.LAZY)
    public Person family;

    public static Finder<Integer,Person> find = new Finder(
        Integer.class, Person.class
    );

    public static List<Person> all() {
        return find.where().eq("is_family", Boolean.FALSE).orderBy("last_name, first_name ASC").findList();
    }

    public void attachToPersonAsFamily(Integer id) {
        Person other_family_member = Person.find.ref(id);

        if (other_family_member.family != null) {
            // Attach to their existing family.
            this.family = other_family_member.family;
        } else {
            // Create a new family to hold these two people.
            Person family = new Person();
            family.is_family = true;
            family.save();
            this.family = family;
            other_family_member.family = family;
            other_family_member.update();
        }
    }

    public static void create(Person person, Integer same_family_id) {
        person.is_family = false;

        if (same_family_id != null) {
            person.attachToPersonAsFamily(same_family_id);
        }
        person.save();
    }

    public static Person updateFromForm(Form<Person> filledForm) {
        Person p = filledForm.get();
        String family_id = filledForm.field("same_family_id").value();

        if (family_id != null) {
            p.attachToPersonAsFamily(Integer.parseInt(family_id));
        }
        p.update();
        return p;
    }

    public static void delete(Integer id) {
        find.ref(id).delete();
    }
}
