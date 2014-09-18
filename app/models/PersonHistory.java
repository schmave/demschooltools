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
        this(p, true);
    }

    public PersonHistory(Person p, boolean include_today) {
        HashMap<Entry, Record> records = new HashMap<Entry, Record>();

        Date today = new Date();

        for (Charge c : p.charges) {
            Record r = records.get(c.rule);

            Date d = c.the_case.meeting.date;
            if (!include_today && d.getDate() == today.getDate() &&
                d.getMonth() == today.getMonth() && d.getYear() == today.getYear()) {
                continue;
            }

            if (r == null) {
                r = new Record();
                r.most_recent_charge = c.the_case.meeting.date;
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
