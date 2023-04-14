package models;

import io.ebean.*;

import javax.persistence.*;

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
            PhoneNumber.class
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
