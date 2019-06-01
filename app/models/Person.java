package models;

import java.util.*;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.FetchConfig;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;

import controllers.*;

import play.Logger;
import play.data.*;
import play.data.validation.Constraints.*;
import play.data.validation.ValidationError;
import com.avaje.ebean.Model;
import static play.libs.F.*;

@Entity
public class Person extends Model implements Comparable<Person> {
    @Id
    public Integer person_id;

    public String first_name="";
    public String last_name="";

    @Column(columnDefinition = "TEXT")
    public String notes="";

    @ManyToOne()
    public Organization organization;

    public String gender = "Unknown";

    // phone number references
    @OneToMany(mappedBy="owner")
    @JsonIgnore
    public List<PhoneNumber> phone_numbers;

    public String address="";
    public String city="";
    public String state="";
    public String zip="";
    public String neighborhood="";

    // email address
    public String email="";

    public Date dob;
    public Date approximate_dob;

	public String display_name = "";

	public String previous_school = "";
	public String school_district = "";

    @JsonIgnore
    @ManyToMany(mappedBy = "people")
    public List<Tag> tags;

    @OneToMany(mappedBy="person")
    @JsonIgnore
    public List<Comment> comments;

    @OneToMany(mappedBy="person")
    @JsonIgnore
    public List<CompletedTask> completed_tasks;

    // is_family is true if this Person object represents
    // a family, not a single person.
    public boolean is_family;

    @OneToMany(mappedBy="person")
    @JsonIgnore
    @OrderBy("id DESC")
    public List<Charge> charges;

    @OneToMany(mappedBy="person")
    @JsonIgnore
    public List<PersonAtCase> cases_involved_in;

    // family ID
    @ManyToOne()
    @JsonIgnore
    public Person family;

    @OneToMany(mappedBy="family")
    @JsonIgnore
    public List<Person> family_members;

    @OneToMany(mappedBy="person")
    @JsonIgnore
    public List<AttendanceDay> attendance_days;
    @OneToMany(mappedBy="person")
    @JsonIgnore
    public List<AttendanceWeek> attendance_weeks;

    @OneToMany(mappedBy="person")
    @JsonIgnore
    public List<Account> accounts;

	public String grade = "";

    static Set<String> fieldsToUpdateExplicitly = new HashSet<String>();

    {
        fieldsToUpdateExplicitly.add("dob");
        fieldsToUpdateExplicitly.add("approximate_dob");
    }

    public static Finder<Integer,Person> find = new Finder<Integer,Person>(
        Person.class
    );

    public static Person findById(int id) {
        return find.where().eq("organization", Organization.getByHost())
            .eq("person_id", id).findUnique();
    }

    public static Person findByIdWithJCData(int id) {
        return find
            .fetch("charges", new FetchConfig().query())
            .fetch("charges.the_case", new FetchConfig().query())
            .fetch("charges.the_case.meeting", new FetchConfig().query())
            .fetch("charges.the_case.charges", new FetchConfig().query())
            .fetch("charges.the_case.charges.person", new FetchConfig().query())
            .fetch("charges.the_case.charges.rule", new FetchConfig().query())
            .fetch("charges.the_case.charges.rule.section", new FetchConfig().query())
            .fetch("charges.the_case.charges.rule.section.chapter", new FetchConfig().query())
            .fetch("cases_involved_in", new FetchConfig().query())
            .fetch("cases_involved_in.the_case.people_at_case", new FetchConfig().query())
            .fetch("cases_involved_in.the_case", new FetchConfig().query())
            .fetch("cases_involved_in.the_case.meeting", new FetchConfig().query())
            .fetch("cases_involved_in.the_case.charges", new FetchConfig().query())
            .fetch("cases_involved_in.the_case.charges.person", new FetchConfig().query())
            .fetch("cases_involved_in.the_case.charges.rule", new FetchConfig().query())
            .fetch("cases_involved_in.the_case.charges.rule.section", new FetchConfig().query())
            .fetch("cases_involved_in.the_case.charges.rule.section.chapter", new FetchConfig().query())
            .where().eq("organization", Organization.getByHost())
            .eq("person_id", id).findUnique();
    }

    @JsonIgnore
    public String getInitials() {
        String result = "";
        if (this.first_name != null && this.first_name.length() > 0) {
            result += this.first_name.charAt(0);
        }
        if (this.last_name != null && this.last_name.length() > 0) {
            result += this.last_name.charAt(0);
        }
        return result;
    }

    public boolean hasMultipleAddresses() {
        if (!this.address.isEmpty() || this.family == null) {
            return false;
        }
        int addressCount = 0;
        for (Person p2 : this.family.family_members) {
            if (!p2.address.isEmpty()) {
                addressCount++;
            }
        }
        return addressCount > 1;
    }

