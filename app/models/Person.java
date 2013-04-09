package models;

import java.util.*;

import javax.persistence.*;

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
        return find.all();
    }

    public static void create(Person person) {
        person.save();
    }

    public static void delete(Integer id) {
        find.ref(id).delete();
    }
}
