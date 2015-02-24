package controllers;

import java.io.IOException;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;

import com.ecwid.mailchimp.*;
import com.ecwid.mailchimp.method.v2_0.lists.BatchError;
import com.ecwid.mailchimp.method.v2_0.lists.BatchSubscribeInfo;
import com.ecwid.mailchimp.method.v2_0.lists.BatchSubscribeMethod;
import com.ecwid.mailchimp.method.v2_0.lists.BatchSubscribeResult;
import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.user.AuthUser;

import models.*;

import play.*;
import play.data.*;
import play.libs.Json;
import play.mvc.*;
import play.mvc.Http.Context;

import views.html.*;

public class Public extends Controller {

	public static Result postEmail() {
		final java.util.Map<String, String[]> values = request().body().asFormUrlEncoded();

		Email new_email = Email.create(values.get("email")[0]);
		/* new_email.parseMessage();

		boolean autoSend = false;
		Address[] rcpts = new_email.parsedMessage.getRecipients(Message.ReceipientType.TO);
		for (Address a : rcpts) {
			if (a.toString().contains("papal+parents@")) {
				autoSend = true;
			}
		}

		if (autoSend) {

		}
		*/

        return ok();
	}

    public static Result syncMailchimp() {
        MailChimpClient client = new MailChimpClient();

        //Map<Organization, Map<String, Person>> org_to_list_to_changes =
        //    new HashMap<Organization, Map<String, List<PersonTagChange>>>();

        for (Organization org : Organization.find.all()) {
            //Map<String, List<PersonTagChange>> list_to_changes =
            //    org_to_list_to_changes.put(org, new HashMap<String, PersonTagChange>());

            for (Tag t : Tag.find.where().eq("organization", org).findList()) {
                for (MailchimpSync sync : t.syncs) {
                    List<PersonTagChange> changes = PersonTagChange.find.where()
                        .eq("tag", t)
                        .gt("time", sync.last_sync).findList();

                    // Go through the list of all changes and save
                    // only the most recent one for each person
                    Map<Person, Boolean> person_to_add_value = new HashMap<Person, Boolean>();
                    for (PersonTagChange change : changes) {
                        person_to_add_value.put(change.person, change.was_add);
                    }

                    List<Person> adds = new ArrayList<Person>();
                    List<Person> removes = new ArrayList<Person>();

                    for (Person p : person_to_add_value.keySet()) {
                        if (sync.sync_local_adds && person_to_add_value.get(p)) {
                            adds.add(p);
                        }
                        if (sync.sync_local_removes && !person_to_add_value.get(p)) {
                            removes.add(p);
                        }
                    }

                    if (adds.size() > 0) {
                        BatchSubscribeMethod method = new BatchSubscribeMethod();
                        method.apikey = org.mailchimp_api_key;
                        method.id = sync.mailchimp_list_id;
                        method.double_optin = false;
                        method.update_existing = true;
                        method.batch = new ArrayList<BatchSubscribeInfo>();
                        for (Person p : adds) {
                            if (p.email.length() > 0) {
                                System.out.println("adding " + p.email + " to " + method.id);
                                BatchSubscribeInfo info = new BatchSubscribeInfo();
                                info.email = new com.ecwid.mailchimp.method.v2_0.lists.Email();
                                info.email.email = p.email;
                                info.merge_vars = new MailChimpObject();
                                info.merge_vars.put("first_name", p.first_name);
                                info.merge_vars.put("last_name", p.last_name);
                            }
                        }

                        try {
                            BatchSubscribeResult result = client.execute(method);
                            for (BatchError err : result.errors) {
                                System.out.println("Error code " + err.code + " (" + err.error + ") for email " + err.email);
                            }
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                        catch (MailChimpException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return ok();
    }

    public static Result oAuthDenied(String provider)
    {
        session().remove("timeout");
        return redirect(routes.Public.index());
    }

    public static Result index()
    {
        String u = new Secured().getUsername(ctx(), false);
        if (u != null) {
            return redirect(routes.Application.index());
        }
        return ok(views.html.login.render(flash("notice")));
    }

    public static Result authenticate(String provider) {
        session("timeout", "" + System.currentTimeMillis());
        return com.feth.play.module.pa.controllers.Authenticate.authenticate(provider);
    }
}
