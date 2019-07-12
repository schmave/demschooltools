package models;

public class CheckinPerson {
    
    public int person_id;
    public String pin;
    public String name;

    public CheckinPerson(Person person) {
        person_id = person.person_id;
        pin = person.pin;
        name = person.getDisplayName();
    }
}
