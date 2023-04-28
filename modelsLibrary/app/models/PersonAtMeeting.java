package models;

import javax.persistence.*;

import io.ebean.*;

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
    @JoinColumn(name="meeting_id")
    private Meeting meeting;

    @ManyToOne
    @JoinColumn(name="personId")
    private Person person;

    public final static int ROLE_JC_CHAIR = 0;
    public final static int ROLE_JC_MEMBER = 1;
    public final static int ROLE_NOTE_TAKER = 2;
    public final static int ROLE_JC_SUB = 3;
    public final static int ROLE_RUNNER = 4;
    private Integer role;

    public static PersonAtMeeting create(Meeting m, Person p, Integer role)
    {
        PersonAtMeeting result = new PersonAtMeeting();

        result.meeting = m;
        result.person = p;
        result.role = role;

        result.save();
        return result;
    }
}