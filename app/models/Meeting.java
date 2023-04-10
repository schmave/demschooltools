package models;

import com.avaje.ebean.Model;
import play.libs.Json;

import javax.persistence.*;
import java.util.*;


@Entity
public class Meeting extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "meeting_id_seq")
    public Integer id;

    public Date date;

    @ManyToOne()
    public Organization organization;

    @OneToMany(mappedBy="meeting")
    public List<PersonAtMeeting> people_at_meeting;

    @OneToMany(mappedBy="meeting")
    @OrderBy("case_number ASC")
    public List<Case> cases;

    @ManyToMany
    @JoinTable(name="case_meeting",
        inverseJoinColumns = @JoinColumn(name="case_id",referencedColumnName = "id"),
        joinColumns = @JoinColumn(name="meeting_id", referencedColumnName="id"))
    public List<Case> additional_cases;

    public static Finder<Integer, Meeting> find = new Finder<>(
            Meeting.class
    );

    public static Meeting findById(int id) {
        return find.where().eq("organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }

    public String getJsonPeople(int role) {
        List<Map<String, String> > result = new ArrayList<>();

        for (PersonAtMeeting p : people_at_meeting) {
            if (p.role == role) {
                HashMap<String, String> map = new HashMap<>();
				map.put("name", p.person.getDisplayName());
                map.put("id", "" + p.person.person_id);
                result.add(map);
            }
        }

        return Json.stringify(Json.toJson(result));
    }

    public static Meeting create(Date d)
    {
        Meeting result = new Meeting();
        result.date = d;
        result.organization = Organization.getByHost();
        return result;
    }

    public void prepareForEditing() {
        if (OrgConfig.get().org.enable_case_references) {
            for (Case c : cases) {
                for (Charge charge : c.charges) {
                    charge.is_referenced = charge.referencing_charges.size() > 0 || charge.referencing_cases.size() > 0;
                }
            }
        }
    }
}
