package controllers;

import java.util.*;
import java.util.stream.*;
import models.*;
import play.mvc.*;
import play.data.*;
import play.libs.*;

@With(DumpOnError.class)
@Secured.Auth(UserRole.ROLE_VIEW_JC)
public class Accounting extends Controller {

    public Result transaction(Integer id) {
        Transaction transaction = Transaction.findById(id);
        return ok(views.html.transaction.render(transaction));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result newTransaction() {
        applyMonthlyCredits();
        Form<Transaction> form = Form.form(Transaction.class);
        return ok(views.html.create_transaction.render(form));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result makeNewTransaction() {
        Form<Transaction> form = Form.form(Transaction.class);
        Form<Transaction> filledForm = form.bindFromRequest();
        if (filledForm.hasErrors()) {
            System.out.println("ERRORS: " + filledForm.errorsAsJson().toString());
            return badRequest(views.html.create_transaction.render(filledForm));
        } else {
            try {
                Transaction transaction = Transaction.create(filledForm);
                return redirect(routes.Accounting.transaction(transaction.id));
            }
            catch (Exception ex) {
                return badRequest(ex.toString());
            }
        }
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result deleteTransaction(Integer id) {
        Transaction.delete(id);
        return redirect(routes.Accounting.balances());
    }

    public Result balances() {
        applyMonthlyCredits();
        List<Account> personalAccounts = Account.allPersonalChecking();
        List<Account> institutionalAccounts = Account.allInstitutionalChecking();
        Collections.sort(personalAccounts, (a, b) -> a.getName().compareTo(b.getName()));
        Collections.sort(institutionalAccounts, (a, b) -> a.getName().compareTo(b.getName()));
        return ok(views.html.balances.render(personalAccounts, institutionalAccounts));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result bankCashBalance() {
        applyMonthlyCredits();
        return ok(views.html.bank_cash_balance.render(TransactionList.allCash()));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result allTransactions() {
        applyMonthlyCredits();
        return ok(views.html.all_transactions.render(TransactionList.all()));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result toggleTransactionArchived(int id) {
        Transaction transaction = Transaction.findById(id);
        transaction.archived = !transaction.archived;
        transaction.save();
        return ok();
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result report() {
        applyMonthlyCredits();
        return ok(views.html.accounting_report.render(new AccountingReport()));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result runReport() {
        Form<AccountingReport> form = Form.form(AccountingReport.class);
        Form<AccountingReport> filledForm = form.bindFromRequest();
        if (filledForm.hasErrors()) {
            System.out.println("ERRORS: " + filledForm.errorsAsJson().toString());
            return badRequest(views.html.accounting_report.render(new AccountingReport()));
        }
        AccountingReport report = AccountingReport.create(filledForm);
        return ok(views.html.accounting_report.render(report));
    }

    public Result account(Integer id) {
        applyMonthlyCredits();
        Account account = Account.findById(id);
        return ok(views.html.account.render(account));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result deleteAccount(Integer id) throws Exception {
        Account.delete(id);
        return redirect(routes.Accounting.accounts());
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result accounts() {
        applyMonthlyCredits();
        List<Account> accounts = Account.all();
        Collections.sort(accounts, (a, b) -> a.getName().compareTo(b.getName()));
        return ok(views.html.accounts.render(accounts));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result newAccount() {
        Form<Account> form = Form.form(Account.class);
        return ok(views.html.new_account.render(form));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result makeNewAccount() {
        Form<Account> form = Form.form(Account.class);
        Form<Account> filledForm = form.bindFromRequest();
        if (filledForm.hasErrors()) {
            System.out.println("ERRORS: " + filledForm.errorsAsJson().toString());
            return badRequest(views.html.new_account.render(filledForm));
        }
        else {
            String name = filledForm.field("name").value();
            AccountType type = AccountType.valueOf(filledForm.field("type").value());
            Account account = Account.create(type, name, null);
            return redirect(routes.Accounting.account(account.id));
        }
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result editAccount(Integer id) {
        Account account = Account.findById(id);
        Form<Account> form = Form.form(Account.class).fill(account);
        return ok(views.html.edit_account.render(form));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result saveAccount() {
        Form<Account> form = Form.form(Account.class).bindFromRequest();
        Account account = Account.findById(Integer.parseInt(form.field("id").value()));
        account.updateFromForm(form);
        return redirect(routes.Accounting.account(account.id));
    }

    public static String accountsJson() {
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        for (Account a : Account.all()) {
            HashMap<String, String> values = new HashMap<String, String>();
            values.put("label", a.getName());
            values.put("id", "" + a.id);
            values.put("balance", a.getFormattedBalance());
            result.add(values);
        }
        return Json.stringify(Json.toJson(result));
    }

    static void applyMonthlyCredits() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        Date monthStartDate = c.getTime();
        int month = c.get(Calendar.MONTH) + 1;

        // no credits in the months of July and August
        if (month == 7 || month == 8) return;

        List<Account> accounts = Account.allWithMonthlyCredits().stream()
            .filter(a -> a.date_last_monthly_credit == null || a.date_last_monthly_credit.before(monthStartDate))
            .collect(Collectors.toList());

        for (Account a : accounts) {
            a.createMonthlyCreditTransaction(monthStartDate);
        }
    }
}