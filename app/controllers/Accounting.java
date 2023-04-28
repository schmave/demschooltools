package controllers;

import com.csvreader.CsvWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
import models.*;
import play.data.Form;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;
import views.html.*;

@With(DumpOnError.class)
@Secured.Auth(UserRole.ROLE_VIEW_JC)
public class Accounting extends Controller {

    FormFactory mFormFactory;
    final MessagesApi mMessagesApi;

    @Inject
    public Accounting(FormFactory formFactory,
                      MessagesApi messagesApi) {
        mFormFactory = formFactory;
        mMessagesApi = messagesApi;
    }

    public Result transaction(Integer id, Http.Request request) {
        Transaction transaction = Transaction.findById(id, Utils.getOrg(request));
        Form<Transaction> form = mFormFactory.form(Transaction.class).fill(transaction);
        return ok(views.html.transaction.render(transaction, form, request, mMessagesApi.preferred(request)));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result newTransaction(Http.Request request) {
        applyMonthlyCredits(Utils.getOrg(request));
        Form<Transaction> form = mFormFactory.form(Transaction.class);
        return ok(create_transaction.render(form, request, mMessagesApi.preferred(request)));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result makeNewTransaction(Http.Request request) {
        Form<Transaction> form = mFormFactory.form(Transaction.class);
        Form<Transaction> filledForm = form.bindFromRequest(request);
        if (filledForm.hasErrors()) {
            System.out.println("ERRORS: " + filledForm.errorsAsJson().toString());
            return badRequest(create_transaction.render(filledForm, request, mMessagesApi.preferred(request)));
        } else {
            try {
                Transaction transaction = Transaction.create(filledForm,
                        Utils.getOrg(request), Application.getCurrentUser(request));
                return redirect(routes.Accounting.transaction(transaction.getId()));
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
            Transaction transaction = Transaction.findById(Integer.parseInt(form.field("id").value().get()), Utils.getOrg(request));
            transaction.updateFromForm(form);
            return redirect(routes.Accounting.transaction(transaction.getId()));
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
        Organization org = Utils.getOrg(request);
        applyMonthlyCredits(org);
        List<Account> personalAccounts = Account.allPersonalChecking(org);
        List<Account> nonPersonalAccounts = Account.allNonPersonalChecking(org);
        personalAccounts.sort(Comparator.comparing(Account::getName));
        nonPersonalAccounts.sort(Comparator.comparing(Account::getName));
        return ok(balances.render(personalAccounts, nonPersonalAccounts, request, mMessagesApi.preferred(request)));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result bankCashBalance(Http.Request request) {
        applyMonthlyCredits(Utils.getOrg(request));
        return ok(bank_cash_balance.render(TransactionList.allCash(Utils.getOrg(request)), request, mMessagesApi.preferred(request)));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result transactionsReport(Http.Request request) {
        applyMonthlyCredits(Utils.getOrg(request));
        return ok(transactions_report.render(TransactionList.blank(), request, mMessagesApi.preferred(request)));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result runTransactionsReport(Http.Request request) throws IOException {
        Form<TransactionList> form = mFormFactory.form(TransactionList.class);
        Form<TransactionList> filledForm = form.bindFromRequest(request);
        if (filledForm.hasErrors()) {
            System.out.println("ERRORS: " + filledForm.errorsAsJson().toString());
            return badRequest(transactions_report.render(TransactionList.blank(), request, mMessagesApi.preferred(request)));
        }
        TransactionList report = TransactionList.createFromForm(filledForm, Utils.getOrg(request));
        String action = request.body().asFormUrlEncoded().get("action")[0];
        if (action.equals("download")) {
            return downloadTransactionReport(report);
        }
        else {
            return ok(transactions_report.render(report, request, mMessagesApi.preferred(request)));
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
            writer.write(t.getArchived() ? "yes" : "no");
            writer.write(t.getId().toString());
            writer.write(t.getFormattedDate());
            writer.write(t.getCreatedByUserName());
            writer.write(t.getTypeName());
            writer.write(t.getDescription());
            writer.write(t.getFormattedAmount(false));
            writer.endRecord();
        }

        writer.close();

        return ok(baos.toByteArray()).as("application/csv")
                .withHeader("Content-Disposition", "attachment; filename=transactions.csv");
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result toggleTransactionArchived(int id, Http.Request request) {
        Transaction transaction = Transaction.findById(id, Utils.getOrg(request));
        transaction.setArchived(!transaction.getArchived());
        transaction.save();
        return ok();
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result report(Http.Request request) {
        applyMonthlyCredits(Utils.getOrg(request));
        return ok(accounting_report.render(new AccountingReport(), request, mMessagesApi.preferred(request)));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result runReport(Http.Request request) {
        Form<AccountingReport> form = mFormFactory.form(AccountingReport.class);
        Form<AccountingReport> filledForm = form.bindFromRequest(request);
        if (filledForm.hasErrors()) {
            System.out.println("ERRORS: " + filledForm.errorsAsJson().toString());
            return badRequest(accounting_report.render(new AccountingReport(), request, mMessagesApi.preferred(request)));
        }
        AccountingReport report = AccountingReport.create(filledForm, Utils.getOrg(request));
        return ok(accounting_report.render(report, request, mMessagesApi.preferred(request)));
    }

    public Result account(Integer id, Http.Request request) {
        Organization org = Utils.getOrg(request);
        applyMonthlyCredits(org);
        Account account = Account.findById(id, org);
        return ok(views.html.account.render(account, request, mMessagesApi.preferred(request)));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result deleteAccount(Integer id) throws Exception {
        Account.delete(id);
        return redirect(routes.Accounting.accounts());
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result accounts(Http.Request request) {
        Organization org = Utils.getOrg(request);
        applyMonthlyCredits(org);
        List<Account> accounts = Account.all(org);
        accounts.sort(Comparator.comparing(Account::getName));
        return ok(views.html.accounts.render(accounts, request, mMessagesApi.preferred(request)));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result newAccount(Http.Request request) {
        Form<Account> form = mFormFactory.form(Account.class);
        return ok(new_account.render(form, request, mMessagesApi.preferred(request)));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result makeNewAccount(Http.Request request) {
        Form<Account> form = mFormFactory.form(Account.class);
        Form<Account> filledForm = form.bindFromRequest(request);
        if (filledForm.hasErrors()) {
            System.out.println("ERRORS: " + filledForm.errorsAsJson().toString());
            return badRequest(new_account.render(filledForm, request, mMessagesApi.preferred(request)));
        }
        else {
            String name = filledForm.field("name").value().get();
            AccountType type = AccountType.valueOf(filledForm.field("type").value().get());
            Account account = Account.create(type, name, null, Utils.getOrg(request));
            return redirect(routes.Accounting.account(account.getId()));
        }
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result editAccount(Integer id, Http.Request request) {
        Account account = Account.findById(id, Utils.getOrg(request));
        Form<Account> form = mFormFactory.form(Account.class).fill(account);
        return ok(edit_account.render(form, account.isActive(), request, mMessagesApi.preferred(request)));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result saveAccount(Http.Request request) {
        Form<Account> form = mFormFactory.form(Account.class).bindFromRequest(request);
        Account account = Account.findById(Integer.parseInt(form.field("id").value().get()), Utils.getOrg(request));
        account.updateFromForm(form);
        return redirect(routes.Accounting.account(account.getId()));
    }

    public static String accountsJson(Organization org) {
        List<Map<String, String>> result = new ArrayList<>();
        List<Account> accounts = new ArrayList<>();
        accounts.addAll(Account.allPersonalChecking(org));
        accounts.addAll(Account.allNonPersonalChecking(org));
        for (Account a : accounts) {
            HashMap<String, String> values = new HashMap<>();
            values.put("label", a.getName());
            values.put("id", "" + a.getId());
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
            .filter(a -> a.getDateLastMonthlyCredit() == null || a.getDateLastMonthlyCredit().before(monthStartDate))
            .collect(Collectors.toList());

        for (Account a : accounts) {
            a.createMonthlyCreditTransaction(monthStartDate, org);
        }
    }
}
