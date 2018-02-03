package models;

import java.text.*;
import java.util.*;
import java.math.*;
import javax.persistence.*;
import com.fasterxml.jackson.annotation.*;
import play.data.*;
import com.avaje.ebean.*;

public class PettyCash {

    public List<Transaction> deposit_transactions;
    public List<Transaction> withdrawal_transactions;

    private BigDecimal getBalance() {
        return deposit_transactions.stream().map(t -> t.amount).reduce(BigDecimal.ZERO, BigDecimal::add)
            .subtract(withdrawal_transactions.stream().map(t -> t.amount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public String getFormattedBalance() {
        return new DecimalFormat("0.00").format(getBalance());
    }

    public List<Transaction> getTransactionsViewModel() {
        List<Transaction> result = new ArrayList<Transaction>();
        for (Transaction t : deposit_transactions) {
            t.description = getFormattedDescription(t.from_name, t.to_name, t.description);
            result.add(t);
        }
        for (Transaction t : withdrawal_transactions) {
            t.description = getFormattedDescription(t.from_name, t.to_name, t.description);
            t.amount = BigDecimal.ZERO.subtract(t.amount);
            result.add(t);
        }
        Collections.sort(result, (a, b) -> b.id.compareTo(a.id));
        return result;
    }

    private String getFormattedDescription(String from, String to, String description) {
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
        model.deposit_transactions = Transaction.allCashDeposits();
        model.withdrawal_transactions = Transaction.allCashWithdrawals();
        return model;
    }
}
