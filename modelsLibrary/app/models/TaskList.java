package models;

import io.ebean.*;

import javax.persistence.*;
import javax.persistence.OrderBy;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class TaskList extends Model {
  @Id private Integer id;

  private String title;

  @ManyToOne() private Organization organization;

  @OneToMany(mappedBy = "taskList")
  @OrderBy("sortOrder")
  public List<Task> tasks;

  @OneToOne()
  @JoinColumn(name = "tag_id")
  private Tag tag;

  public static Finder<Integer, TaskList> find = new Finder<>(TaskList.class);

  public static TaskList findById(int id, Organization org) {
    return find.query().where().eq("organization", org).eq("id", id).findOne();
  }

  public static List<TaskList> allForOrg(Organization org) {
    return TaskList.find.query().where().eq("organization", org).order("title ASC").findList();
  }
}
