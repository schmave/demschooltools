package models;

import controllers.Application;

import java.util.*;

import javax.persistence.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;
import com.avaje.ebean.validation.NotNull;

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

    public String gender;

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

    @Transient
    public List<Tag> tags;

    @OneToMany(mappedBy="person")
    public List<Comment> comments;

    @OneToMany(mappedBy="person")
    public List<CompletedTask> completed_tasks;

    // is_family is true if this Person object represents
    // a family, not a single person.
    @NotNull
    public boolean is_family;

    // family ID
    @ManyToOne()
    public Person family;

    @OneToMany(mappedBy="family")
    public List<Person> family_members;

    public static Finder<Integer,Person> find = new Finder(
        Integer.class, Person.class
    );

    public static List<Person> all() {
        return find.where().eq("is_family", Boolean.FALSE).orderBy("last_name, first_name ASC").findList();
    }

    public void attachToPersonAsFamily(String id_string) {
        if (id_string == null || id_string.equals("")) {
            return;
        }

        Person other_family_member = Person.find.ref(Integer.parseInt(id_string));

        if (other_family_member.family != null) {
            // Attach to their existing family.
            this.family = other_family_member.family;
        } else {
            // Create a new family to hold these two people.
            Person family = new Person();
            family.is_family = true;
            family.save();
            this.family = family;
            other_family_member.family = family;
            other_family_member.update();
        }
    }

    public void addPhoneNumbers(Form<Person> form) {
        phone_numbers = new ArrayList<PhoneNumber>();

        for (int i = 1; i <= 3; i++) {
            if (!form.field("number_" + i).value().equals("")) {
                phone_numbers.add(PhoneNumber.create(
                    form.field("number_" + i).value(),
                    form.field("number_" + i + "_comment").value(),
                    this));
            }
        }
    }

    public static Person create(Form<Person> form) {
        Person person = form.get();
        person.is_family = false;
        person.attachToPersonAsFamily(form.field("same_family_id").value());

        person.save();
        person.addPhoneNumbers(form);
        return person;
    }

    public static Person updateFromForm(Form<Person> form) {
        Person p = form.get();
        p.attachToPersonAsFamily(form.field("same_family_id").value());

        // Remove all existing phone numbers -- they are not loaded
        // into the object, so we have to go direct to the DB to get them.
        List<PhoneNumber> numbers = PhoneNumber.find.where().eq("person_id", p.person_id).findList();
        for (PhoneNumber number : numbers) {
            number.delete();
        }

        p.addPhoneNumbers(form);

        p.update();
        return p;
    }

    public static void delete(Integer id) {
        find.ref(id).delete();
    }

    public boolean isStudent()
    {
        return tags.contains(Tag.find.where().eq("title", "Intent to Enroll").findUnique());
    }

    public Form<Person> fillForm() {
        HashMap<String, String> data = new HashMap<String, String>();
        int i = 1;
        for (PhoneNumber number : phone_numbers) {
            data.put("number_" + i, number.number);
            data.put("number_" + i + "_comment", number.comment);
            i++;
        }

        // This is how you create a hybrid form based
        // on both a map of values and an object. Crazy.
        return new Form<Person>(null, Person.class, data,
            new HashMap<String,List<ValidationError>>(),
            Some(this), null);
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

    public CompletedTask completedTask(Task t) {
        for (CompletedTask ct : completed_tasks) {
            if (ct.task.id == t.id) {
                return ct;
            }
        }
        return null;
    }

    public int numSiblings()
    {
        if (this.family == null)
        {
            return 0;
        }
        else
        {
            int num_siblings = 0;
            for (Person p : this.family.family_members)
            {
                if (this != p &&
                    p.dob != null &&
                    Application.calcAge(p) < 18)
                {
                    num_siblings++;
                }
            }
            return num_siblings;
        }
    }

    public List<String> familyAddresses()
    {
        Set<Person> candidates = new HashSet<Person>();
        candidates.add(this);
        if (family != null)
        {
            candidates.addAll(family.family_members);
        }

        ArrayList<String> addresses = new ArrayList<String>();
        for (Person p : candidates)
        {
            if (p.address.length() > 0)
            {
                addresses.add(p.first_name + " " + p.last_name +
                    ": " + p.address + ", " + p.city + ", " + p.state + ", " +
                    p.zip);
            }
        }
        return addresses;
    }

    public List<String> familyPhoneNumbers()
    {
        Set<Person> candidates = new HashSet<Person>();
        candidates.add(this);
        if (family != null)
        {
            candidates.addAll(family.family_members);
        }

        ArrayList<String> numbers = new ArrayList<String>();
        for (Person p : candidates)
        {
            for (PhoneNumber num : p.phone_numbers)
            {
                String label = p.first_name + " " + p.last_name;
                if (num.comment.length() > 0)
                {
                    label = label + "(" + num.comment + ")";
                }
                numbers.add(label + ": " + num.number);
            }
        }

        return numbers;
    }

    public int compareTo(Person other) {
        int last_name_compare = last_name.compareTo(other.last_name);
        if (last_name_compare == 0) {
            return first_name.compareTo(other.first_name);
        }
        return last_name_compare;
    }
}
