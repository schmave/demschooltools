package models;

import io.ebean.*;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class CompletedTask extends Model {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "completed_task_id_seq")
  private Integer id;

  @ManyToOne
  @JoinColumn(name = "task_id")
  private Task task;

  @ManyToOne
  @JoinColumn(name = "person_id")
  private Person person;

  @ManyToOne
  @JoinColumn(name = "comment_id")
  private Comment comment;

  public static Finder<Integer, CompletedTask> find = new Finder<>(CompletedTask.class);

  public static CompletedTask create(Task t, Comment c) {
    CompletedTask result = new CompletedTask();
    result.task = t;
    result.comment = c;
    result.person = c.getPerson();
    result.save();

    return result;
  }
}
