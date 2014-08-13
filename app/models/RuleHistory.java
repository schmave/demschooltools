package models;

import java.util.*;

public class RuleHistory {

    public class Record {
        public Date most_recent_charge;
        public Person person;
        public int count;
    }

    public ArrayList<Record> rule_records;

    public RuleHistory(Entry rule) {
        HashMap<Person, Record> records = new HashMap<Person, Record>();
        for (Charge c : rule.charges) {
            Record r = records.get(c.person);
            if (r == null) {
                r = new Record();
                r.most_recent_charge = c.the_case.date;
                if (r.most_recent_charge == null) {
                    r.most_recent_charge = c.the_case.meeting.date;
                }
                r.count = 1;
                r.person = c.person;
                records.put(c.person, r);
            } else {
                r.count++;
            }
        }

        rule_records = new ArrayList<Record>(records.values());
    }


}
