package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.*;

import org.codehaus.jackson.annotate.*;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;

@Entity
public class Email extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "email_id_seq")
    public Integer id;

    public String message;
	public boolean sent;
	public boolean deleted;

    public static Finder<Integer, Email> find = new Finder(
        Integer.class, Email.class
    );
	
	public static Email create(String message) {
		Email e = new Email();
		e.message = message;
		return e;
	}
}
