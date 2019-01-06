package models;

import java.text.*;
import java.util.*;
import java.math.*;
import javax.persistence.*;
import com.fasterxml.jackson.annotation.*;
import play.data.*;
import com.avaje.ebean.*;

public class TransactionList {

    public List<Transaction> transactions;

    public Boolean include_personal = false;
    public Boolean include_non_personal = false;
    public Boolean include_cash = false;
    public Boolean include_digital = false;
    public Boolean include_archived = false;

    private BigDecimal getBalance() {
        return transactions.stream()
            .map(t -> t.amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getFormattedBalance() {
        return new DecimalFormat("0.00").format(getBalance());
    }

    private BigDecimal getBalanceAsOfTransaction(Transaction transaction) {
        return transactions.stream()
            .filter(t -> t.id <= transaction.id)
            .map(t -> t.amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getFormattedBalanceAsOfTransaction(Transaction transaction) {
        return new DecimalFormat("0.00").format(getBalanceAsOfTransaction(transaction));
    }

    private static String getFormattedDescription(TransactionType type, String from, String to, String description) {
        String result = "";
        if (type == TransactionType.CashWithdrawal) {
            result += from + " withdrawal ";
        } else {
            if (!from.trim().isEmpty()) {
                result += "from " + from + " ";
            }
            if (!to.trim().isEmpty()) {
                result += "to " + to + " ";
            }
        }
        if (!description.trim().isEmpty()) {
            if (!result.isEmpty()) {
                result += " – ";
            }
            result += description;
        }
        return result;
    }

    public static TransactionList allCash() {
        TransactionList model = new TransactionList();
        model.transactions = new ArrayList<Transaction>();
        List<Transaction> deposit_transactions = Transaction.allCashDeposits();
        List<Transaction> withdrawal_transactions = Transaction.allCashWithdrawals();
        for (Transaction t : deposit_transactions) {
            t.description = getFormattedDescription(t.type, t.from_name, t.to_name, t.description);
            model.transactions.add(t);
        }
        for (Transaction t : withdrawal_transactions) {
            t.description = getFormattedDescription(t.type, t.from_name, t.to_name, t.description);
            t.amount = BigDecimal.ZERO.subtract(t.amount);
            model.transactions.add(t);
        }
        sortTransactions(model.transactions);
        return model;
    }

    public static TransactionList blank() {
        TransactionList model = new TransactionList();
        model.include_personal = true;
        model.include_non_personal = true;
        model.include_cash = true;
        model.include_digital = true;
        model.include_archived = true;
        return model;
    }

    public static TransactionList createFromForm(Form<TransactionList> form) {
        TransactionList model = form.get();

        model.transactions = Transaction.allWithConditions(
            model.include_personal,
            model.include_non_personal,
            model.include_cash,
            model.include_digital,
            model.include_archived);

        for (Transaction t : model.transactions) {
            t.description = getFormattedDescription(t.type, t.from_name, t.to_name, t.description);
        }
        sortTransactions(model.transactions);
        return model;
    }

    public static void sortTransactions(List<Transaction> transactions) {
        Collections.sort(transactions, (a, b) -> (getTransactionSortValue(b)).compareTo(getTransactionSortValue(a)));
    }

    private static String getTransactionSortValue(Transaction transaction) {
        // millisec * sec * min * hours
        // 1000 * 60 * 60 * 24 = 86400000
        int hours = (int) (transaction.date_created.getTime() / 86400000L);
        // this will sort by date, then by ID
        return Integer.toString(hours) + Integer.toString(transaction.id);
    }
}
