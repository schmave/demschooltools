package models;

import java.util.*;

public class CheckinData {

	public List<CheckinPerson> people;
	public List<String> absence_codes;

	public CheckinData(List<CheckinPerson> people, List<String> absence_codes) {
		this.people = people;
		this.absence_codes = absence_codes;
	}
}