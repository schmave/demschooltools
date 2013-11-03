package models;

import java.util.*;

import javax.persistence.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;
import com.avaje.ebean.validation.NotNull;

import controllers.Application;

import org.codehaus.jackson.annotate.*;

import play.data.*;
import play.data.validation.Constraints.*;
import play.data.validation.ValidationError;
import play.db.ebean.*;
import static play.libs.F.*;

@Entity
public class Person extends Model implements Comparable<Person> {
    @Id
    public Integer person_id;

    public String first_name, last_name;

    @Column(columnDefinition = "TEXT")
    public String notes;

    @NotNull
    public String gender = "Unknown";

    // phone number references
    @OneToMany(mappedBy="owner")
    public List<PhoneNumber> phone_numbers;

    public String address;
    public String city;
    public String state;
    public String zip;
    public String neighborhood;

    // email address
    public String email;

    public Date dob;
    public Date approximate_dob;

	public String previous_school;
	public String school_district;

	private String display_name = "";

    @Transient
    public List<Tag> tags;

    // is_family is true if this Person object represents
    // a family, not a single person.
    @NotNull
    public boolean is_family;

    //// family ID
    //@ManyToOne()
    //public Person family;

    //@OneToMany(mappedBy="family")
    //public List<Person> family_members;
    //

    @OneToMany(mappedBy="person")
    @JsonIgnore
    @OrderBy("id DESC")
    public List<Charge> charges;

    public static Finder<Integer,Person> find = new Finder(
        Integer.class, Person.class
    );

    public static List<Person> all() {
        return find.where().eq("is_family", Boolean.FALSE).orderBy("last_name, first_name ASC").findList();
    }


    // called by PersonController
    void loadTags() {
        RawSql rawSql = RawSqlBuilder.
            parse("SELECT tag.id, tag.title from person join person_tag pt on person.person_id = pt.person_id join tag on pt.tag_id=tag.id").
            create();

        tags = Ebean.find(Tag.class).setRawSql(rawSql).
            where().eq("person.person_id", person_id).findList();
        if (tags == null) {
            tags = new ArrayList<Tag>();
        }
    }

	public String getDisplayName()
	{
		if (display_name.equals("")) {
			return first_name;
		}
		else {
			return display_name;
		}
	}

    public int compareTo(Person other) {
        int last_name_compare = last_name.compareTo(other.last_name);
        if (last_name_compare == 0) {
            return first_name.compareTo(other.first_name);
        }
        return last_name_compare;
    }
}
