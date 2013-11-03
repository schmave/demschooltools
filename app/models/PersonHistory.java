package models;

import java.util.*;

public class PersonHistory {

    public class Record {
        public Date most_recent_charge;
        public Rule rule;
        public int count;
    }

    public ArrayList<Record> rule_records;

    public PersonHistory(Person p) {
        HashMap<Rule, Record> records = new HashMap<Rule, Record>();
        for (Charge c : p.charges) {
            Record r = records.get(c.rule);
            if (r == null) {
                r = new Record();
                r.most_recent_charge = c.the_case.date;
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
