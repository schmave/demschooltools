package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.ebean.*;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class PersonAtCase extends Model {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "testify_record_id_seq")
  private Integer id;

  @ManyToOne
  @JoinColumn(name = "person_id")
  private Person person;

  @ManyToOne
  @JoinColumn(name = "case_id")
  @JsonIgnore
  private Case theCase;

  public static final int ROLE_TESTIFIER = 0;
  public static final int ROLE_WRITER = 1;
  private Integer role;

  public static PersonAtCase create(Case c, Person p, Integer r) {
    PersonAtCase result = new PersonAtCase();
    result.theCase = c;
    result.person = p;
    result.role = r;
    result.save();

    return result;
  }
}
