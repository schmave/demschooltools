package models;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import play.data.Form;

public class TransactionList {

  public List<Transaction> transactions;

  public Boolean include_personal = false;
  public Boolean include_non_personal = false;
  public Boolean include_cash_deposits = false;
  public Boolean include_cash_withdrawals = false;
  public Boolean include_digital = false;
  public Boolean include_archived = false;
  public Date start_date;
  public Date end_date;

  public static List<Transaction> getTransactionsViewModel(Account account) {
    List<Transaction> result = new ArrayList<>();
    for (Transaction t : account.credit_transactions) {
      t.setDescription(
          account.getFormattedDescription("from", t.getFromName(), t.getDescription()));
      result.add(t);
    }
    for (Transaction t : account.payment_transactions) {
      t.setDescription(account.getFormattedDescription("to", t.getToName(), t.getDescription()));
      t.setAmount(BigDecimal.ZERO.subtract(t.getAmount()));
      result.add(t);
    }
    sortTransactions(result);
    return result;
  }

  private BigDecimal getBalance() {
    return transactions.stream().map(t -> t.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public String getFormattedBalance() {
    return new DecimalFormat("0.00").format(getBalance());
  }

  private BigDecimal getBalanceAsOfTransaction(Transaction transaction) {
    return transactions.stream()
        .filter(
            t -> {
              long time1 = t.getDateCreated().getTime();
              long time2 = transaction.getDateCreated().getTime();
              if (time1 == time2) {
                return t.getId() <= transaction.getId();
              }
              return time1 < time2;
            })
        .map(t -> t.getAmount())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public String getFormattedBalanceAsOfTransaction(Transaction transaction) {
    return new DecimalFormat("0.00").format(getBalanceAsOfTransaction(transaction));
  }

  private static String getFormattedDescription(
      TransactionType type, String from, String to, String description) {
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

  public static TransactionList allCash(Organization org) {
    TransactionList model = new TransactionList();
    model.transactions = new ArrayList<>();
    List<Transaction> deposit_transactions = Transaction.allCashDeposits(org);
    List<Transaction> withdrawal_transactions = Transaction.allCashWithdrawals(org);
    for (Transaction t : deposit_transactions) {
      t.setDescription(
          getFormattedDescription(t.getType(), t.getFromName(), t.getToName(), t.getDescription()));
      model.transactions.add(t);
    }
    for (Transaction t : withdrawal_transactions) {
      t.setDescription(
          getFormattedDescription(t.getType(), t.getFromName(), t.getToName(), t.getDescription()));
      t.setAmount(BigDecimal.ZERO.subtract(t.getAmount()));
      model.transactions.add(t);
    }
    sortTransactions(model.transactions);
    return model;
  }

  public static TransactionList blank() {
    TransactionList model = new TransactionList();
    model.include_personal = true;
    model.include_non_personal = true;
    model.include_cash_deposits = true;
    model.include_cash_withdrawals = true;
    model.include_digital = true;
    model.include_archived = true;
    return model;
  }

  public static TransactionList createFromForm(Form<TransactionList> form, Organization org) {
    TransactionList model = form.get();

    model.transactions =
        Transaction.allWithConditions(
            model.include_personal,
            model.include_non_personal,
            model.include_cash_deposits,
            model.include_cash_withdrawals,
            model.include_digital,
            model.include_archived,
            model.start_date,
            model.end_date,
            org);

    for (Transaction t : model.transactions) {
      t.setDescription(
          getFormattedDescription(t.getType(), t.getFromName(), t.getToName(), t.getDescription()));
    }
    sortTransactions(model.transactions);
    return model;
  }

  public static void sortTransactions(List<Transaction> transactions) {
    transactions.sort((a, b) -> (getTransactionSortValue(b)).compareTo(getTransactionSortValue(a)));
  }

  private static Integer getTransactionSortValue(Transaction transaction) {
    // Convert milliseconds to minutes so we can fit into an Integer
    int minutes = (int) (transaction.getDateCreated().getTime() / 60000);
    // The goal is to sort by date, then by ID.
    // This will work as long as the difference in minutes between two differently-dated
    // transactions is always
    // greater than the difference in ID numbers between them, which will practically always be the
    // case.
    return minutes + transaction.getId();
  }
}
