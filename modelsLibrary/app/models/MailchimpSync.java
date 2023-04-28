package models;

import io.ebean.*;

import javax.persistence.*;
import java.util.Date;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class MailchimpSync extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mailchimp_sync_id_seq")
    private int id;

    @ManyToOne()
    @JoinColumn(name="tag_id")
    private Tag tag;

    private String mailchimpListId = "";

    private Date lastSync;

    private boolean syncLocalAdds;

    private boolean syncLocalRemoves;

    public static Finder<Integer, MailchimpSync> find = new Finder<>(
            MailchimpSync.class
    );

    public static MailchimpSync create(Tag t, String mailchimpListId, boolean syncLocalAdds, boolean syncLocalRemoves) {
        MailchimpSync result = new MailchimpSync();
        result.tag = t;
        result.mailchimpListId = mailchimpListId;
        result.syncLocalAdds = syncLocalAdds;
        result.syncLocalRemoves = syncLocalRemoves;

        result.save();
        return result;
    }

    public void updateLastSync(Date d) {
        this.lastSync = d;
        this.save();
    }
}