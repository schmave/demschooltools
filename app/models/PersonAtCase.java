package models;

import io.ebean.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

@Entity
public class PersonAtCase extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "testify_record_id_seq")
    public Integer id;

    @ManyToOne
    @JoinColumn(name="person_id")
    public Person person;

    @ManyToOne
    @JoinColumn(name="case_id")
    @JsonIgnore
    public Case the_case;

    public final static int ROLE_TESTIFIER = 0;
    public final static int ROLE_WRITER = 1;
    public Integer role;

    public static PersonAtCase create(Case c, Person p, Integer r)
    {
        PersonAtCase result = new PersonAtCase();
        result.the_case = c;
        result.person = p;
        result.role = r;
        result.save();

        return result;
    }
}
