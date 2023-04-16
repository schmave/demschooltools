package models;

import io.ebean.*;

import javax.persistence.*;
import javax.persistence.OrderBy;
import java.util.List;

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
            TaskList.class
    );

    public static TaskList findById(int id, Organization org) {
        return find.query().where().eq("organization", org)
            .eq("id", id).findOne();
    }

    public static List<TaskList> allForOrg(Organization org) {
        return TaskList.find.query()
            .where().eq("organization", org)
            .order("title ASC")
            .findList();
    }
}

