package models;

import javax.persistence.*;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;

@Entity
public class CompletedTask extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "completed_task_id_seq")
    public Integer id;

    @ManyToOne
    @JoinColumn(name="task_id")
    public Task task;

    @ManyToOne
    @JoinColumn(name="person_id")
    public Person person;

    @ManyToOne
    @JoinColumn(name="comment_id")
    public Comment comment;

    public static Finder<Integer, CompletedTask> find = new Finder(
        Integer.class, CompletedTask.class
    );

    public static CompletedTask create(Task t, Comment c)
    {
        CompletedTask result = new CompletedTask();
        result.task = t;
        result.comment = c;
        result.person = c.person;
        result.save();

        return result;
    }
}

