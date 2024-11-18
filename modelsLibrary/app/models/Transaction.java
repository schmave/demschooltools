package models;

import io.ebean.*;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import play.data.Form;

@Getter
@Setter
@Entity
@Table(name = "transactions")
public class Transaction extends Model {

  @Id private Integer id;

  @ManyToOne() private Organization organization;

  @ManyToOne()
  @JoinColumn(name = "from_account_id", referencedColumnName = "id")
  private Account fromAccount;

  @ManyToOne()
  @JoinColumn(name = "to_account_id", referencedColumnName = "id")
  private Account toAccount;

  @ManyToOne()
  @JoinColumn(name = "created_by_user_id", referencedColumnName = "id")
  private User createdByUser;

  private String fromName = "";
  private String toName = "";
  private String description = "";

  private BigDecimal amount;

  @play.data.format.Formats.DateTime(pattern = "MM/dd/yyyy")
  private Date dateCreated = new Date();

  private TransactionType type;

  private Boolean archived = false;

  public String getTypeName() {
    return type.toString();
  }

  public String getCreatedByUserName() {
    return createdByUser == null ? "guest" : createdByUser.getName();
  }

  public String getFormattedDate() {
    SimpleDateFormat sdf = new SimpleDateFormat("M/d/yy");
    // we don't need to worry about time zones because the transaction date is set in js on the
    // client.
    // sdf.setTimeZone(TimeZone.getTimeZone("EST"));
    return sdf.format(dateCreated);
  }

  public String getFormattedDate2() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    return sdf.format(dateCreated);
  }

  public String getFormattedAmount(boolean includePlusSign) {
    if (Objects.equals(amount, BigDecimal.ZERO)) return "0";
    String formatted = new DecimalFormat("0.00").format(amount);
    if (includePlusSign) {
      String prefix = amount.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
      return prefix + formatted;
    }
    return formatted;
  }

  private Boolean isPersonal() {
    return (toAccount != null && toAccount.getType() == AccountType.PersonalChecking)
        || (fromAccount != null && fromAccount.getType() == AccountType.PersonalChecking);
  }

  public String getCssClass() {
    String cssClass = "js-archivable";
    if (archived) {
      cssClass += " js-archived accounting-archived";
    }
    return cssClass;
  }

  public static Finder<Integer, Transaction> find = new Finder<>(Transaction.class);

  public static Transaction findById(Integer id, Organization org) {
    return find.query().where().eq("organization", org).eq("id", id).findOne();
  }

  public static List<Transaction> allWithConditions(
      Boolean include_personal,
      Boolean include_non_personal,
      Boolean include_cash_deposits,
      Boolean include_cash_withdrawals,
      Boolean include_digital,
      Boolean include_archived,
      Date start_date,
      Date end_date,
      Organization org) {

    return find
        .query()
        .fetch("toAccount", FetchConfig.ofQuery())
        .fetch("fromAccount", FetchConfig.ofQuery())
        .where()
        .eq("organization", org)
        .findList()
        .stream()
        .filter(
            t ->
                (include_personal || !t.isPersonal())
                    && (include_non_personal || t.isPersonal())
                    && (include_cash_deposits || t.type != TransactionType.CashDeposit)
                    && (include_cash_withdrawals || t.type != TransactionType.CashWithdrawal)
                    && (include_digital || t.type != TransactionType.DigitalTransaction)
                    && (include_archived || !t.archived)
                    && (start_date == null || t.dateCreated.compareTo(start_date) >= 0)
                    && (end_date == null || t.dateCreated.compareTo(end_date) <= 0))
        .collect(Collectors.toList());
  }

  public static List<Transaction> allCashDeposits(Organization org) {
    return find.query()
        .where()
        .eq("organization", org)
        .eq("type", TransactionType.CashDeposit)
        .findList();
  }

  public static List<Transaction> allCashWithdrawals(Organization org) {
    return find.query()
        .where()
        .eq("organization", org)
        .eq("type", TransactionType.CashWithdrawal)
        .findList();
  }

  public static Transaction create(Form<Transaction> form, Organization org, User current_user)
      throws Exception {
    Transaction transaction = form.get();

    //        transaction.type = TransactionType.valueOf(form.field("type").value().get());
    transaction.fromAccount = findAccountById(form.field("from_account_id").value().get(), org);
    transaction.toAccount = findAccountById(form.field("to_account_id").value().get(), org);

    DateFormat format = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH);
    transaction.dateCreated = format.parse(form.field("dateCreated").value().get());

    transaction.organization = org;
    transaction.createdByUser = current_user;

    transaction.save();
    return transaction;
  }

  public static Transaction createMonthlyCreditTransaction(
      Account account, Date date, Organization org) {
    Transaction transaction = new Transaction();
    transaction.toAccount = account;
    transaction.amount = account.getMonthlyCredit();
    transaction.type = TransactionType.DigitalTransaction;
    transaction.description = new SimpleDateFormat("MMMM").format(date) + " monthly credit";
    transaction.dateCreated = date;
    transaction.organization = org;
    transaction.save();
    return transaction;
  }

  private static Account findAccountById(String id, Organization org) {
    if (id == null || id.trim().length() == 0) return null;
    return Account.findById(Integer.valueOf(id), org);
  }

  public static void delete(Integer id) {
    Transaction transaction = find.ref(id);
    transaction.delete();
  }

  public void updateFromForm(Form<Transaction> form) throws Exception {
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    description = form.field("description").value().get();
    amount = new BigDecimal(form.field("amount").value().get());
    dateCreated = format.parse(form.field("dateCreated").value().get());
    save();
  }
}
