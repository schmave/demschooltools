package models;

import java.util.*;

import javax.persistence.*;

import play.data.*;
import play.data.validation.Constraints.*;
import com.avaje.ebean.Model;


@Entity
public class PersonTag extends Model {
    public long tag_id;
    public long person_id;


    //@EmbeddedId
    //public PersonTagKey key;

    //@Id
    //@ManyToOne()
    //@JoinColumn(name="tag_id")
    //public Tag tag;
    //
    //@Id
    //@ManyToOne()
    //@JoinColumn(name="person_id")
    //public Person person;

    public static PersonTag create(Tag t, Person p) {
        PersonTag result = new PersonTag();
        result.tag_id = t.id;
        result.person_id = p.person_id;

        result.save();
        return result;
    }
}
