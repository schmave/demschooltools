package models;

import io.ebean.*;

import javax.persistence.*;
import java.util.Date;


@Entity
public class PersonTagChange extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_tag_change_id_seq")
    public int id;

    @ManyToOne()
    @JoinColumn(name="tag_id")
    public Tag tag;

    @ManyToOne()
    @JoinColumn(name="person_id")
    public Person person;

    @ManyToOne()
    @JoinColumn(name="creator_id")
    public User creator;

    @Column(insertable = false, updatable = false)
    public Date time;

    public boolean was_add;

    public static Finder<Integer, PersonTagChange> find = new Finder<>(
            PersonTagChange.class
    );

    public static PersonTagChange create(Tag t, Person p, User u, boolean was_add) {
        PersonTagChange result = new PersonTagChange();
        result.tag = t;
        result.person = p;
        result.creator = u;
        result.was_add = was_add;

        result.save();
        return result;
    }
}
