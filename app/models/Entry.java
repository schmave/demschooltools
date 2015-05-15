package models;

import java.util.*;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;
import com.avaje.ebean.validation.NotNull;

import controllers.*;

import play.data.*;
import play.data.validation.Constraints.*;
import play.data.validation.ValidationError;
import play.db.ebean.*;
import static play.libs.F.*;

@Entity
public class Entry extends Model implements Comparable<Entry> {
    @Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "entry_id_seq")
    public Integer id;

    @NotNull
    public String title = "";
    @NotNull
	public String num = "";

    @Column(columnDefinition = "TEXT")
    @NotNull
    public String content = "";

    @ManyToOne()
    public Section section;

	@NotNull
	public boolean deleted;

	@OneToMany(mappedBy="rule")
    @JsonIgnore
    @OrderBy("id DESC")
    public List<Charge> charges;

    public static Finder<Integer,Entry> find = new Finder(
        Integer.class, Entry.class
    );

    public static Entry findById(int id) {
        return find.where().eq("section.chapter.organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }

    @JsonIgnore
    public List<ManualChange> changes() {
        return ManualChange.find.where()
            .eq("entry", this)
            .orderBy("date_entered ASC").findList();
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
