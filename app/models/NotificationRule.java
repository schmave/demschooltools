package models;

import java.util.*;

import javax.persistence.*;

import io.ebean.*;


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
    public static final int TYPE_SCHOOL_MEETING = 3;

    public int the_type;

    public String email;

    @ManyToOne()
    @JoinColumn(name="organization_id")
    public Organization organization;

    public static Finder<Integer, NotificationRule> find = new Finder<>(
            NotificationRule.class
    );

    public static NotificationRule findById(int id) {
        return find.query().where().eq("organization", OrgConfig.get().org)
            .eq("id", id).findOne();
    }

    public static List<NotificationRule> findByType(int type) {
        return find.query().where().eq("organization", OrgConfig.get().org)
            .eq("the_type", type).findList();
    }

    public static List<NotificationRule> findByType(int type, OrgConfig org_config) {
        return find.query().where().eq("organization", org_config.org)
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
