package models;

import java.util.*;

import javax.persistence.*;

import com.avaje.ebean.Model;
import com.avaje.ebean.Model.Finder;

import controllers.Utils;
import play.data.*;

@Entity
public class Tag extends Model {
    @Id
    public Integer id;

    public String title;

    public boolean use_student_display;
    public boolean show_in_menu;
    public boolean show_in_jc;
    public boolean show_in_attendance;

    @ManyToMany(cascade=CascadeType.ALL)
    @JoinTable(name="person_tag",
        joinColumns=@JoinColumn(name="tag_id",referencedColumnName = "id"),
        inverseJoinColumns = @JoinColumn(name="person_id", referencedColumnName="person_id"))
    public List<Person> people;

    @ManyToOne()
    public Organization organization;

    @OneToMany(mappedBy="tag")
    public List<TaskList> task_lists;

    @OneToMany(mappedBy="tag")
    public List<MailchimpSync> syncs;

    @OneToMany(mappedBy="tag")
    @OrderBy("time DESC")
    public List<PersonTagChange> changes;

    @OneToMany(mappedBy="tag")
    public List<NotificationRule> notification_rules;

    public static Finder<Integer, Tag> find = new Finder<Integer, Tag>(
        Tag.class
    );

    public static Tag findById(int id) {
        return find.where().eq("organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }

    public static Tag create(String title) {
        Tag result = new Tag();
        result.title = title;
        result.organization = Organization.getByHost();
        result.show_in_menu = true;

        result.save();
        return result;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> t = new HashMap<>();
        t.put("id", id);
        t.put("title", title);
        t.put("show_in_menu", show_in_menu);
        t.put("show_in_jc", show_in_jc);
        t.put("show_in_attendance", show_in_attendance);
        return t;
    }

    public void updateFromForm(Form<Tag> form) {
        title = form.field("title").value();
        use_student_display = Utils.getBooleanFromFormValue(form.field("use_student_display").value());
        show_in_jc = Utils.getBooleanFromFormValue(form.field("show_in_jc").value());
        show_in_attendance = Utils.getBooleanFromFormValue(form.field("show_in_attendance").value());
        show_in_menu = Utils.getBooleanFromFormValue(form.field("show_in_menu").value());
        save();
    }

    public static Map<String, List<Tag>> getWithPrefixes() {
        Map<String, List<Tag>> result = new TreeMap<String, List<Tag>>(String.CASE_INSENSITIVE_ORDER);

        for (Tag t : find.where()
                .eq("organization", Organization.getByHost())
                .eq("show_in_menu", true)
                .order("title ASC").findList()) {
            String[] splits = t.title.split(":");
            String prefix = t.title;

            if (splits.length > 1) {
                prefix = splits[0];
            }

            List<Tag> cur_list = result.get(splits[0]);
            if (cur_list == null) {
                cur_list = new ArrayList<Tag>();
            }
            cur_list.add(t);
            result.put(splits[0], cur_list);
        }

        return result;
    }
}
