package models;

import io.ebean.*;

import javax.persistence.*;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Task extends Model {
  @Id private Integer id;

  @ManyToOne()
  @JoinColumn(name = "task_list_id")
  private TaskList taskList;

  private String title;

  private Integer sortOrder;

  private boolean enabled;

  @OneToMany(mappedBy = "task")
  public List<CompletedTask> completed_tasks;

  public static Finder<Integer, Task> find = new Finder<>(Task.class);

  public static Task findById(int id, Organization org) {
    return find.query().where().eq("taskList.organization", org).eq("id", id).findOne();
  }
}
