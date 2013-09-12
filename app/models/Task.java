package models;

import java.util.List;

import javax.persistence.*;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;

@Entity
public class Task extends Model {
    @Id
    public Integer id;

    @ManyToOne()
    @JoinColumn(name="task_list_id")
    public TaskList task_list;

    public String title;

    public Integer sort_order;

	public boolean enabled;

    @OneToMany(mappedBy="task")
    public List<CompletedTask> completed_tasks;

    public static Finder<Integer, Task> find = new Finder(
        Integer.class, Task.class
    );
}
