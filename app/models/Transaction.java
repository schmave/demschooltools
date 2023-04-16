package models;

import io.ebean.*;
import play.data.Form;

import javax.persistence.*;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

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

    @ManyToOne()
    @JoinColumn(name = "created_by_user_id", referencedColumnName = "id")
    public User created_by_user;

    public String from_name = "";
    public String to_name = "";
    public String description = "";

    public BigDecimal amount;

    @play.data.format.Formats.DateTime(pattern="MM/dd/yyyy")
    public Date date_created = new Date();

    public TransactionType type;

    public Boolean archived = false;

    public String getTypeName() {
        return type.toString();
    }

    public String getCreatedByUserName() {
        return created_by_user == null ? "guest" : created_by_user.name;
    }    

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("M/d/yy");
        // we don't need to worry about time zones because the transaction date is set in js on the client.
        //sdf.setTimeZone(TimeZone.getTimeZone("EST"));
        return sdf.format(date_created);
    }

    public String getFormattedDate2() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date_created);
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
        return (to_account != null && to_account.type == AccountType.PersonalChecking)
            || (from_account != null && from_account.type == AccountType.PersonalChecking);
    }

    public String getCssClass() {
        String cssClass = "js-archivable";
        if (archived) {
            cssClass += " js-archived accounting-archived";
        }
        return cssClass;
    }

    public static Finder<Integer, Transaction> find = new Finder<>(
            Transaction.class
    );

    public static Transaction findById(Integer id, Organization org) {
        return find.query().where().eq("organization", org)
            .eq("id", id).findOne();
    }

    public static List<Transaction> allWithConditions(
            Boolean include_personal,
            Boolean include_non_personal,
            Boolean include_cash_deposits,
            Boolean include_cash_withdrawals,
            Boolean include_digital,
            Boolean include_archived, Organization org) {
        
        return find.query()
            .fetch("to_account", new FetchConfig().query())
            .fetch("from_account", new FetchConfig().query())
            .where()
            .eq("organization", org)
            .findList()
            .stream()
            .filter(t -> 
                (include_personal || !t.isPersonal()) &&
                (include_non_personal || t.isPersonal()) &&
                (include_cash_deposits || t.type != TransactionType.CashDeposit) &&
                (include_cash_withdrawals || t.type != TransactionType.CashWithdrawal) &&
                (include_digital || t.type != TransactionType.DigitalTransaction) &&
                (include_archived || !t.archived))
            .collect(Collectors.toList());
    }

    public static List<Transaction> allCashDeposits(Organization org) {
        return find.query().where()
            .eq("organization", org)
            .eq("type", TransactionType.CashDeposit)
            .findList();
    }

    public static List<Transaction> allCashWithdrawals(Organization org) {
        return find.query().where()
            .eq("organization", org)
            .eq("type", TransactionType.CashWithdrawal)
            .findList();
    }

    public static Transaction create(Form<Transaction> form,
                                     Organization org, User current_user) throws Exception {
        Transaction transaction = form.get();

        transaction.from_account = findAccountById(form.field("from_account_id").value().get());
        transaction.to_account = findAccountById(form.field("to_account_id").value().get());

        DateFormat format = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH);
        transaction.date_created = format.parse(form.field("date_created").value().get());

        transaction.organization = org;
        transaction.created_by_user = current_user;

        transaction.save();
        return transaction;
    }

    public static Transaction createMonthlyCreditTransaction(Account account, Date date, Organization org) {
        Transaction transaction = new Transaction();
        transaction.to_account = account;
        transaction.amount = account.monthly_credit;
        transaction.type = TransactionType.DigitalTransaction;
        transaction.description = new SimpleDateFormat("MMMM").format(date) + " monthly credit";
        transaction.date_created = date;
        transaction.organization = org;
        transaction.save();
        return transaction;
    }

    private static Account findAccountById(String id) {
        if (id == null || id.trim().length() == 0) return null;
        return Account.findById(Integer.valueOf(id));
    }

    public static void delete(Integer id) {
        Transaction transaction = find.ref(id);
        transaction.delete();
    }

    public void updateFromForm(Form<Transaction> form) throws Exception {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        description = form.field("description").value().get();
        amount = new BigDecimal(form.field("amount").value().get());
        date_created = format.parse(form.field("date_created").value().get());
        save();
    }
}
