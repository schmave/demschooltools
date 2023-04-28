package models;

import io.ebean.*;

import javax.persistence.*;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name="phone_numbers")
public class PhoneNumber extends Model {
    @Id
    private Integer id;

    private String number;
    private String comment;

    @ManyToOne()
    @JoinColumn(name="personId")
    private Person owner;

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