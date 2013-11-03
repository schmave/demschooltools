package models;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.*;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;
import play.libs.Json;


@Entity
public class Meeting extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "meeting_id_seq")
    public Integer id;

    public Date date;

    @OneToMany(mappedBy="meeting")
    public List<PersonAtMeeting> people_at_meeting;

    @OneToMany(mappedBy="meeting")
    @OrderBy("case_number ASC")
    public List<Case> cases;

    public static Finder<Integer, Meeting> find = new Finder(
        Integer.class, Meeting.class
    );

    public String getJsonPeople(int role) {
        List<Map<String, String> > result = new ArrayList<Map<String, String> >();

        for (PersonAtMeeting p : people_at_meeting) {
            if (p.role == role) {
                HashMap<String, String> map = new HashMap<String, String>();
				map.put("name", p.person.getDisplayName());
                map.put("id", "" + p.person.person_id);
                result.add(map);
            }
        }

        return Json.stringify(Json.toJson(result));
    }

	public String getCaseNumberPrefix()
	{
		return new SimpleDateFormat("MM-dd-").format(date);
	}

    public static Meeting create(Date d)
    {
        Meeting result = new Meeting();
        result.date = d;
        return result;
    }
}
