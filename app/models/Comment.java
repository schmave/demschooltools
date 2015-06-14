package models;

import java.util.*;

import javax.persistence.*;

import play.data.*;
import play.data.validation.Constraints.*;
import com.avaje.ebean.Model;

@Entity
@Table(name = "comments")
public class Comment extends Model {
    @Id
    public Integer id;

    @ManyToOne()
    @JoinColumn(name="person_id")
    public Person person;

    @ManyToOne()
    @JoinColumn(name="user_id")
    public User user;

    @Column(columnDefinition = "TEXT")
    public String message;

    @Column(insertable = false, updatable = false)
    public Date created;

    @OneToMany(mappedBy="comment")
    public List<CompletedTask> completed_tasks;

    public static Finder<Integer, Comment> find = new Finder<>(
        Integer.class, Comment.class
    );
}
