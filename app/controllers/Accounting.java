package controllers;

import com.csvreader.CsvWriter;
import models.*;
import play.data.Form;
import play.data.FormFactory;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@With(DumpOnError.class)
@Secured.Auth(UserRole.ROLE_VIEW_JC)
public class Accounting extends Controller {

    FormFactory mFormFactory;

    @Inject
    public Accounting(FormFactory formFactory) {
        mFormFactory = formFactory;
    }

    public Result transaction(Integer id, Http.Request request) {
        Transaction transaction = Transaction.findById(id, Organization.getByHost(request));
        Form<Transaction> form = mFormFactory.form(Transaction.class).fill(transaction);
        return ok(views.html.transaction.render(transaction, form));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result newTransaction(Http.Request request) {
        applyMonthlyCredits(Organization.getByHost(request));
        Form<Transaction> form = mFormFactory.form(Transaction.class);
        return ok(views.html.create_transaction.render(form));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result makeNewTransaction(Http.Request request) {
        Form<Transaction> form = mFormFactory.form(Transaction.class);
        Form<Transaction> filledForm = form.bindFromRequest(request);
        if (filledForm.hasErrors()) {
            System.out.println("ERRORS: " + filledForm.errorsAsJson().toString());
            return badRequest(views.html.create_transaction.render(filledForm));
        } else {
            try {
                Transaction transaction = Transaction.create(filledForm,
                        Organization.getByHost(request), Application.getCurrentUser(request));
                return redirect(routes.Accounting.transaction(transaction.id));
            }
            catch (Exception ex) {
                return badRequest(ex.toString());
            }
        }
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result saveTransaction(Http.Request request) {
        try {
            Form<Transaction> form = mFormFactory.form(Transaction.class).bindFromRequest(request);
            Transaction transaction = Transaction.findById(Integer.parseInt(form.field("id").value().get()), Organization.getByHost(request));
            transaction.updateFromForm(form);
            return redirect(routes.Accounting.transaction(transaction.id));
        }
        catch (Exception ex) {
            return badRequest(ex.toString());
        }
    }    

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result deleteTransaction(Integer id) {
        Transaction.delete(id);
        return redirect(routes.Accounting.balances());
    }

    public Result balances(Http.Request request) {
        Organization org = Organization.getByHost(request);
        applyMonthlyCredits(org);
        List<Account> personalAccounts = Account.allPersonalChecking(org);
        List<Account> nonPersonalAccounts = Account.allNonPersonalChecking(org);
        personalAccounts.sort(Comparator.comparing(Account::getName));
        nonPersonalAccounts.sort(Comparator.comparing(Account::getName));
        return ok(views.html.balances.render(personalAccounts, nonPersonalAccounts));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result bankCashBalance(Http.Request request) {
        applyMonthlyCredits(Organization.getByHost(request));
        return ok(views.html.bank_cash_balance.render(TransactionList.allCash(Organization.getByHost(request))));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result transactionsReport(Http.Request request) {
        applyMonthlyCredits(Organization.getByHost(request));
        return ok(views.html.transactions_report.render(TransactionList.blank()));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result runTransactionsReport(Http.Request request) throws IOException {
        Form<TransactionList> form = mFormFactory.form(TransactionList.class);
        Form<TransactionList> filledForm = form.bindFromRequest(request);
        if (filledForm.hasErrors()) {
            System.out.println("ERRORS: " + filledForm.errorsAsJson().toString());
            return badRequest(views.html.transactions_report.render(TransactionList.blank()));
        }
        TransactionList report = TransactionList.createFromForm(filledForm, Organization.getByHost(request));
        String action = request.body().asFormUrlEncoded().get("action")[0];
        if (action.equals("download")) {
            return downloadTransactionReport(report);
        }
        else {
            return ok(views.html.transactions_report.render(report));
        }
    }

    private Result downloadTransactionReport(TransactionList report) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Charset charset = StandardCharsets.UTF_8;
        CsvWriter writer = new CsvWriter(baos, ',', charset);

        writer.write("Archived");
        writer.write("Transaction ID");
        writer.write("Date");
        writer.write("Created By User");
        writer.write("Type");
        writer.write("Description");
        writer.write("Amount");
        writer.endRecord();

        for (Transaction t : report.transactions) {
            writer.write(t.archived ? "yes" : "no");
            writer.write(t.id.toString());
            writer.write(t.getFormattedDate());
            writer.write(t.getCreatedByUserName());
            writer.write(t.getTypeName());
            writer.write(t.description);
            writer.write(t.getFormattedAmount(false));
            writer.endRecord();
        }

        writer.close();

        return ok(baos.toByteArray()).withHeader("Content-Type", "application/csv")
                .withHeader("Content-Disposition", "attachment; filename=transactions.csv");
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result toggleTransactionArchived(int id, Http.Request request) {
        Transaction transaction = Transaction.findById(id, Organization.getByHost(request));
        transaction.archived = !transaction.archived;
        transaction.save();
        return ok();
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result report(Http.Request request) {
        applyMonthlyCredits(Organization.getByHost(request));
        return ok(views.html.accounting_report.render(new AccountingReport()));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result runReport(Http.Request request) {
        Form<AccountingReport> form = mFormFactory.form(AccountingReport.class);
        Form<AccountingReport> filledForm = form.bindFromRequest(request);
        if (filledForm.hasErrors()) {
            System.out.println("ERRORS: " + filledForm.errorsAsJson().toString());
            return badRequest(views.html.accounting_report.render(new AccountingReport()));
        }
        AccountingReport report = AccountingReport.create(filledForm, Organization.getByHost(request));
        return ok(views.html.accounting_report.render(report));
    }

    public Result account(Integer id, Http.Request request) {
        Organization org = Organization.getByHost(request);
        applyMonthlyCredits(org);
        Account account = Account.findById(id, org);
        return ok(views.html.account.render(account));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result deleteAccount(Integer id) throws Exception {
        Account.delete(id);
        return redirect(routes.Accounting.accounts());
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result accounts(Http.Request request) {
        Organization org = Organization.getByHost(request);
        applyMonthlyCredits(org);
        List<Account> accounts = Account.all(org);
        accounts.sort(Comparator.comparing(Account::getName));
        return ok(views.html.accounts.render(accounts));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result newAccount(Http.Request request) {
        Form<Account> form = mFormFactory.form(Account.class);
        return ok(views.html.new_account.render(form));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result makeNewAccount(Http.Request request) {
        Form<Account> form = mFormFactory.form(Account.class);
        Form<Account> filledForm = form.bindFromRequest(request);
        if (filledForm.hasErrors()) {
            System.out.println("ERRORS: " + filledForm.errorsAsJson().toString());
            return badRequest(views.html.new_account.render(filledForm));
        }
        else {
            String name = filledForm.field("name").value().get();
            AccountType type = AccountType.valueOf(filledForm.field("type").value().get());
            Account account = Account.create(type, name, null, Organization.getByHost(request));
            return redirect(routes.Accounting.account(account.id));
        }
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result editAccount(Integer id, Http.Request request) {
        Account account = Account.findById(id, Organization.getByHost(request));
        Form<Account> form = mFormFactory.form(Account.class).fill(account);
        return ok(views.html.edit_account.render(form, account.is_active));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result saveAccount(Http.Request request) {
        Form<Account> form = mFormFactory.form(Account.class).bindFromRequest(request);
        Account account = Account.findById(Integer.parseInt(form.field("id").value().get()), Organization.getByHost(request));
        account.updateFromForm(form);
        return redirect(routes.Accounting.account(account.id));
    }

    public static String accountsJson(Organization org) {
        List<Map<String, String>> result = new ArrayList<>();
        List<Account> accounts = new ArrayList<>();
        accounts.addAll(Account.allPersonalChecking(org));
        accounts.addAll(Account.allNonPersonalChecking(org));
        for (Account a : accounts) {
            HashMap<String, String> values = new HashMap<>();
            values.put("label", a.getName());
            values.put("id", "" + a.id);
            values.put("balance", a.getFormattedBalance());
            result.add(values);
        }
        return Json.stringify(Json.toJson(result));
    }

    static void applyMonthlyCredits(Organization org) {
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

        List<Account> accounts = Account.allWithMonthlyCredits(org).stream()
            .filter(a -> a.date_last_monthly_credit == null || a.date_last_monthly_credit.before(monthStartDate))
            .collect(Collectors.toList());

        for (Account a : accounts) {
            a.createMonthlyCreditTransaction(monthStartDate, org);
        }
    }
}