    @Override
    public int hashCode() {
        return person_id.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Person) {
            return this.person_id.equals(((Person) other).person_id);
        } else {
            return false;
        }
    }

    public static List<Person> all() {
        return find.where()
            .eq("organization", Organization.getByHost())
            .eq("is_family", Boolean.FALSE)
            .orderBy("last_name, first_name ASC")
            .fetch("phone_numbers", new FetchConfig().query())
            .findList();
    }

    @JsonIgnore
    public List<Charge> getThisYearCharges() {
        Date beginning_of_year = Application.getStartOfYear();

        List<Charge> result = new ArrayList<Charge>();
        for (Charge c : charges) {
            if (c.the_case.meeting.date.after(beginning_of_year)) {
                result.add(c);
            }
        }

        return result;
    }

    @JsonIgnore
    public List<Case> getThisYearCasesWrittenUp() {
        Date beginning_of_year = Application.getStartOfYear();

        List<Case> result = new ArrayList<Case>();
        for (PersonAtCase pac : cases_involved_in) {
            if (pac.role == PersonAtCase.ROLE_WRITER &&
                pac.the_case.meeting.date.after(beginning_of_year)) {
                result.add(pac.the_case);
            }
        }

        return result;
    }

    @JsonIgnore
    public List<Case> getCasesWrittenUp() {
        List<Case> result = new ArrayList<Case>();
        for (PersonAtCase pac : cases_involved_in) {
            if (pac.role == PersonAtCase.ROLE_WRITER) {
                result.add(pac.the_case);
            }
        }

        return result;
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
            family.organization = Organization.getByHost();
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

	public void trimSpaces() {
		first_name = first_name.trim();
		last_name = last_name.trim();
		address = address.trim();
		city = city.trim();
		state = state.trim();
		zip = zip.trim();
		neighborhood = neighborhood.trim();
		email = email.trim();
		display_name = display_name.trim();
		previous_school = previous_school.trim();
		school_district = school_district.trim();
	}

    public static Person create(Form<Person> form) {
        java.util.Map<java.lang.String,java.lang.String> data = form.data();
        Person person = form.get();
        person.is_family = false;
        person.attachToPersonAsFamily(form.field("same_family_id").value());
        person.organization = Organization.getByHost();
		person.trimSpaces();
        person.save();

        if (OrgConfig.get().org.show_accounting) {
            Account.create(AccountType.PersonalChecking, "", person);
        }

        person.addPhoneNumbers(form);
        return person;
    }

    public static Person updateFromForm(Form<Person> form) {
        Person p = form.get();
        p.attachToPersonAsFamily(form.field("same_family_id").value());

        if (!p.is_family) {
            // Remove all existing phone numbers -- they are not loaded
            // into the object, so we have to go direct to the DB to get them.
            List<PhoneNumber> numbers = PhoneNumber.find.where().eq("person_id", p.person_id).findList();
            for (PhoneNumber number : numbers) {
                number.delete();
            }

            p.addPhoneNumbers(form);

            Person old_p = Person.findById(p.person_id);
            if (!old_p.email.equals(p.email)) {
                PersonChange.create(old_p, p.email);
            }
        }

		p.trimSpaces();
        p.update();
        Ebean.update(p);
        return p;
    }

    @JsonIgnore
    public String nonEmptyName() {
        String result = this.first_name + " " + this.last_name;
        if (result.trim().equals("")) {
            return "<Person ID " + this.person_id + ">";
        }
        return result;
    }

    public static void delete(Integer id) {
        Person person = find.ref(id);
        for (Account account : person.accounts) {
            account.delete();
        }
        person.delete();
    }

    public boolean isStudent()
    {
        for (Tag t : tags) {
            if (t.use_student_display) {
                return true;
            }
        }
        return false;
    }

    public Form<Person> fillForm() {
        HashMap<String, String> data = new HashMap<String, String>();
        int i = 1;
        for (PhoneNumber number : phone_numbers) {
            data.put("number_" + i, number.number);
            data.put("number_" + i + "_comment", number.comment);
            i++;
        }

        if (this.dob !=  null) {
            // The default form filler writes the DOB in an unparseable
            // format, so override it with this.
            data.put("dob", Application.forDateInput(this.dob));
        }

        // This is how you create a hybrid form based
        // on both a map of values and an object. Crazy.
        return new Form<Person>(null, Person.class, data,
            new HashMap<String,List<ValidationError>>(),
            Optional.of(this), null, null, null);
    }

    // called by PersonController
    public void loadTags() {
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

    public boolean searchStringMatches(String term)
    {
        return first_name.toLowerCase().contains(term) ||
                last_name.toLowerCase().contains(term) ||
				display_name.toLowerCase().contains(term);
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
                    CRM.calcAge(p) < 18)
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

    public boolean hasAccount(AccountType type) {
        return accounts.stream().anyMatch(a -> a.type == type);
    }

    public String toString() {
        return this.first_name + " " + this.last_name;
    }

    public int compareTo(Person other) {
        int last_name_compare = last_name.compareTo(other.last_name);
        if (last_name_compare == 0) {
            return first_name.compareTo(other.first_name);
        }
        return last_name_compare;
    }

    public static Comparator<Person> SORT_DISPLAY_NAME = new Comparator<Person>() {
            @Override
            public int compare(Person o1, Person o2) {
                return o1.getDisplayName().compareTo(o2.getDisplayName());
            }
        };

    public static Comparator<Person> SORT_FIRST_NAME = new Comparator<Person>() {
            @Override
            public int compare(Person o1, Person o2) {
                int first_name_compare = o1.first_name.compareTo(o2.first_name);
                if (first_name_compare == 0) {
                    return o1.last_name.compareTo(o2.first_name);
                }
                return first_name_compare;
            }
        };
}
