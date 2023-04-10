package models;

import com.avaje.ebean.Model;

import javax.persistence.*;
import java.util.List;

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
            Task.class
    );

    public static Task findById(int id) {
        return find.where().eq("task_list.organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }
}
