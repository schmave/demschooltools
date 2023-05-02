package models;

import com.fasterxml.jackson.annotation.*;
import io.ebean.*;
import io.ebean.Query;
import java.math.*;
import java.text.*;
import java.util.*;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import play.data.*;

@Getter
@Setter
@Entity
public class Account extends Model {

  @Id private Integer id;

  @ManyToOne() private Organization organization;

  @ManyToOne()
  @JoinColumn(name = "person_id")
  private Person person;

  @OneToMany(mappedBy = "fromAccount")
  @JsonIgnore
  public List<Transaction> payment_transactions;

  @OneToMany(mappedBy = "toAccount")
  @JsonIgnore
  public List<Transaction> credit_transactions;

  private AccountType type;

  private String name = "";

  private BigDecimal initialBalance = new BigDecimal(0);

  private BigDecimal monthlyCredit = new BigDecimal(0);

  private boolean isActive;

  @play.data.format.Formats.DateTime(pattern = "MM/dd/yyyy")
  private Date dateLastMonthlyCredit;

  public static void createPersonalAccounts(Organization org) {
    for (Person person : allPeople(org)) {
      if (!person.hasAccount(AccountType.PersonalChecking)) {
        create(AccountType.PersonalChecking, "", person, org);
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

  public String getTitle(OrgConfig orgConfig) {
    String typeName = type == AccountType.PersonalChecking ? getTypeName(orgConfig) + " " : "";
    return getName() + "'s " + typeName + "Account";
  }

  public String getTypeName(OrgConfig orgConfig) {
    return type.toString(orgConfig);
  }

  public boolean hasTransactions() {
    return payment_transactions.size() > 0 || credit_transactions.size() > 0;
  }

  public BigDecimal getBalance() {
    return initialBalance
        .add(
            credit_transactions.stream()
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add))
        .subtract(
            payment_transactions.stream()
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
  }

  public BigDecimal getBalanceAsOf(Date date) {
    return initialBalance
        .add(
            credit_transactions.stream()
                .filter(t -> !t.getDateCreated().after(date))
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add))
        .subtract(
            payment_transactions.stream()
                .filter(t -> !t.getDateCreated().after(date))
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
  }

  public String getFormattedBalance() {
    return new DecimalFormat("0.00").format(getBalance());
  }

  public String getFormattedInitialBalance() {
    return new DecimalFormat("0.00").format(initialBalance);
  }

  public String getFormattedMonthlyCredit() {
    return new DecimalFormat("0.00").format(monthlyCredit);
  }

  public String getFormattedDescription(
      String toFromPrefix, String toFromName, String description) {
    if (toFromName.trim().isEmpty()) return description;
    String toFrom = toFromPrefix + " " + toFromName;
    if (description.trim().isEmpty()) return toFrom;
    return toFrom + " – " + description;
  }

  private static final Finder<Integer, Account> find = new Finder<>(Account.class);

  public static List<Account> all(Organization org) {
    return baseQuery().where().eq("organization", org).findList();
  }

  public static List<Account> allPersonalChecking(Organization org) {
    return baseQuery()
        .where()
        .in("person", allPeople(org))
        .eq("type", AccountType.PersonalChecking)
        .findList();
  }

  public static List<Account> allNonPersonalChecking(Organization org) {
    return baseQuery()
        .where()
        .eq("organization", org)
        .eq("isActive", true)
        .ne("type", AccountType.Cash)
        .ne("type", AccountType.PersonalChecking)
        .findList();
  }

  public static List<Account> allWithMonthlyCredits(Organization org) {
    return baseQuery()
        .where()
        .eq("organization", org)
        .eq("isActive", true)
        .ne("monthlyCredit", BigDecimal.ZERO)
        .findList();
  }

  private static Query<Account> baseQuery() {
    return find.query()
        .fetch("person", FetchConfig.ofQuery())
        .fetch("payment_transactions", FetchConfig.ofQuery())
        .fetch("credit_transactions", FetchConfig.ofQuery());
  }

  private static List<Person> allPeople(Organization org) {
    List<Tag> tags =
        Tag.find
            .query()
            .where()
            .eq("showInAccountBalances", true)
            .eq("organization", org)
            .findList();

    Set<Person> people = new HashSet<>();
    for (Tag tag : tags) {
      people.addAll(tag.people);
    }
    return new ArrayList<>(people);
  }

  public static Account findById(Integer id, Organization org) {
    return find.query().where().eq("organization", org).eq("id", id).findOne();
  }

  public static Account create(AccountType type, String name, Person person, Organization org) {
    Account account = new Account();
    account.person = person;
    account.name = name;
    account.type = type;
    account.isActive = true;
    account.organization = org;
    account.save();
    return account;
  }

  public void updateFromForm(Form<Account> form) {
    isActive = ModelUtils.getBooleanFromFormValue(form.field("isActive"));
    name = form.field("name").value().get();
    type = AccountType.valueOf(form.field("type").value().get());
    monthlyCredit = new BigDecimal(form.field("monthlyCredit").value().get());
    // if we are changing the monthly credit, set the date last applied to today
    if (monthlyCredit.compareTo(BigDecimal.ZERO) != 0) {
      dateLastMonthlyCredit = new Date();
    }
    save();
  }

  public void createMonthlyCreditTransaction(Date date, Organization org) {
    Transaction.createMonthlyCreditTransaction(this, date, org);
    dateLastMonthlyCredit = date;
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
