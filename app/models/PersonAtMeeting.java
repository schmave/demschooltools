package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.*;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;

@Entity
public class PersonAtMeeting extends Model {
    @ManyToOne
    @JoinColumn(name="meeting_id")
    public Meeting meeting;

    @ManyToOne
    @JoinColumn(name="person_id")
    public Person person;

    public Integer role;

    public static Finder<String, PersonAtMeeting> find = new Finder(
        String.class, PersonAtMeeting.class
    );
}
