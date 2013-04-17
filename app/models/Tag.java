package models;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;

import javax.persistence.*;

@Entity
public class Tag extends Model {
    @Id
    public Integer id;

    public String title;

    @OneToOne(mappedBy="tag")
    public TaskList task_list;

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
