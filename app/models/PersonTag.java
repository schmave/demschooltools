package models;

import java.util.*;

import javax.persistence.*;

import play.data.*;
import play.data.validation.Constraints.*;
import play.db.ebean.*;

@Entity
public class PersonTag extends Model {
    @Id
    public long tag_id;

    @Id
    public long person_id;
    //@Id
    //@ManyToOne()
    //@JoinColumn(name="tag_id")
    //public Tag tag;
    //
    //@Id
    //@ManyToOne()
    //@JoinColumn(name="person_id")
    //public Person person;

    @ManyToOne()
    @JoinColumn(name="creator_id")
    public User creator;

    @Column(insertable = false, updatable = false)
    public Date created;

    public static Finder<Integer, PersonTag> find = new Finder(
        Integer.class, PersonTag.class
    );

    public static PersonTag create(Tag t, Person p, User u) {
        PersonTag result = new PersonTag();
        result.tag_id = t.id;
        result.person_id = p.person_id;
        result.creator = u;

        result.save();
        return result;
    }
}
