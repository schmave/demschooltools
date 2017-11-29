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
    @JoinColumn(name="person_id")
    public Person person;

    @OneToMany(mappedBy = "from_account")
    @JsonIgnore
    public List<Transaction> payment_transactions;

    @OneToMany(mappedBy = "to_account")
    @JsonIgnore
    public List<Transaction> credit_transactions;

    public AccountType type;

    public String name = "";

    public BigDecimal initial_balance = new BigDecimal(0);

    public String getName() {
        if (name != null && name.trim().length() > 0) {
            return name;
        }
        if (person != null) {
            return person.getDisplayName();
        }
        return name;
    }

    public String getTypeName() {
        return type.toString();
    }

    public boolean isCash() {
        return type == AccountType.Cash;
    }

    private static Finder<Integer, Account> find = new Finder<Integer, Account>(Account.class);

    public static List<Account> all() {
        return find.where()
            .eq("organization", Organization.getByHost())
            .findList();
    }

    public static List<Account> allCash() {
        return find.where()
            .eq("organization", Organization.getByHost())
            .eq("type", AccountType.Cash)
            .findList();
    }

    public static List<Account> allDigital() {
        return find.where()
            .eq("organization", Organization.getByHost())
            .ne("type", AccountType.Cash)
            .findList();
    }

    public static Account findById(Integer id) {
        return find.where()
            .eq("organization", Organization.getByHost())
            .eq("id", id)
            .findUnique();
    }

    public static Account create(AccountType type, String name, Person person) {
        Account account = new Account();
        account.person = person;
        account.name = name;
        account.type = type;
        account.organization = Organization.getByHost();
        account.save();
        return account;
    }

    public static Account createFromForm(Form<Account> form) {
        Account account = form.get();
        account.organization = Organization.getByHost();
        account.save();
        return account;
    }

}
