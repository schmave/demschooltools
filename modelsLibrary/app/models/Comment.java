package models;

import io.ebean.*;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "comments")
public class Comment extends Model {
    @Id
    private Integer id;

    @ManyToOne()
    @JoinColumn(name="personId")
    private Person person;

    @ManyToOne()
    @JoinColumn(name="user_id")
    private User user;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(insertable = false, updatable = false)
    private Date created;

    @OneToMany(mappedBy="comment")
    public List<CompletedTask> completed_tasks;

    public static Finder<Integer, Comment> find = new Finder<>(
            Comment.class
    );
}