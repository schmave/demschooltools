package models;

import java.util.*;

import javax.persistence.*;

import play.data.*;
import play.data.validation.Constraints.*;
import com.avaje.ebean.Model;


@Entity
public class NotificationRule extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_rule_id_seq")
    public int id;

    @ManyToOne()
    @JoinColumn(name="tag_id")
    public Tag tag;

    public static final int TYPE_TAG = 0;
    // public static final int TYPE_DONATION = 1;
    public static final int TYPE_COMMENT = 2;

    public int the_type;

    public String email;

    @ManyToOne()
    @JoinColumn(name="organization_id")
    public Organization organization;

    public static Finder<Integer, NotificationRule> find = new Finder<Integer, NotificationRule>(
        NotificationRule.class
    );

    public static NotificationRule findById(int id) {
        return find.where().eq("organization", OrgConfig.get().org)
            .eq("id", id).findUnique();
    }

    public static List<NotificationRule> findByType(int type) {
        return find.where().eq("organization", OrgConfig.get().org)
            .eq("the_type", type).findList();
    }

    public static NotificationRule create(int type, Tag tag, String email) {
        NotificationRule result = new NotificationRule();

        result.organization = OrgConfig.get().org;

        result.the_type = type;
        if (type == TYPE_TAG) {
            result.tag = tag;
        } else {
            result.tag = null;
        }

        result.email = email;
        if (result.email == null) {
            result.email = "";
        }

        result.save();
        return result;
    }
}
