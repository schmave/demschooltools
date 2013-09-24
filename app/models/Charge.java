package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.*;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;

@Entity
public class Charge extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "charge_id_seq")
    public Integer id;

    public String title;

    @ManyToOne
    @JoinColumn(name="person_id")
    public Person person;

    @ManyToOne
    @JoinColumn(name="rule_id")
    public Rule rule;

    @ManyToOne
    @JoinColumn(name="case_id")
    public Case the_case;

    public String plea;
    public String resolution_plan;

    public static Finder<Integer, Charge> find = new Finder(
        Integer.class, Charge.class
    );
}
