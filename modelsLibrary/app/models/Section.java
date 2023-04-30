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
public class Section extends Model {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "section_id_seq")
  private Integer id;

  private String title = "";
  private String num = "";

  @OneToMany(mappedBy = "section")
  @OrderBy("num ASC")
  @Where(clause = "${ta}.deleted = false")
  @JsonIgnore
  public List<Entry> entries;

  @ManyToOne() private Chapter chapter;

  private Boolean deleted;

  public static Finder<Integer, Section> find = new Finder<>(Section.class);

  public static Section findById(int id, Organization org) {
    return find.query().where().eq("chapter.organization", org).eq("id", id).findOne();
  }

  public String getNumber() {
    return chapter.getNum() + num;
  }

  public void updateFromForm(Form<Section> form) {
    title = form.field("title").value().get();
    num = form.field("num").value().get();
    chapter = Chapter.find.byId(Integer.parseInt(form.field("chapter.id").value().get()));
    deleted = ModelUtils.getBooleanFromFormValue(form.field("deleted"));
    save();
  }

  public static Section create(Form<Section> form) {
    Section result = form.get();
    result.updateFromForm(form);
    result.save();
    return result;
  }
}
