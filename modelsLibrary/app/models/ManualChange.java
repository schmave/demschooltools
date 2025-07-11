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

  public static Finder<Integer, ManualChange> find = new Finder<>(ManualChange.class);

  public static Comparator<ManualChange> SORT_NUM_DATE =
      (c1, c2) -> {
        // I want to order the change records by the user visible num.
        // However, when entries are created or deleted, the newNum
        // or oldNum, respectively, is null. Use the newNum if it
        // is available, or oldNum otherwise.
        String num1 = (c1.newNum == null ? c1.oldNum : c1.newNum);
        String num2 = (c2.newNum == null ? c2.oldNum : c2.newNum);

        if (num1.equals(num2)) {
          return c1.dateEntered.compareTo(c2.dateEntered);
        } else {
          return num1.compareTo(num2);
        }
      };
}
