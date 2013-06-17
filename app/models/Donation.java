package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.*;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;

@Entity
public class Donation extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="donation_id_seq")
    public Integer id;

    public boolean is_cash;
    public float dollar_value;
    public String description;
    public Date date;

    @ManyToOne()
    @JoinColumn(name="person_id")
    public Person person;

    public boolean thanked;
    @ManyToOne()
    @JoinColumn(name="thanked_by_user_id")
    public User thanked_by_user;
    public Date thanked_time;

    public boolean indiegogo_reward_given;
    @ManyToOne()
    @JoinColumn(name="indiegogo_reward_by_user_id")
    public User indiegogo_reward_by_user;
    public Date indiegogo_reward_given_time;

    public static Finder<Integer, Donation> find = new Finder(
        Integer.class, Donation.class
    );
}
