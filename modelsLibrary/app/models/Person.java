package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.ebean.*;
import java.util.*;
import javax.persistence.*;
import javax.persistence.OrderBy;
import lombok.Getter;
import lombok.Setter;
import play.data.Form;

@Getter
@Setter
@Entity
public class Person extends Model implements Comparable<Person> {
    @Id
    private Integer personId;

    private String firstName="";
    private String lastName="";

    @Column(columnDefinition = "TEXT")
    private String notes="";

    @ManyToOne()
    private Organization organization;

    private String gender = "Unknown";

    // phone number references
    @OneToMany(mappedBy="owner")
    @JsonIgnore
    public List<PhoneNumber> phone_numbers;

    private String address="";
    private String city="";
    private String state="";
    private String zip="";
    private String neighborhood="";

    // email address
    private String email="";

    private Date dob;
    private Date approximateDob;

    private String displayName = "";

    private String previousSchool = "";
    private String schoolDistrict = "";

    private String pin = "";

    @JsonIgnore
    @ManyToMany(mappedBy = "people")
    public List<Tag> tags;

    @OneToMany(mappedBy="person")
    @JsonIgnore
    public List<Comment> comments;

    @OneToMany(mappedBy="person")
    @JsonIgnore
    public List<CompletedTask> completed_tasks;

    // isFamily is true if this Person object represents
    // a family, not a single person.
    private boolean isFamily;

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
    private Person family;

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

    private String grade = "";

    static Set<String> fieldsToUpdateExplicitly = new HashSet<>();

    static {
        fieldsToUpdateExplicitly.add("dob");
        fieldsToUpdateExplicitly.add("approximateDob");
    }

    public static Finder<Integer,Person> find = new Finder<>(
            Person.class
    );

    public static Person findById(int id, Organization org) {
        return find.query().where().eq("organization", org)
            .eq("personId", id).findOne();
    }

    public static Person findByIdWithJCData(int id, Organization org) {
        return find.query()
            .fetch("charges", FetchConfig.ofQuery())
            .fetch("charges.theCase", FetchConfig.ofQuery())
            .fetch("charges.theCase.meeting", FetchConfig.ofQuery())
            .fetch("charges.theCase.charges", FetchConfig.ofQuery())
            .fetch("charges.theCase.charges.person", FetchConfig.ofQuery())
            .fetch("charges.theCase.charges.rule", FetchConfig.ofQuery())
            .fetch("charges.theCase.charges.rule.section", FetchConfig.ofQuery())
            .fetch("charges.theCase.charges.rule.section.chapter", FetchConfig.ofQuery())
            .fetch("cases_involved_in", FetchConfig.ofQuery())
            .fetch("cases_involved_in.theCase.people_at_case", FetchConfig.ofQuery())
            .fetch("cases_involved_in.theCase", FetchConfig.ofQuery())
            .fetch("cases_involved_in.theCase.meeting", FetchConfig.ofQuery())
            .fetch("cases_involved_in.theCase.charges", FetchConfig.ofQuery())
            .fetch("cases_involved_in.theCase.charges.person", FetchConfig.ofQuery())
            .fetch("cases_involved_in.theCase.charges.rule", FetchConfig.ofQuery())
            .fetch("cases_involved_in.theCase.charges.rule.section", FetchConfig.ofQuery())
            .fetch("cases_involved_in.theCase.charges.rule.section.chapter", FetchConfig.ofQuery())
            .where().eq("organization", org)
            .eq("personId", id).findOne();
    }

    public int calcAge() {
        return (int)((new Date().getTime() - dob.getTime()) / 1000 / 60 / 60 / 24 / 365.25);
    }

    @JsonIgnore
    public String getInitials() {
        String result = "";
        if (this.firstName != null && this.firstName.length() > 0) {
            result += this.firstName.charAt(0);
        }
        if (this.lastName != null && this.lastName.length() > 0) {
            result += this.lastName.charAt(0);
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
        return personId.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Person) {
            return this.personId.equals(((Person) other).personId);
        } else {
            return false;
        }
    }

    public static List<Person> all(Organization org) {
        return find.query()
                .fetch("phone_numbers", FetchConfig.ofQuery())
                .where()
            .eq("organization", org)
            .eq("isFamily", Boolean.FALSE)
            .orderBy("lastName, firstName ASC")
            .findList();
    }

    @JsonIgnore
    public List<Charge> getThisYearCharges() {
        Date beginning_of_year = ModelUtils.getStartOfYear();

        List<Charge> result = new ArrayList<>();
        for (Charge c : charges) {
            if (c.getTheCase().getMeeting().getDate().after(beginning_of_year)) {
                result.add(c);
            }
        }

        return result;
    }

    @JsonIgnore
    public List<Case> getThisYearCasesWrittenUp() {
        Date beginning_of_year = ModelUtils.getStartOfYear();

        List<Case> result = new ArrayList<>();
        for (PersonAtCase pac : cases_involved_in) {
            if (pac.getRole() == PersonAtCase.ROLE_WRITER &&
                pac.getTheCase().getMeeting().getDate().after(beginning_of_year)) {
                result.add(pac.getTheCase());
            }
        }

        return result;
    }

