package models;

import java.text.*;
import java.util.*;
import java.math.*;
import javax.persistence.*;
import com.fasterxml.jackson.annotation.*;
import play.data.*;
import com.avaje.ebean.*;

public class PettyCash {

    public List<Transaction> transactions;

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

    private static String getFormattedDescription(String from, String to, String description) {
        String result = "";
        if (!from.trim().isEmpty()) {
            result += "from " + from + " ";
        }
        if (!to.trim().isEmpty()) {
            result += "to " + to + " ";
        }
        if (!description.trim().isEmpty()) {
            if (!result.isEmpty()) {
                result += " – ";
            }
            result += description;
        }
        return result;
    }

    public static PettyCash find() {
        PettyCash model = new PettyCash();
        model.transactions = new ArrayList<Transaction>();
        List<Transaction> deposit_transactions = Transaction.allCashDeposits();
        List<Transaction> withdrawal_transactions = Transaction.allCashWithdrawals();
        for (Transaction t : deposit_transactions) {
            t.description = getFormattedDescription(t.from_name, t.to_name, t.description);
            model.transactions.add(t);
        }
        for (Transaction t : withdrawal_transactions) {
            t.description = getFormattedDescription(t.from_name, t.to_name, t.description);
            t.amount = BigDecimal.ZERO.subtract(t.amount);
            model.transactions.add(t);
        }
        Collections.sort(model.transactions, (a, b) -> b.id.compareTo(a.id));
        return model;
    }
}
