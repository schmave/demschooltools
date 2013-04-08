package models;

import play.db.ebean.*;
import play.data.validation.Constraints.*;

import java.util.*;

import javax.persistence.*;

@Entity
public class Student extends Model {
    @Id
    public Long id;

    @Required
    public String firstName;

    @Required
    public String lastName;

    public static Finder<Long,Student> find = new Finder(
        Long.class, Student.class
    );

    public static List<Student> all() {
        return find.all();
    }

    public static void create(Student student) {
        student.save();
    }

    public static void delete(Long id) {
        find.ref(id).delete();
    }
}
