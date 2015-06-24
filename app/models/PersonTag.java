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

    public static Finder<Integer, PersonTag> find = new Finder(
        Integer.class, PersonTag.class
    );

    public static PersonTag create(Tag t, Person p) {
        PersonTag result = new PersonTag();
        result.tag_id = t.id;
        result.person_id = p.person_id;

        result.save();
        return result;
    }
}
