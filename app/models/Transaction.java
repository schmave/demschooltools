package models;

import java.util.*;
import java.math.*;
import javax.persistence.*;
import com.fasterxml.jackson.annotation.*;
import play.data.*;
import com.avaje.ebean.*;

@Entity
@Table(name="transactions")
public class Transaction extends Model {

    @Id
    public Integer id;

    @ManyToOne()
    public Organization organization;

    @ManyToOne()
    @JoinColumn(name = "from_account_id", referencedColumnName = "id")
    public Account from_account;

    @ManyToOne()
    @JoinColumn(name = "to_account_id", referencedColumnName = "id")
    public Account to_account;

    public String from_name = "";
    public String to_name = "";
    public String description = "";

    public BigDecimal amount;

    public Date date_created = new Date();

    public TransactionType type;

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
