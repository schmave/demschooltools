package models;

import java.util.*;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.FetchConfig;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;

import controllers.*;

import play.data.*;
import play.data.validation.Constraints.*;
import play.data.validation.ValidationError;
import com.avaje.ebean.Model;
import static play.libs.F.*;

@Entity
public class Entry extends Model implements Comparable<Entry> {
    @Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "entry_id_seq")
    public Integer id;

    public String title = "";
	public String num = "";

    @Column(columnDefinition = "TEXT")
    public String content = "";

    @ManyToOne()
    public Section section;

	public boolean deleted;

	@OneToMany(mappedBy="rule")
    @JsonIgnore
    @OrderBy("id DESC")
    public List<Charge> charges;

    @JsonIgnore
    @OneToMany(mappedBy="entry")
    @OrderBy("date_entered ASC")
    public List<ManualChange> changes;

    public static Finder<Integer,Entry> find = new Finder<Integer,Entry>(
        Entry.class
    );

    public static Entry findById(int id) {
        return find.where().eq("section.chapter.organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }

    public static Entry findByIdWithJCData(int id) {
        return find
            .fetch("charges", new FetchConfig().query())
            .fetch("charges.the_case", new FetchConfig().query())
            .fetch("charges.the_case.meeting", new FetchConfig().query())
            .fetch("charges.the_case.charges", new FetchConfig().query())
            .fetch("charges.the_case.charges.person", new FetchConfig().query())
            .fetch("charges.the_case.charges.rule", new FetchConfig().query())
            .fetch("charges.the_case.charges.rule.section", new FetchConfig().query())
            .fetch("charges.the_case.charges.rule.section.chapter", new FetchConfig().query())
            .where().eq("section.chapter.organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }

    @JsonIgnore
    public List<Charge> getThisYearCharges() {
        Date beginning_of_year = Application.getStartOfYear();

        List<Charge> result = new ArrayList<Charge>();
        for (Charge c : charges) {
            if (c.the_case.meeting.date.after(beginning_of_year)) {
                result.add(c);
            }
        }

        return result;
    }

    public String getNumber() {
        return section.getNumber() + "." + num;
    }

    void updateFromForm(Form<Entry> form, boolean make_change_record) {
        String old_num = (make_change_record ? getNumber() : "");
        String old_title = title;
        String old_content = content;

        title = form.field("title").value();
        content = form.field("content").value();
        num = form.field("num").value();
        String deleted_val = form.field("deleted").value();

        boolean previous_deleted = deleted;
        deleted = deleted_val != null && deleted_val.equals("true");

        section = Section.find.byId(Integer.parseInt(form.field("section.id").value()));

        if (make_change_record) {
            if (!previous_deleted && deleted) {
                ManualChange.recordEntryDelete(this);
            } else if (previous_deleted && !deleted) {
                ManualChange.recordEntryCreate(this);
            } else {
                ManualChange.recordEntryChange(this, old_num, old_title, old_content);
            }
        }

        save();
    }

	public void updateFromForm(Form<Entry> form) {
        updateFromForm(form, true);
	}

	public static Entry create(Form<Entry> form) {
		Entry result = form.get();
		result.updateFromForm(form, false);
        ManualChange.recordEntryCreate(result);
		result.save();
		return result;
	}

	public int compareTo(Entry other) {
        return title.compareTo(other.title);
    }

    public static Comparator<Entry> SORT_NUMBER = new Comparator<Entry>() {
            @Override
            public int compare(Entry o1, Entry o2) {
                return o1.getNumber().compareTo(o2.getNumber());
            }
        };
}
