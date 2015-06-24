package models;

import java.util.*;

import javax.persistence.*;

import play.data.*;
import play.data.validation.Constraints.*;
import play.db.ebean.*;

@Entity
public class CaseMeeting extends Model {
    @Id
    public int case_id;

    @Id
    public int meeting_id;

    public static Finder<Integer, CaseMeeting> find = new Finder(
        Integer.class, CaseMeeting.class
    );

    public static CaseMeeting create(Case c, Meeting m) {
        CaseMeeting result = new CaseMeeting();
        result.case_id = c.id;
        result.meeting_id = m.id;

        result.save();
        return result;
    }
}
