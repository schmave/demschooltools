package models;

import java.util.*;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;
import com.avaje.ebean.annotation.Where;
import com.avaje.ebean.validation.NotNull;

import controllers.*;

import play.data.*;
import play.data.validation.Constraints.*;
import play.data.validation.ValidationError;
import com.avaje.ebean.Model;
import static play.libs.F.*;

@Entity
public class Chapter extends Model {
    @Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chapter_id_seq")
    public Integer id;

    @NotNull
    public String title = "";
    @NotNull
	public String num = "";

    @OneToMany(mappedBy="chapter")
    @JsonIgnore
    private List<Section> sections;

    @ManyToOne()
    public Organization organization;

    @NotNull
	public Boolean deleted;

    public static Finder<Integer,Chapter> find = new Finder<>(
        Integer.class, Chapter.class
    );

    public static Chapter findById(int id) {
        return find.where().eq("organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }

    public static List<Chapter> all() {
        return find.where()
            .eq("deleted", Boolean.FALSE)
            .eq("organization", Organization.getByHost())
            .orderBy("num ASC").findList();
    }

    public List<Section> sections() {
        return Section.find.where()
            .eq("chapter", this)
            .eq("deleted", Boolean.FALSE)
            .orderBy("num ASC").findList();
    }

	public void updateFromForm(Form<Chapter> form) {
		title = form.field("title").value();
		num = form.field("num").value();
		String deleted_val = form.field("deleted").value();
		deleted = deleted_val != null && deleted_val.equals("true");
		save();
	}

	public static Chapter create(Form<Chapter> form) {
		Chapter result = form.get();
        result.organization = Organization.getByHost();
		result.updateFromForm(form);
		result.save();
		return result;
	}
}
