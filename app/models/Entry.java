package models;

import java.util.*;

import javax.persistence.*;
import javax.persistence.OrderBy;

import com.fasterxml.jackson.annotation.*;

import io.ebean.FetchConfig;

import controllers.*;

import play.data.*;
import io.ebean.*;

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

    public boolean is_breaking_res_plan;

    public static Finder<Integer,Entry> find = new Finder<>(
            Entry.class
    );

    public static Entry findById(int id, Organization org) {
        return find.query().where().eq("section.chapter.organization", org)
            .eq("id", id).findOne();
    }

    public static Entry findByIdWithJCData(int id, Organization org) {
        return find.query()
            .fetch("charges", new FetchConfig().query())
            .fetch("charges.the_case", new FetchConfig().query())
            .fetch("charges.the_case.meeting", new FetchConfig().query())
            .fetch("charges.the_case.charges", new FetchConfig().query())
            .fetch("charges.the_case.charges.person", new FetchConfig().query())
            .fetch("charges.the_case.charges.rule", new FetchConfig().query())
            .fetch("charges.the_case.charges.rule.section", new FetchConfig().query())
            .fetch("charges.the_case.charges.rule.section.chapter", new FetchConfig().query())
            .where().eq("section.chapter.organization", org)
            .eq("id", id).findOne();
    }

    public static Entry findBreakingResPlanEntry(Organization org) {
        return find.query().where()
            .eq("section.chapter.organization", org)
            .eq("is_breaking_res_plan", true)
            .findOne();
    }

    public static Integer findBreakingResPlanEntryId(Organization org) {
        Entry entry = findBreakingResPlanEntry(org);
        return entry != null ? entry.id : null;
    }

    public static void unassignBreakingResPlanEntry(Organization org) {
        Entry entry = findBreakingResPlanEntry(org);
        if (entry != null) {
            entry.is_breaking_res_plan = false;
            entry.save();
        }
    }

    @JsonIgnore
    public List<Charge> getThisYearCharges() {
        Date beginning_of_year = Application.getStartOfYear();

        List<Charge> result = new ArrayList<>();
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

        title = form.field("title").value().get();
        content = form.field("content").value().get();
        num = form.field("num").value().get();
        String deleted_val = form.field("deleted").value().get();

        boolean previous_deleted = deleted;
        deleted = deleted_val != null && deleted_val.equals("true");

        section = Section.find.byId(Integer.parseInt(form.field("section.id").value().get()));

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

    public static Comparator<Entry> SORT_NUMBER = Comparator.comparing(Entry::getNumber);
}
