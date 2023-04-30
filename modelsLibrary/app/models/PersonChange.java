package models;

import io.ebean.*;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class PersonChange extends Model {
    @OneToOne()
    @JoinColumn(name="person_id")
    private Person person;

    private String oldEmail="";

    private String newEmail="";

    @Column(insertable = false, updatable = false)
    private Date time;

    public static Finder<Integer, PersonChange> find = new Finder<>(
            PersonChange.class
    );

    public static PersonChange create(Person p, String newEmail) {
        PersonChange result = new PersonChange();
        result.person = p;
        result.oldEmail = p.getEmail();
        result.newEmail = newEmail;

        result.save();
        return result;
    }
}