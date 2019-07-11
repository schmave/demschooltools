package models;

public class CheckinPerson {
    
    public int person_id;
    public String pin;
    public String name;
    public int records_today;

    public CheckinPerson(Person person) {
        person_id = person.person_id;
        pin = person.pin;
        name = person.getDisplayName();
        records_today = 0;
    }
}
