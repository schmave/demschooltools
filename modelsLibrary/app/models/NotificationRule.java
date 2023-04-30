package models;

import java.util.*;

import javax.persistence.*;

import io.ebean.*;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class NotificationRule extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_rule_id_seq")
    private int id;

    @ManyToOne()
    @JoinColumn(name="tag_id")
    private Tag tag;

    public static final int TYPE_TAG = 0;
    // public static final int TYPE_DONATION = 1;
    public static final int TYPE_COMMENT = 2;
    public static final int TYPE_SCHOOL_MEETING = 3;

    private int theType;

    private String email;

    @ManyToOne()
    @JoinColumn(name="organization_id")
    private Organization organization;

    public static Finder<Integer, NotificationRule> find = new Finder<>(
            NotificationRule.class
    );

    public static NotificationRule findById(int id, Organization org) {
        return find.query().where().eq("organization", org)
            .eq("id", id).findOne();
    }

    public static List<NotificationRule> findByType(int type, Organization org) {
        return find.query().where().eq("organization", org)
            .eq("theType", type).findList();
    }

    public static NotificationRule create(int type, Tag tag, String email, Organization org) {
        NotificationRule result = new NotificationRule();

        result.organization = org;

        result.theType = type;
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