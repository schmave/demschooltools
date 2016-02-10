package models;

import java.util.List;

import javax.persistence.*;

import com.avaje.ebean.Model;
import com.avaje.ebean.Model.Finder;

@Entity
public class TaskList extends Model {
    @Id
    public Integer id;

    public String title;

    @ManyToOne()
    public Organization organization;

    @OneToMany(mappedBy="task_list")
    @OrderBy("sort_order")
    public List<Task> tasks;

    @OneToOne()
    @JoinColumn(name="tag_id")
    public Tag tag;

    public static Finder<Integer, TaskList> find = new Finder<>(
        Integer.class, TaskList.class
    );

    public static TaskList findById(int id) {
        return find.where().eq("organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }

    public static List<TaskList> allForOrg() {
        return TaskList.find
            .where().eq("organization", OrgConfig.get().org)
            .order("title ASC")
            .findList();
    }
}

