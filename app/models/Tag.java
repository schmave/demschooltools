package models;

import java.util.*;

import javax.persistence.*;

import play.data.*;
import play.data.validation.Constraints.*;
import play.db.ebean.*;

@Entity
public class Tag extends Model {
    @Id
    public Integer id;

    public String title;

    public static Finder<Integer, Tag> find = new Finder(
        Integer.class, Tag.class
    );

    public static Tag create(String title) {
        Tag result = new Tag();
        result.title = title;

        result.save();
        return result;
    }
}