    public void attachToPersonAsFamily(String id_string, Organization org) {
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
            family.isFamily = true;
            family.organization = org;
            family.save();
            this.family = family;
            other_family_member.family = family;
            other_family_member.update();
        }
    }

    public void addPhoneNumbers(Form<Person> form) {
        phone_numbers = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            if (!form.field("number_" + i).value().get().equals("")) {
                phone_numbers.add(PhoneNumber.create(
                    form.field("number_" + i).value().get(),
                    form.field("number_" + i + "_comment").value().get(),
                    this));
            }
        }
    }

	public void trimSpaces() {
		firstName = firstName.trim();
		lastName = lastName.trim();
		address = address.trim();
		city = city.trim();
		state = state.trim();
		zip = zip.trim();
		neighborhood = neighborhood.trim();
		email = email.trim();
		displayName = displayName.trim();
		previousSchool = previousSchool.trim();
		schoolDistrict = schoolDistrict.trim();
	}

    public static Person create(Form<Person> form, Organization org) {
        Person person = form.get();
        person.isFamily = false;
        person.attachToPersonAsFamily(form.field("same_family_id").value().get(), org);
        person.organization = org;
		person.trimSpaces();
        person.save();

        if (org.getShowAccounting()) {
            Account.create(AccountType.PersonalChecking, "", person, org);
        }

        person.addPhoneNumbers(form);
        return person;
    }

    public static Person updateFromForm(Form<Person> form, Organization org) {
        Person p = form.get();
        p.attachToPersonAsFamily(form.field("same_family_id").value().get(), org);

        if (!p.isFamily) {
            // Remove all existing phone numbers -- they are not loaded
            // into the object, so we have to go direct to the DB to get them.
            List<PhoneNumber> numbers = PhoneNumber.find.query().where().eq("personId", p.personId).findList();
            for (PhoneNumber number : numbers) {
                number.delete();
            }

            p.addPhoneNumbers(form);

            Person old_p = Person.findById(p.personId, org);
            if (!old_p.email.equals(p.email)) {
                PersonChange.create(old_p, p.email);
            }
        }

		p.trimSpaces();
        p.update();
        DB.update(p);
        return p;
    }

    @JsonIgnore
    public String nonEmptyName() {
        String result = this.firstName + " " + this.lastName;
        if (result.trim().equals("")) {
            return "<Person ID " + this.personId + ">";
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
            if (t.isUseStudentDisplay()) {
                return true;
            }
        }
        return false;
    }

    public Form<Person> fillForm() {
        HashMap<String, String> data = new HashMap<>();
        int i = 1;
        for (PhoneNumber number : phone_numbers) {
            data.put("number_" + i, number.getNumber());
            data.put("number_" + i + "_comment", number.getComment());
            i++;
        }

        if (this.dob !=  null) {
            // The default form filler writes the DOB in an unparseable
            // format, so override it with this.
            data.put("dob", ModelUtils.forDateInput(this.dob));
        }

        // This is how you create a hybrid form based
        // on both a map of values and an object. Crazy.
        return new Form<>(null, Person.class, data,
                new ArrayList<>(),
                Optional.of(this), null, null, null, null);
    }

    // called by PersonController
    public void loadTags() {
        RawSql rawSql = RawSqlBuilder.
            parse("SELECT tag.id, tag.title from person join person_tag pt on person.personId = pt.personId join tag on pt.tag_id=tag.id").
            create();

        tags = DB.find(Tag.class).setRawSql(rawSql).
            where().eq("person.personId", personId).findList();
    }

	public String getDisplayName()
	{
		if (displayName.equals("")) {
			return firstName;
		}
		else {
			return displayName;
		}
	}

    public boolean searchStringMatches(String term)
    {
        return firstName.toLowerCase().contains(term) ||
                lastName.toLowerCase().contains(term) ||
				displayName.toLowerCase().contains(term);
	}

    public CompletedTask completedTask(Task t) {
        for (CompletedTask ct : completed_tasks) {
            if (ct.getTask().getId().equals(t.getId())) {
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
                    p.calcAge() < 18)
                {
                    num_siblings++;
                }
            }
            return num_siblings;
        }
    }

    public List<String> familyAddresses()
    {
        Set<Person> candidates = new HashSet<>();
        candidates.add(this);
        if (family != null)
        {
            candidates.addAll(family.family_members);
        }

        ArrayList<String> addresses = new ArrayList<>();
        for (Person p : candidates)
        {
            if (p.address.length() > 0)
            {
                addresses.add(p.firstName + " " + p.lastName +
                    ": " + p.address + ", " + p.city + ", " + p.state + ", " +
                    p.zip);
            }
        }
        return addresses;
    }

    public List<String> familyPhoneNumbers()
    {
        Set<Person> candidates = new HashSet<>();
        candidates.add(this);
        if (family != null)
        {
            candidates.addAll(family.family_members);
        }

        ArrayList<String> numbers = new ArrayList<>();
        for (Person p : candidates)
        {
            for (PhoneNumber num : p.phone_numbers)
            {
                String label = p.firstName + " " + p.lastName;
                if (num.getComment().length() > 0)
                {
                    label = label + "(" + num.getComment() + ")";
                }
                numbers.add(label + ": " + num.getNumber());
            }
        }

        return numbers;
    }

    public boolean hasAccount(AccountType type) {
        return accounts.stream().anyMatch(a -> a.getType() == type);
    }

    public String toString() {
        return this.firstName + " " + this.lastName;
    }

    public int compareTo(Person other) {
        int last_name_compare = lastName.compareTo(other.lastName);
        if (last_name_compare == 0) {
            return firstName.compareTo(other.firstName);
        }
        return last_name_compare;
    }

    public static Comparator<Person> SORT_DISPLAY_NAME = Comparator.comparing(Person::getDisplayName);

    public static Comparator<Person> SORT_FIRST_NAME = (o1, o2) -> {
        int first_name_compare = o1.firstName.compareTo(o2.firstName);
        if (first_name_compare == 0) {
            return o1.lastName.compareTo(o2.firstName);
        }
        return first_name_compare;
    };
}