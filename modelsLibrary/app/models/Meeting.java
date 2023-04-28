package models;

import io.ebean.*;
import play.libs.Json;

import javax.persistence.*;
import javax.persistence.OrderBy;
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

    public static Meeting findById(int id, Organization org) {
        return find.query().where().eq("organization", org)
            .eq("id", id).findOne();
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

    public static Meeting create(Date d, Organization org)
    {
        Meeting result = new Meeting();
        result.date = d;
        result.organization = org;
        return result;
    }

    public void prepareForEditing(Organization org) {
        if (org.getEnableCaseReferences()) {
            for (Case c : cases) {
                for (Charge charge : c.charges) {
                    charge.is_referenced = charge.referencing_charges.size() > 0 || charge.referencing_cases.size() > 0;
                }
            }
        }
    }
}