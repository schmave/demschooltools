package models;

import java.util.*;

import controllers.Application;

public class PersonHistory {

    public static class Record {
        public Date most_recent_charge;
        public Entry rule;
        public int count;
    }

    public ArrayList<Record> rule_records;
	public Date start_date;
	public Date end_date;
	
	public Map<Entry, List<Charge>> charges_by_rule;
	public List<Charge> charges_by_date;
	
    public PersonHistory(Person p, boolean include_today, Date start_date, Date end_date) {
        HashMap<Entry, Record> records = new HashMap<Entry, Record>();
		charges_by_date = new ArrayList<Charge>();
		charges_by_rule = new TreeMap<Entry, List<Charge>>();

        Date today = new Date();
		if (end_date == null) {
			end_date = new Date(start_date.getTime() + 365L * 24 * 60 * 60 * 1000);
		}
		this.start_date = start_date;
		this.end_date = end_date;

        Collections.sort(p.charges);
        Collections.reverse(p.charges);

        for (Charge c : p.charges) {
            Date d = c.the_case.meeting.date;
            if (d.before(start_date) || d.after(end_date) ||
                (!include_today && d.getDate() == today.getDate() &&
                 d.getMonth() == today.getMonth() && d.getYear() == today.getYear())) {
                continue;
            }

			if (c.rule != null) {
				List<Charge> cur_list = charges_by_rule.get(c.rule);
				if (cur_list == null) {
					cur_list = new ArrayList<Charge>();
					charges_by_rule.put(c.rule, cur_list);
				}
				cur_list.add(c);
			}
			
			charges_by_date.add(c);

            Record r = records.get(c.rule);
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
