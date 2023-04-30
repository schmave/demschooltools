package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.ebean.*;
import io.ebean.annotation.Where;
import java.util.List;
import javax.persistence.*;
import javax.persistence.OrderBy;
import lombok.Getter;
import lombok.Setter;
import play.data.Form;

@Getter
@Setter
@Entity
public class Chapter extends Model {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chapter_id_seq")
  private Integer id;

  private String title = "";
  private String num = "";

  @OneToMany(mappedBy = "chapter")
  @OrderBy("num ASC")
  @Where(clause = "${ta}.deleted = false")
  @JsonIgnore
  public List<Section> sections;

  @ManyToOne() private Organization organization;

  private Boolean deleted;

  public static Finder<Integer, Chapter> find = new Finder<>(Chapter.class);

  public static Chapter findById(int id, Organization org) {
    return find.query().where().eq("organization", org).eq("id", id).findOne();
  }

  public static List<Chapter> all(Organization org) {
    return find.query()
        .where()
        .eq("deleted", Boolean.FALSE)
        .eq("organization", org)
        .orderBy("num ASC")
        .findList();
  }

  public void updateFromForm(Form<Chapter> form) {
    title = form.field("title").value().get();
    num = form.field("num").value().get();
    deleted = ModelUtils.getBooleanFromFormValue(form.field("deleted"));
    save();
  }

  public static Chapter create(Form<Chapter> form, Organization org) {
    Chapter result = form.get();
    result.organization = org;
    result.updateFromForm(form);
    result.save();
    return result;
  }
}
