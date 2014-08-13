package models;

import java.util.*;

public class PersonHistory {

    public class Record {
        public Date most_recent_charge;
        public Entry rule;
        public int count;
    }

    public ArrayList<Record> rule_records;

    public PersonHistory(Person p) {
        HashMap<Entry, Record> records = new HashMap<Entry, Record>();
        for (Charge c : p.charges) {
            Record r = records.get(c.rule);
            if (r == null) {
                r = new Record();
                r.most_recent_charge = c.the_case.date;
                if (r.most_recent_charge == null) {
                    r.most_recent_charge = c.the_case.meeting.date;
                }
                r.count = 1;
                r.rule = c.rule;
                records.put(c.rule, r);
            } else {
                r.count++;
            }
        }

        rule_records = new ArrayList<Record>(records.values());
    }


}
