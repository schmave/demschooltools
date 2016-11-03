package models;

import java.util.*;

import javax.persistence.*;

import com.avaje.ebean.Model;
import play.data.*;
import play.data.validation.Constraints.*;
import play.db.ebean.*;

@Entity
public class CaseMeeting extends Model {
    public int case_id;
    public int meeting_id;

    public static CaseMeeting create(Case c, Meeting m) {
        CaseMeeting result = new CaseMeeting();
        result.case_id = c.id;
        result.meeting_id = m.id;

        result.save();
        return result;
    }
}
