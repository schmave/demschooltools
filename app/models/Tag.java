package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.*;

import com.avaje.ebean.Model;
import com.avaje.ebean.Model.Finder;

@Entity
public class Tag extends Model {
    @Id
    public Integer id;

    public String title;

    public boolean use_student_display;

    @ManyToMany
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
        Integer.class, Tag.class
    );

    public static Tag findById(int id) {
        return find.where().eq("organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }

    public static Tag create(String title) {
        Tag result = new Tag();
        result.title = title;
        result.organization = Organization.getByHost();

        result.save();
        return result;
    }

    public static Map<String, List<Tag>> getWithPrefixes() {
        Map<String, List<Tag>> result = new TreeMap<String, List<Tag>>();

        for (Tag t : find.where()
                .eq("organization", Organization.getByHost())
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
