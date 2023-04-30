package models;

import java.util.*;

public class RuleHistory {

  public static class Record {
    public Date most_recent_charge;
    public Person person;
    public int count;
  }

  public Date start_date;
  public Date end_date;
  public ArrayList<Record> rule_records;
  public List<Charge> charges;

  public RuleHistory(Entry rule, boolean include_today, Date start_date, Date end_date) {
    charges = new ArrayList<>();
    HashMap<Person, Record> records = new HashMap<>();

    Date today = new Date();
    if (end_date == null) {
      end_date = new Date(start_date.getTime() + 365L * 24 * 60 * 60 * 1000);
    }
    this.start_date = start_date;
    this.end_date = end_date;

    Collections.sort(rule.charges);
    Collections.reverse(rule.charges);

    for (Charge c : rule.charges) {
      Date d = c.getTheCase().getMeeting().getDate();
      if (d.before(start_date)
          || d.after(end_date)
          || (!include_today
              && d.getDate() == today.getDate()
              && d.getMonth() == today.getMonth()
              && d.getYear() == today.getYear())) {
        continue;
      }

      charges.add(c);

      Record r = records.get(c.getPerson());
      if (r == null) {
        r = new Record();
        r.most_recent_charge = c.getTheCase().getMeeting().getDate();
        r.count = 1;
        r.person = c.getPerson();
        records.put(c.getPerson(), r);
      } else {
        r.count++;
      }
    }

    rule_records = new ArrayList<>(records.values());
  }
}
