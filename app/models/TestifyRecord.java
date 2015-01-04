package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.*;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;

@Entity
public class TestifyRecord extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "testify_record_id_seq")
    public Integer id;

    @ManyToOne
    @JoinColumn(name="person_id")
    public Person person;

    @ManyToOne
    @JoinColumn(name="case_number")
    @JsonIgnore
    public Case the_case;

    public static TestifyRecord create(Case c, Person p)
    {
        TestifyRecord result = new TestifyRecord();
        result.the_case = c;
        result.person = p;
        result.save();

        return result;
    }
}
