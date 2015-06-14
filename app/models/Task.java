package models;

import java.util.List;

import javax.persistence.*;

import com.avaje.ebean.Model;
import com.avaje.ebean.Model.Finder;

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

    public static Finder<Integer, Task> find = new Finder<>(
        Integer.class, Task.class
    );

    public static Task findById(int id) {
        return find.where().eq("task_list.organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }
}
