package models;

import java.util.*;
import javax.persistence.*;
import play.data.*;
import com.avaje.ebean.Model;

@Entity
public class Transaction extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_id_seq")
    public Integer id;

    @ManyToOne()
    public Organization organization;

    public static Finder<Integer, Transaction> find = new Finder<Integer, Transaction>(
        Transaction.class
    );

    public static Transaction findById(Integer id) {
        return find.where().eq("organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }

    public static Transaction create(Form<Transaction> form) {
        Transaction transaction = form.get();
        transaction.organization = Organization.getByHost();
        transaction.save();
        return transaction;
    }
}
