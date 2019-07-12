package models;

public class CheckinMessage {
    
    public long time;
    public int person_id;
    public boolean is_arriving;

    public CheckinMessage(long time, int person_id, boolean is_arriving) {
        this.time = time;
        this.person_id = person_id;
        this.is_arriving = is_arriving;
    }
}
