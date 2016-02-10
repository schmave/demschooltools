package models;

import java.util.*;

import javax.persistence.*;

import play.data.*;
import play.data.validation.Constraints.*;
import com.avaje.ebean.Model;

@Entity
@Table(name="phone_numbers")
public class PhoneNumber extends Model {
    @Id
    public Integer id;

    public String number;
    public String comment;

    @ManyToOne()
    @JoinColumn(name="person_id")
    public Person owner;

    public static Finder<Integer, PhoneNumber> find = new Finder<>(
        Integer.class, PhoneNumber.class
    );

    public static PhoneNumber create(String number, String comment, Person owner) {
        PhoneNumber result = new PhoneNumber();
        result.number = number;
        result.comment = comment;
        result.owner = owner;

        result.save();
        return result;
    }
}
