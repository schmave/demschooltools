package models;

import play.db.ebean.*;
import play.data.validation.Constraints.*;

import java.util.*;

import javax.persistence.*;

@Entity
public class Person extends Model {
    @Id
    public Integer person_id;

    @Required
    public String first_name;

    @Required
    public String last_name;

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
