package models;

import io.ebean.*;
import java.util.*;
import javax.persistence.*;
import javax.persistence.OrderBy;
import lombok.Getter;
import lombok.Setter;
import play.data.Form;

@Getter
@Setter
@Entity
public class Tag extends Model {
  @Id private Integer id;

  private String title;

  private boolean useStudentDisplay;
  private boolean showInMenu;
  private boolean showInJc;
  private boolean showInAttendance;
  private boolean showInAccountBalances;

  @ManyToMany(cascade = CascadeType.ALL)
  @JoinTable(
      name = "person_tag",
      joinColumns = @JoinColumn(name = "tag_id", referencedColumnName = "id"),
      inverseJoinColumns = @JoinColumn(name = "person_id", referencedColumnName = "person_id"))
  public List<Person> people;

  @ManyToOne() private Organization organization;

  @OneToMany(mappedBy = "tag")
  public List<TaskList> task_lists;

  @OneToMany(mappedBy = "tag")
  public List<MailchimpSync> syncs;

  @OneToMany(mappedBy = "tag")
  @OrderBy("time DESC")
  public List<PersonTagChange> changes;

  @OneToMany(mappedBy = "tag")
  public List<NotificationRule> notification_rules;

  public static Finder<Integer, Tag> find = new Finder<>(Tag.class);

  public static Tag findById(int id, Organization org) {
    return find.query().where().eq("organization", org).eq("id", id).findOne();
  }

  public static Tag create(String title, Organization org) {
    Tag result = new Tag();
    result.title = title;
    result.organization = org;
    result.showInMenu = true;

    result.save();
    return result;
  }

  public Map<String, Object> serialize() {
    Map<String, Object> t = new HashMap<>();
    t.put("id", id);
    t.put("title", title);
    t.put("showInMenu", showInMenu);
    t.put("showInJc", showInJc);
    t.put("showInAttendance", showInAttendance);
    return t;
  }

  public void updateFromForm(Form<Tag> form) {
    title = form.field("title").value().get();
    useStudentDisplay = ModelUtils.getBooleanFromFormValue(form.field("useStudentDisplay"));
    showInJc = ModelUtils.getBooleanFromFormValue(form.field("showInJc"));
    showInAttendance = ModelUtils.getBooleanFromFormValue(form.field("showInAttendance"));
    showInMenu = ModelUtils.getBooleanFromFormValue(form.field("showInMenu"));
    showInAccountBalances = ModelUtils.getBooleanFromFormValue(form.field("showInAccountBalances"));
    save();
  }

  public static Map<String, List<Tag>> getWithPrefixes(Organization org) {
    Map<String, List<Tag>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    for (Tag t :
        find.query()
            .where()
            .eq("organization", org)
            .eq("showInMenu", true)
            .order("title ASC")
            .findList()) {
      String[] splits = t.title.split(":");

      List<Tag> cur_list = result.get(splits[0]);
      if (cur_list == null) {
        cur_list = new ArrayList<>();
      }
      cur_list.add(t);
      result.put(splits[0], cur_list);
    }

    return result;
  }
}
