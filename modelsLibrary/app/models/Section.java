package models;

import io.ebean.*;
import io.ebean.annotation.Where;
import com.fasterxml.jackson.annotation.JsonIgnore;
import play.data.Form;

import javax.persistence.*;
import javax.persistence.OrderBy;
import java.util.List;

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
			Section.class
	);

    public static Section findById(int id, Organization org) {
        return find.query().where().eq("chapter.organization", org)
            .eq("id", id).findOne();
    }

    public String getNumber() {
        return chapter.num + num;
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
