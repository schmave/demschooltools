package models;

import io.ebean.*;
import java.util.Comparator;
import java.util.Date;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class ManualChange extends Model {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "manual_change_id_seq")
  private Integer id;

  @ManyToOne() private Chapter chapter;
  @ManyToOne() private Section section;
  @ManyToOne() private Entry entry;

  private Boolean wasDeleted = false;
  private Boolean wasCreated = false;

  @Column(columnDefinition = "TEXT")
  private String oldContent;

  @Column(columnDefinition = "TEXT")
  private String newContent;

  private String oldTitle;
  private String newTitle;

  private String oldNum;
  private String newNum;

  private Date dateEntered = new Date();
}
