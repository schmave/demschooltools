package models;

import com.avaje.ebean.validation.NotNull;

import java.util.*;

import javax.persistence.*;

import play.data.*;
import play.data.validation.Constraints.*;
import com.avaje.ebean.Model;


@Entity
public class MailchimpSync extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mailchimp_sync_id_seq")
    public int id;

    @NotNull
    @ManyToOne()
    @JoinColumn(name="tag_id")
    public Tag tag;

    @NotNull
    public String mailchimp_list_id;

    public Date last_sync;

    public boolean sync_local_adds;

    public boolean sync_local_removes;

    public static Finder<Integer, MailchimpSync> find = new Finder<>(
        Integer.class, MailchimpSync.class
    );

    public static MailchimpSync create(Tag t, String mailchimp_list_id, boolean sync_local_adds, boolean sync_local_removes) {
        MailchimpSync result = new MailchimpSync();
        result.tag = t;
        result.mailchimp_list_id = mailchimp_list_id;
        result.sync_local_adds = sync_local_adds;
        result.sync_local_removes = sync_local_removes;

        result.save();
        return result;
    }

    public void updateLastSync(Date d) {
        this.last_sync = d;
        this.save();
    }
}
