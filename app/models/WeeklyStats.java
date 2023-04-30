package models;

import java.util.List;
import java.util.Map;

public class WeeklyStats {
  public static class PersonCounts {

    public PersonCounts() {
      this_period = last_28_days = all_time = 0;
    }

    public PersonCounts addThisPeriod() {
      this_period++;
      return this;
    }

    public PersonCounts addLast28Days() {
      last_28_days++;
      return this;
    }

    public PersonCounts addAllTime() {
      all_time++;
      return this;
    }

    public int this_period;
    public int last_28_days;
    public int all_time;
  }

  public int num_cases;
  public int num_charges;
  public Map<Entry, Integer> rule_counts;
  public Map<Person, PersonCounts> person_counts;
  public List<Person> uncharged_people;
}
