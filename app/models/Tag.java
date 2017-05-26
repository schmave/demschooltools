package models;

import java.util.*;

import javax.persistence.*;

import com.avaje.ebean.Model;
import com.avaje.ebean.Model.Finder;

import play.data.*;

@Entity
public class Tag extends Model {
    @Id
    public Integer id;

    public String title;

    public boolean use_student_display;
    public boolean show_in_menu;
    public boolean show_in_jc;

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
        return t;
    }

    public static boolean getBooleanFromForm(Form<?> form, String field_name) {
        String val = form.field(field_name).value();
        return val != null && val.equals("true");
    }

    public void updateFromForm(Form<Tag> form) {
        title = form.field("title").value();
        use_student_display = getBooleanFromForm(form, "use_student_display");
        show_in_jc = getBooleanFromForm(form, "show_in_jc");
        show_in_menu = getBooleanFromForm(form, "show_in_menu");
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
