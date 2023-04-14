package models;

import io.ebean.*;
import io.ebean.annotation.Where;
import com.fasterxml.jackson.annotation.JsonIgnore;
import controllers.Utils;
import play.data.Form;

import javax.persistence.*;
import javax.persistence.OrderBy;
import java.util.List;

@Entity
public class Chapter extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chapter_id_seq")
    public Integer id;

    public String title = "";
    public String num = "";

    @OneToMany(mappedBy="chapter")
    @OrderBy("num ASC")
    @Where(clause = "${ta}.deleted = false")
    @JsonIgnore
    public List<Section> sections;

    @ManyToOne()
    public Organization organization;

    public Boolean deleted;

    public static Finder<Integer,Chapter> find = new Finder<>(
            Chapter.class
    );

    public static Chapter findById(int id) {
        return find.query().where().eq("organization", Organization.getByHost())
            .eq("id", id).findOne();
    }

    public static List<Chapter> all() {
        return find.query().where()
            .eq("deleted", Boolean.FALSE)
            .eq("organization", Organization.getByHost())
            .orderBy("num ASC").findList();
    }

    public void updateFromForm(Form<Chapter> form) {
        title = form.field("title").value().get();
        num = form.field("num").value().get();
        deleted = Utils.getBooleanFromFormValue(form.field("deleted"));
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
