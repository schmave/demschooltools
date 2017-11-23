package models;

import java.util.*;
import java.math.*;
import javax.persistence.*;
import com.fasterxml.jackson.annotation.*;
import play.data.*;
import com.avaje.ebean.*;

@Entity
public class Account extends Model {

    @Id
    public Integer id;

    @ManyToOne()
    public Organization organization;

    @ManyToOne()
    public Person person;

    @ManyToOne()
    public Institution institution;

    @OneToMany(mappedBy = "from_account")
    @JsonIgnore
    public List<Transaction> payment_transactions;

    @OneToMany(mappedBy = "to_account")
    @JsonIgnore
    public List<Transaction> credit_transactions;

    public AccountType type;

    public BigDecimal initial_balance;

    public static Finder<Integer, Account> find = new Finder<Integer, Account>(
        Account.class
    );

    public static Account findById(Integer id) {
        return find.where().eq("organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }

    public static Account create(Person person, Institution institution, AccountType type) {
        Account account = new Account();
        account.person = person;
        account.institution = institution;
        account.type = type;
        account.organization = Organization.getByHost();
        account.save();
        return account;
    }
}
