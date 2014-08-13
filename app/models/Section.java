package models;

import java.util.*;

import javax.persistence.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;
import com.avaje.ebean.validation.NotNull;

import controllers.*;

import org.codehaus.jackson.annotate.*;

import play.data.*;
import play.data.validation.Constraints.*;
import play.data.validation.ValidationError;
import play.db.ebean.*;
import static play.libs.F.*;

@Entity
public class Section extends Model {
    @Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "section_id_seq")
    public Integer id;

    @NotNull
    public String title = "";
    @NotNull
	public String num = "";

    @OneToMany(mappedBy="section")
    @OrderBy("num ASC")
    public List<Entry> entries;

    @ManyToOne()
    public Chapter chapter;

    public static Finder<Integer,Section> find = new Finder(
        Integer.class, Section.class
    );

	public void updateFromForm(Form<Section> form) {
		title = form.field("title").value();
		num = form.field("num").value();
		chapter = Chapter.find.byId(Integer.parseInt(form.field("chapter.id").value()));
		save();
	}
	
	public static Section create(Form<Section> form) {
		Section result = form.get();
		result.updateFromForm(form);
		result.save();
		return result;
	}
}
