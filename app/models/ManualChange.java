package models;

import io.ebean.*;

import javax.persistence.*;
import java.util.Comparator;
import java.util.Date;

@Entity
public class ManualChange extends Model {
    @Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "manual_change_id_seq")
    public Integer id;

    @ManyToOne()
    public Chapter chapter;
    @ManyToOne()
    public Section section;
    @ManyToOne()
    public Entry entry;

    public Boolean was_deleted = false;
    public Boolean was_created = false;

    @Column(columnDefinition = "TEXT")
    public String old_content;
    @Column(columnDefinition = "TEXT")
    public String new_content;

    public String old_title;
    public String new_title;

    public String old_num;
    public String new_num;

    public Date date_entered = new Date();

    public static Finder<Integer,ManualChange> find = new Finder<>(
            ManualChange.class
    );

    void initCreate(String num, String title, String content) {
        was_created = true;
        new_num = num;
        new_title = title;
        new_content = content;
    }

    void initDelete(String num, String title, String content) {
        was_deleted = true;
        old_num = num;
        old_title = title;
        old_content = content;
    }

    boolean initChange(String new_num, String old_num, String new_title, String old_title,
        String new_content, String old_content) {

        //System.out.println("initChange " + new_num + " " + old_num + " T " + new_title + " " + old_title + " C " + new_content + " " + old_content);

        this.new_num = new_num;
        this.old_num = old_num;

        this.new_title = new_title;
        this.old_title = old_title;

        this.new_content = new_content;
        this.old_content = old_content;

        return (!this.new_num.equals(old_num) ||
            !this.new_title.equals(old_title) ||
            !this.new_content.equals(old_content));
    }

    public static ManualChange recordSectionCreate(Section s) {
		ManualChange result = new ManualChange();
        result.section = s;
        result.initCreate(s.getNumber(), s.title, null);
		result.save();
		return result;
	}

    public static ManualChange recordChapterCreate(Chapter c) {
        ManualChange result = new ManualChange();
        result.chapter = c;
        result.initCreate("" + c.num, c.title, null);
        result.save();
        return result;
    }

    public static ManualChange recordEntryCreate(Entry e) {
        ManualChange result = new ManualChange();
        result.entry = e;
        result.initCreate(e.getNumber(), e.title, e.content);
        result.save();
        return result;
    }

    public static ManualChange recordSectionDelete(Section s) {
        ManualChange result = new ManualChange();
        result.section = s;
        result.initDelete(s.getNumber(), s.title, null);
        result.save();
        return result;
    }

    public static ManualChange recordChapterDelete(Chapter c) {
        ManualChange result = new ManualChange();
        result.chapter = c;
        result.initDelete("" + c.num, c.title, null);
        result.save();
        return result;
    }

    public static ManualChange recordEntryDelete(Entry e) {
        ManualChange result = new ManualChange();
        result.entry = e;
        result.initDelete(e.getNumber(), e.title, e.content);
        result.save();
        return result;
    }

    public static ManualChange recordEntryChange(Entry e, String old_num, String old_title, String old_content) {
        ManualChange result = new ManualChange();
        result.entry = e;
        if (result.initChange(e.getNumber(), old_num, e.title, old_title, e.content, old_content)) {
            result.save();
            return result;
        }
        return null;
    }

    public static ManualChange recordSectionChange(Section s, String old_num, String old_title) {
        ManualChange result = new ManualChange();
        result.section = s;
        if (result.initChange(s.getNumber(), old_num, s.title, old_title, null, null)) {
            result.save();
            return result;
        }
        return null;
    }

    public static ManualChange recordChapterChange(Chapter c, String old_num, String old_title) {
        ManualChange result = new ManualChange();
        result.chapter = c;
        if (result.initChange("" + c.num, old_num, c.title, old_title, null, null)) {
            result.save();
            return result;
        }
        return null;
    }

    public static Comparator<ManualChange> SORT_NUM_DATE = (c1, c2) -> {
        // I want to order the change records by the user visible num.
        // However, when entries are created or deleted, the new_num
        // or old_num, respectively, is null. Use the new_num if it
        // is available, or old_num otherwise.
        String num1 = (c1.new_num == null ? c1.old_num : c1.new_num);
        String num2 = (c2.new_num == null ? c2.old_num : c2.new_num);

        if (num1.equals(num2)) {
            return c1.date_entered.compareTo(c2.date_entered);
        } else {
            return num1.compareTo(num2);
        }
    };
}
