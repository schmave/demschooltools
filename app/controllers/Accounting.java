package controllers;

import java.util.*;
import models.*;
import play.mvc.*;
import play.data.*;
import play.libs.Json;

@With(DumpOnError.class)
@Secured.Auth(UserRole.ROLE_VIEW_JC)
public class Accounting extends Controller {

    public Result transaction(Integer id) {
        Transaction transaction = Transaction.findById(id);
        return ok(views.html.transaction.render(transaction));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result newTransaction() {
        Form<Transaction> form = Form.form(Transaction.class);
        return ok(views.html.create_transaction.render(form));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result makeNewTransaction() {
        Form<Transaction> form = Form.form(Transaction.class);
        Form<Transaction> filledForm = form.bindFromRequest();
        if(filledForm.hasErrors()) {
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

    public Result balances() {
        List<Account> personalAccounts = Account.allPersonalChecking();
        List<Account> institutionalAccounts = Account.allInstitutionalChecking();
        Collections.sort(personalAccounts, (a, b) -> a.getName().compareTo(b.getName()));
        Collections.sort(institutionalAccounts, (a, b) -> a.getName().compareTo(b.getName()));
        return ok(views.html.balances.render(personalAccounts, institutionalAccounts));
    }

    @Secured.Auth(UserRole.ROLE_ACCOUNTING)
    public Result pettyCash() {
        return ok(views.html.petty_cash.render(PettyCash.find()));
    }

    public Result account(Integer id) {
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
            Account.create(type, name, null);
            return redirect(routes.Accounting.accounts());
        }
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
}