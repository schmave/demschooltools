package models;

import play.db.ebean.*;
import play.data.validation.Constraints.*;

import java.util.*;

import javax.persistence.*;

@Entity
public class Student extends Model {
    @Id
    public Integer student_id;

    @Required
    public String first_name;

    @Required
    public String last_name;

    public static Finder<Integer,Student> find = new Finder(
        Integer.class, Student.class
    );

    public static List<Student> all() {
        return find.all();
    }

    public static void create(Student student) {
        student.save();
    }

    public static void delete(Integer id) {
        find.ref(id).delete();
    }
}
