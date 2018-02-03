package models;

import controllers.Application;
import java.text.*;
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

    @ManyToOne()
    @JoinColumn(name = "created_by_user_id", referencedColumnName = "id")
    public User created_by_user;

    public String from_name = "";
    public String to_name = "";
    public String description = "";

    public BigDecimal amount;

    public Date date_created = new Date();

    public TransactionType type;

    public String getTypeName() {
        return type.toString();
    }

    public String getCreatedByUserName() {
        return created_by_user == null ? "guest" : created_by_user.name;
    }    

    public String getFormattedDate() {
        return new SimpleDateFormat("M/d/yy h:mm aa").format(date_created);
    }

    public String getFormattedAmount() {
        if (amount == BigDecimal.ZERO) return "0";
        String prefix = amount.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
        return prefix + new DecimalFormat("0.00").format(amount);
    }

    public static Finder<Integer, Transaction> find = new Finder<Integer, Transaction>(
        Transaction.class
    );

    public static Transaction findById(Integer id) {
        return find.where().eq("organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }

    public static List<Transaction> allCashDeposits() {
        return find.where()
            .eq("organization", Organization.getByHost())
            .eq("type", TransactionType.CashDeposit)
            .findList();
    }

    public static List<Transaction> allCashWithdrawals() {
        return find.where()
            .eq("organization", Organization.getByHost())
            .eq("type", TransactionType.CashWithdrawal)
            .findList();
    }

    public static Transaction create(Form<Transaction> form) throws Exception {
        Transaction transaction = form.get();
        transaction.from_account = findAccountById(form.field("from_account_id").value());
        transaction.to_account = findAccountById(form.field("to_account_id").value());
        if (transaction.type == TransactionType.CashWithdrawal) {
            // for cash withdrawals from personal accounts, the to_account is automatically the person's cash account
            if (transaction.from_account.person != null) {
                List<Account> personAccounts = transaction.from_account.person.accounts;
                transaction.to_account = personAccounts.stream().filter(a -> a.type == AccountType.Cash).findFirst().get();
            }
        }
        if (transaction.type == TransactionType.DigitalTransaction 
            && transaction.from_account == null && transaction.to_account == null) {
            throw new Exception("A Digital Transaction must be either from an account or to an account.");
        }
        transaction.organization = Organization.getByHost();
        transaction.created_by_user = Application.getCurrentUser();
        transaction.save();
        return transaction;
    }

    private static Account findAccountById(String id) {
        if (id == null || id.trim().length() == 0) return null;
        return Account.findById(Integer.valueOf(id));
    }
}