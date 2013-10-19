package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.*;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;

@Entity
public class TestifyRecord extends Model {
    @ManyToOne
    @JoinColumn(name="person_id")
    public Person person;

    @ManyToOne
    @JoinColumn(name="case_id")
    public Case the_case;

    public static Finder<Integer, TestifyRecord> find = new Finder(
        Integer.class, TestifyRecord.class
    );
}
