package models;

import io.ebean.*;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class PersonAtMeeting extends Model {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_at_meeting_id_seq")
  private Integer id;

  @ManyToOne
  @JoinColumn(name = "meeting_id")
  private Meeting meeting;

  @ManyToOne
  @JoinColumn(name = "person_id")
  private Person person;

  public static final int ROLE_JC_CHAIR = 0;
  public static final int ROLE_JC_MEMBER = 1;
  public static final int ROLE_NOTE_TAKER = 2;
  public static final int ROLE_JC_SUB = 3;
  public static final int ROLE_RUNNER = 4;
  private Integer role;

  public static PersonAtMeeting create(Meeting m, Person p, Integer role) {
    PersonAtMeeting result = new PersonAtMeeting();

    result.meeting = m;
    result.person = p;
    result.role = role;

    result.save();
    return result;
  }
}
