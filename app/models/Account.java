package models;

import java.text.*;
import java.util.*;
import java.math.*;
import javax.persistence.*;

import com.fasterxml.jackson.annotation.*;
import io.ebean.Query;
import play.data.*;
import io.ebean.*;

import controllers.Utils;

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

    public BigDecimal monthly_credit = new BigDecimal(0);

    public boolean is_active;

    @play.data.format.Formats.DateTime(pattern="MM/dd/yyyy")
    public Date date_last_monthly_credit;

    public static void createPersonalAccounts() {
        for (Person person : allPeople()) {
            if (!person.hasAccount(AccountType.PersonalChecking)) {
                create(AccountType.PersonalChecking, "", person);
            }
        }
    }

    public Integer getId() {
        return id;
    }

    public AccountType getType() {
        return type;
    }

    public String getName() {
        if (name != null && name.trim().length() > 0) {
            return name;
        }
        if (person != null) {
            return person.getDisplayName();
        }
        return "<no name>";
    }

    public String getTitle() {
        String typeName = type == AccountType.PersonalChecking ? getTypeName() + " " : "";
        return getName() + "'s " + typeName + "Account";
    }

    public String getTypeName() {
        return type.toString();
    }

    public boolean hasTransactions() {
        return payment_transactions.size() > 0 || credit_transactions.size() > 0;
    }

    public BigDecimal getBalance() {
        return initial_balance
            .add(credit_transactions.stream().map(t -> t.amount).reduce(BigDecimal.ZERO, BigDecimal::add))
            .subtract(payment_transactions.stream().map(t -> t.amount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal getBalanceAsOf(Date date) {
        return initial_balance
            .add(credit_transactions.stream()
                .filter(t -> !t.date_created.after(date))
                .map(t -> t.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
            .subtract(payment_transactions.stream()
                .filter(t -> !t.date_created.after(date))
                .map(t -> t.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public String getFormattedBalance() {
        return new DecimalFormat("0.00").format(getBalance());
    }

    public String getFormattedInitialBalance() {
        return new DecimalFormat("0.00").format(initial_balance);
    }

    public String getFormattedMonthlyCredit() {
        return new DecimalFormat("0.00").format(monthly_credit);
    }

    public List<Transaction> getTransactionsViewModel() {
        List<Transaction> result = new ArrayList<>();
        for (Transaction t : credit_transactions) {
            t.description = getFormattedDescription("from", t.from_name, t.description);
            result.add(t);
        }
        for (Transaction t : payment_transactions) {
            t.description = getFormattedDescription("to", t.to_name, t.description);
            t.amount = BigDecimal.ZERO.subtract(t.amount);
            result.add(t);
        }
        TransactionList.sortTransactions(result);
        return result;
    }

    private String getFormattedDescription(String toFromPrefix, String toFromName, String description) {
        if (toFromName.trim().isEmpty()) return description;
        String toFrom = toFromPrefix + " " + toFromName;
        if (description.trim().isEmpty()) return toFrom;
        return toFrom + " – " + description;
    }

    private static final Finder<Integer, Account> find = new Finder<>(Account.class);

    public static List<Account> all() {
        return baseQuery().where()
                .eq("organization", Organization.getByHost())
                .findList();
    }

    public static List<Account> allPersonalChecking() {
        return baseQuery().where()
            .in("person", allPeople())
            .eq("type", AccountType.PersonalChecking)
            .findList();
    }

    public static List<Account> allNonPersonalChecking() {
        return baseQuery().where()
            .eq("organization", Organization.getByHost())
            .eq("is_active", true)
            .ne("type", AccountType.Cash)
            .ne("type", AccountType.PersonalChecking)
            .findList();
    }

    public static List<Account> allWithMonthlyCredits() {
        return baseQuery().where()
                .eq("organization", Organization.getByHost())
                .eq("is_active", true)
                .ne("monthly_credit", BigDecimal.ZERO)
                .findList();
    }

    private static Query<Account> baseQuery() {
        return find.query()
            .fetch("person", new FetchConfig().query())
            .fetch("payment_transactions", new FetchConfig().query())
            .fetch("credit_transactions", new FetchConfig().query());
    }

    private static List<Person> allPeople() {
        List<Tag> tags = Tag.find.query().where()
            .eq("show_in_account_balances", true)
            .eq("organization", Organization.getByHost())
            .findList();

        Set<Person> people = new HashSet<>();
        for (Tag tag : tags) {
            people.addAll(tag.people);
        }
        return new ArrayList<>(people);
    }

    public static Account findById(Integer id) {
        return find.query().where()
            .eq("organization", Organization.getByHost())
            .eq("id", id)
            .findOne();
    }

    public static Account create(AccountType type, String name, Person person) {
        Account account = new Account();
        account.person = person;
        account.name = name;
        account.type = type;
        account.is_active = true;
        account.organization = Organization.getByHost();
        account.save();
        return account;
    }

    public void updateFromForm(Form<Account> form) {
        is_active = Utils.getBooleanFromFormValue(form.field("is_active"));
        name = form.field("name").value().get();
        type = AccountType.valueOf(form.field("type").value().get());
        monthly_credit = new BigDecimal(form.field("monthly_credit").value().get());
        // if we are changing the monthly credit, set the date last applied to today
        if (monthly_credit.compareTo(BigDecimal.ZERO) != 0) {
            date_last_monthly_credit = new Date();
        }
        save();
    }

    public void createMonthlyCreditTransaction(Date date) {
        Transaction.createMonthlyCreditTransaction(this, date);
        date_last_monthly_credit = date;
        save();
    }

    public static void delete(Integer id) throws Exception {
        Account account = find.ref(id);
        if (account.hasTransactions()) {
            throw new Exception("Can't delete an account that has transactions");
        }
        account.delete();
    }
}
