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

    @ManyToOne()
    public Institution institution;

    @OneToMany(mappedBy = "from_account")
    @JsonIgnore
    public List<Transaction> payment_transactions;

    @OneToMany(mappedBy = "to_account")
    @JsonIgnore
    public List<Transaction> credit_transactions;

    public AccountType type;

    public String name = "";

    public BigDecimal initial_balance = new BigDecimal(0);

    // TODO account should have its own name field, with this value by default but overridable
    public String getName() {
        if (person != null) {
            return person.getDisplayName();
        }
        else if (institution != null) {
            return institution.name;
        }
        else throw new Exception("account is not associated with either a person or an institution");
    }

    public static Finder<Integer, Account> find = new Finder<Integer, Account>(
        Account.class
    );

    public static Account findById(Integer id) {
        return find.where()
            .eq("organization", Organization.getByHost())
            .eq("id", id)
            .findUnique();
    }

    // TODO do we create accounts here if there exist people with no accounts?
    public static List<Account> findByType(AccountType type) {
        return find.where()
            .eq("organization", Organization.getByHost())
            .eq("type", type)
            .findList();
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
