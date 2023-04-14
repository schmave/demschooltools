package models;

import io.ebean.*;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

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
            Comment.class
    );
}
