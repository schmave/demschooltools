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
public class Entry extends Model {
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
	public Boolean deleted;

    public static Finder<Integer,Entry> find = new Finder(
        Integer.class, Entry.class
    );

	public void updateFromForm(Form<Entry> form) {
		title = form.field("title").value();
		content = form.field("content").value();
		num = form.field("num").value();
		String deleted_val = form.field("deleted").value();
		deleted = deleted_val != null && deleted_val.equals("true");
		section = Section.find.byId(Integer.parseInt(form.field("section.id").value()));
		save();
	}
	
	public static Entry create(Form<Entry> form) {
		Entry result = form.get();
		result.updateFromForm(form);
		result.save();
		return result;
	}
}
