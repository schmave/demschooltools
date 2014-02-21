package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.*;

import org.codehaus.jackson.annotate.*;

import play.data.*;
import play.db.ebean.Model;
import play.db.ebean.Model.Finder;

@Entity
public class Rule extends Model implements Comparable<Rule> {
    @Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rule_id_seq")
    public Integer id;

    public String title = "";

    @OneToMany(mappedBy="rule")
    @JsonIgnore
    @OrderBy("id DESC")
    public List<Charge> charges;
	
	public boolean removed = false;

    public static Finder<Integer, Rule> find = new Finder(
        Integer.class, Rule.class
    );

    public int compareTo(Rule other) {
        return title.compareTo(other.title);
    }
	
	public void updateFromForm(Form<Rule> form) {
		title = form.field("title").value();
		if (form.field("removed").value().equals("true")) {
			removed = true;
		} else {
			removed = false;
		}
		save();
	}
	
	public static Rule create(Form<Rule> form) {
		Rule result = form.get();
		result.updateFromForm(form);
		result.save();
		return result;
	}
}
