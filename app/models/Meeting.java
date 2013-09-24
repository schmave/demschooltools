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
public class Meeting extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "meeting_id_seq")
    public Integer id;

    public Date date;

    @OneToMany(mappedBy="meeting")
    public List<PersonAtMeeting> people_at_meeting;

    @OneToMany(mappedBy="meeting")
    public List<Case> cases;

    public static Finder<Integer, Meeting> find = new Finder(
        Integer.class, Meeting.class
    );

    public static Meeting create(Date d)
    {
        Meeting result = new Meeting();
        result.date = d;
        return result;
    }
}
