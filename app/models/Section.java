package models;

import java.util.*;

import javax.persistence.*;

import com.avaje.ebean.annotation.Where;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;

import controllers.*;

import com.fasterxml.jackson.annotation.*;

import play.data.*;
import play.data.validation.Constraints.*;
import play.data.validation.ValidationError;
import com.avaje.ebean.Model;
import static play.libs.F.*;

@Entity
public class Section extends Model {
    @Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "section_id_seq")
    public Integer id;

    public String title = "";
	public String num = "";

    @OneToMany(mappedBy="section")
    @OrderBy("num ASC")
	@Where(clause = "${ta}.deleted = false")
    @JsonIgnore
    public List<Entry> entries;

    @ManyToOne()
    public Chapter chapter;

	public Boolean deleted;

    public static Finder<Integer,Section> find = new Finder<>(
        Integer.class, Section.class
    );

    public static Section findById(int id) {
        return find.where().eq("chapter.organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }

    public String getNumber() {
        return chapter.num + num;
    }

	public void updateFromForm(Form<Section> form) {
		title = form.field("title").value();
		num = form.field("num").value();
		chapter = Chapter.find.byId(Integer.parseInt(form.field("chapter.id").value()));
		String deleted_val = form.field("deleted").value();
		deleted = deleted_val != null && deleted_val.equals("true");
		save();
	}

	public static Section create(Form<Section> form) {
		Section result = form.get();
		result.updateFromForm(form);
		result.save();
		return result;
	}
}
