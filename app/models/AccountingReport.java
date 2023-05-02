package models;

import java.math.*;
import java.text.*;
import java.util.*;
import play.data.*;

public class AccountingReport {

  @play.data.format.Formats.DateTime(pattern = "MM/dd/yyyy")
  public Date date = new Date();

  public AccountingReportType type;

  public String resultLabel = "";
  public String result = "";

  public String getFormattedDate() {
    return new SimpleDateFormat("M/d/yy").format(date);
  }

  public static AccountingReport create(Form<AccountingReport> form, Organization org) {
    AccountingReport report = form.get();
    if (report.type == AccountingReportType.TotalPersonalAccountsBalance) {
      runTotalPersonalAccountsBalanceReport(report, org);
    }
    return report;
  }

  public static void runTotalPersonalAccountsBalanceReport(
      AccountingReport report, Organization org) {
    List<Account> accounts = Account.allPersonalChecking(org);

    BigDecimal total =
        accounts.stream()
            .map(t -> t.getBalanceAsOf(report.date))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    report.resultLabel =
        "Total Personal Accounts Balance as of " + report.getFormattedDate() + ": ";
    report.result = "$" + new DecimalFormat("0.00").format(total);
  }
}
