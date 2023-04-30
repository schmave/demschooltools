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

  void initCreate(String num, String title, String content) {
    wasCreated = true;
    newNum = num;
    newTitle = title;
    newContent = content;
  }

  void initDelete(String num, String title, String content) {
    wasDeleted = true;
    oldNum = num;
    oldTitle = title;
    oldContent = content;
  }

  boolean initChange(
      String newNum,
      String oldNum,
      String newTitle,
      String oldTitle,
      String newContent,
      String oldContent) {

    // System.out.println("initChange " + newNum + " " + oldNum + " T " + newTitle + " " + oldTitle
    // + " C " + newContent + " " + oldContent);

    this.newNum = newNum;
    this.oldNum = oldNum;

    this.newTitle = newTitle;
    this.oldTitle = oldTitle;

    this.newContent = newContent;
    this.oldContent = oldContent;

    return (!this.newNum.equals(oldNum)
        || !this.newTitle.equals(oldTitle)
        || !this.newContent.equals(oldContent));
  }

  public static ManualChange recordEntryCreate(Entry e) {
    ManualChange result = new ManualChange();
    result.entry = e;
    result.initCreate(e.getNumber(), e.getTitle(), e.getContent());
    result.save();
    return result;
  }

  public static ManualChange recordEntryDelete(Entry e) {
    ManualChange result = new ManualChange();
    result.entry = e;
    result.initDelete(e.getNumber(), e.getTitle(), e.getContent());
    result.save();
    return result;
  }

  public static ManualChange recordEntryChange(
      Entry e, String oldNum, String oldTitle, String oldContent) {
    ManualChange result = new ManualChange();
    result.entry = e;
    if (result.initChange(
        e.getNumber(), oldNum, e.getTitle(), oldTitle, e.getContent(), oldContent)) {
      result.save();
      return result;
    }
    return null;
  }

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
