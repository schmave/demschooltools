package models;

import io.ebean.*;
import java.util.Date;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class PersonTagChange extends Model {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_tag_change_id_seq")
  private int id;

  @ManyToOne()
  @JoinColumn(name = "tag_id")
  private Tag tag;

  @ManyToOne()
  @JoinColumn(name = "person_id")
  private Person person;

  @ManyToOne()
  @JoinColumn(name = "creator_id")
  private User creator;

  @Column(insertable = false, updatable = false)
  private Date time;

  private boolean wasAdd;

  public static Finder<Integer, PersonTagChange> find = new Finder<>(PersonTagChange.class);

  public static PersonTagChange create(Tag t, Person p, User u, boolean wasAdd) {
    PersonTagChange result = new PersonTagChange();
    result.tag = t;
    result.person = p;
    result.creator = u;
    result.wasAdd = wasAdd;

    result.save();
    return result;
  }
}
