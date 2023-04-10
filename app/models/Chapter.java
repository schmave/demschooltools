package models;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.Where;
import com.fasterxml.jackson.annotation.JsonIgnore;
import controllers.Utils;
import play.data.Form;

import javax.persistence.*;
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
        return find.where().eq("organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }

    public static List<Chapter> all() {
        return find.where()
            .eq("deleted", Boolean.FALSE)
            .eq("organization", Organization.getByHost())
            .orderBy("num ASC").findList();
    }

    public void updateFromForm(Form<Chapter> form) {
        title = form.field("title").getValue().get();
        num = form.field("num").getValue().get();
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
