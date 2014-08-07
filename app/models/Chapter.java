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
public class Chapter extends Model {
    @Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chapter_id_seq")
    public Integer id;

    @NotNull
    public String title = "";
    @NotNull
	public Integer num = 0;

    @OneToMany(mappedBy="chapter")
    @OrderBy("num ASC")
    public List<Section> sections;

    public static Finder<Integer,Chapter> find = new Finder(
        Integer.class, Chapter.class
    );
	
	public void updateFromForm(Form<Chapter> form) {
		title = form.field("title").value();
		num = Integer.parseInt(form.field("num").value());
		save();
	}
	
	public static Chapter create(Form<Chapter> form) {
		Chapter result = form.get();
		result.updateFromForm(form);
		result.save();
		return result;
	}
}
