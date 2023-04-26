package models;

import io.ebean.*;
import play.data.Form;

import javax.persistence.*;
import javax.persistence.OrderBy;
import java.util.*;

@Entity
public class Tag extends Model {
    @Id
    public Integer id;

    public String title;

    public boolean use_student_display;
    public boolean show_in_menu;
    public boolean show_in_jc;
    public boolean show_in_attendance;
    public boolean show_in_account_balances;

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

    public static Finder<Integer, Tag> find = new Finder<>(
            Tag.class
    );

    public static Tag findById(int id, Organization org) {
        return find.query().where().eq("organization", org)
            .eq("id", id).findOne();
    }

    public static Tag create(String title, Organization org) {
        Tag result = new Tag();
        result.title = title;
        result.organization = org;
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
        title = form.field("title").value().get();
        use_student_display = ModelUtils.getBooleanFromFormValue(form.field("use_student_display"));
        show_in_jc = ModelUtils.getBooleanFromFormValue(form.field("show_in_jc"));
        show_in_attendance = ModelUtils.getBooleanFromFormValue(form.field("show_in_attendance"));
        show_in_menu = ModelUtils.getBooleanFromFormValue(form.field("show_in_menu"));
        show_in_account_balances = ModelUtils.getBooleanFromFormValue(form.field("show_in_account_balances"));
        save();
    }

    public static Map<String, List<Tag>> getWithPrefixes(Organization org) {
        Map<String, List<Tag>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (Tag t : find.query().where()
                .eq("organization", org)
                .eq("show_in_menu", true)
                .order("title ASC").findList()) {
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
