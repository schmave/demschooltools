package controllers;

import java.io.IOException;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;
import javax.inject.Singleton;

import com.ecwid.mailchimp.*;
import com.ecwid.mailchimp.method.v2_0.lists.ListMethod;
import com.ecwid.mailchimp.method.v2_0.lists.ListMethodResult;
import com.ecwid.mailchimp.method.v2_0.lists.SubscribeMethod;
import com.ecwid.mailchimp.method.v2_0.lists.UnsubscribeMethod;
import com.ecwid.mailchimp.method.v2_0.lists.UpdateMemberMethod;
import com.feth.play.module.pa.controllers.Authenticate;
import com.feth.play.module.pa.PlayAuthenticate;
import com.google.inject.Inject;

import models.*;

import org.mindrot.jbcrypt.BCrypt;


import play.*;
import play.data.*;
import play.libs.Json;
import play.mvc.*;
import play.mvc.Http.Context;

import views.html.*;

@Singleton
@With(DumpOnError.class)
public class Public extends Controller {

    PlayAuthenticate mPlayAuth;
    Authenticate mAuth;

    @Inject
    public Public(final PlayAuthenticate playAuth, final Authenticate auth) {
        mPlayAuth = playAuth;
        mAuth = auth;
    }

	public Result postEmail() {
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

    static void addPersonToList(MailChimpClient client, String api_key, String list_id, Person p) throws MailChimpException, IOException {
        SubscribeMethod method = new SubscribeMethod();
        method.apikey = api_key;
        method.id = list_id;
        method.double_optin = false;
        method.update_existing = true;
        if (p.email.length() > 0) {
            System.out.println("adding " + p.email + " to " + method.id);
            method.email = new com.ecwid.mailchimp.method.v2_0.lists.Email();
            method.email.email = p.email;
            method.merge_vars = new MailChimpObject();
            method.merge_vars.put("FNAME", p.first_name);
            method.merge_vars.put("LNAME", p.last_name);

            client.execute(method);
        }
    }

    // Return true iff the person was previously subscribed to this list
    // and their info was updated.
    static boolean updatePersonInList(MailChimpClient client, String api_key, String list_id, String old_email, Person p) throws MailChimpException, IOException {
        UpdateMemberMethod method = new UpdateMemberMethod();
        method.apikey = api_key;
        method.id = list_id;
        if (p.email.length() > 0) {
            System.out.println("updating " + old_email + " in " + method.id);
            method.email = new com.ecwid.mailchimp.method.v2_0.lists.Email();
            method.email.email = old_email;
            method.merge_vars = new MailChimpObject();
            method.merge_vars.put("FNAME", p.first_name);
            method.merge_vars.put("LNAME", p.last_name);
            method.merge_vars.put("new-email", p.email);
        }

        try {
            client.execute(method);
            return true;
        }
        catch (MailChimpException e) {
            if (e.code != 232 && e.code != 215) {
                throw e;
            }
            else {
                System.out.println(old_email + " not subscribed to this list.");
                return false;
            }
        }
    }

    // Return true iff person was previously subscribed to this list and
    // we removed them from the list.
    static boolean removePersonFromList(MailChimpClient client, String api_key, String list_id, String email) throws MailChimpException, IOException {
        if (email.equals("")) {
            return false;
        }

        UnsubscribeMethod method = new UnsubscribeMethod();
        method.apikey = api_key;
        method.id = list_id;
        method.send_goodbye = false;
        method.delete_member = true;
        method.email = new com.ecwid.mailchimp.method.v2_0.lists.Email();
        method.email.email = email;

        try {
            System.out.println("removing " + email + " from " + method.id);
            client.execute(method);
            return true;
        }
        catch (MailChimpException e) {
            if (e.code != 232 && e.code != 215) {
                throw e;
            }
            else {
                System.out.println(email + " not subscribed to this list.");
                return false;
            }
        }
    }

    public static Map<String, ListMethodResult.Data> getMailChimpLists(MailChimpClient client, String api_key) {
        ListMethod method = new ListMethod();
        method.apikey = api_key;

        Map<String, ListMethodResult.Data> result = new HashMap<String, ListMethodResult.Data>();

        if (api_key.equals("")) {
            // client.execute() returns an IllegalArgumentException when api_key is empty.
            return result;
        }

        try {
            ListMethodResult method_result = client.execute(method);
            for (ListMethodResult.Data data : method_result.data) {
                result.put(data.id, data);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (MailChimpException e) {
            e.printStackTrace();
        }

        return result;
    }

    public Result syncMailchimp() {
        // Disable mailchimp integration for now.
        if (1 == 1) {
            return ok("");
        }

        MailChimpClient client = new MailChimpClient();

        for (Organization org : Organization.find.all()) {
            if (org.mailchimp_api_key.equals("")) {
                // Can't do any mailchimp syncing if an org has no mailchimp API key
                continue;
            }
            MailChimpSyncInfo info = new MailChimpSyncInfo();
            // Find all PersonChanges since last
            // Organization.mailchimp_last_sync_person_changes. Sync them,
            // excluding where old_email="" If new email is "", unsubscribe them
            // from all lists.
            List<PersonChange> changes =
                PersonChange.find.where().eq("person.organization", org)
                    .gt("time", org.mailchimp_last_sync_person_changes)
                    .order("person, time ASC").findList();
            Map<String, ListMethodResult.Data> mc_lists = getMailChimpLists(client, org.mailchimp_api_key);

            Person last_changed_person = null;
            for (PersonChange change : changes) {
                // If a person's email has been changed more than once since the last
                // sync, we only need to look at the oldest record. That record
                // should contain the email address that mailchimp knows about.
                // The person object itself contains the latest data.
                if (last_changed_person != null &&
                    change.person.person_id == last_changed_person.person_id) {
                    continue;
                }
                last_changed_person = change.person;

                if (!change.old_email.equals("")) {
                    for (String list_id : mc_lists.keySet()) {
                        try {
                            if (change.person.email.equals("")) {
                                if (removePersonFromList(client, org.mailchimp_api_key, list_id, change.old_email)) {
                                    info.remove(list_id, change.person);
                                }
                            }
                            else {
                                if (updatePersonInList(client, org.mailchimp_api_key, list_id, change.old_email, change.person)) {
                                    info.update(list_id, change.person);
                                }
                            }
                        } catch (IOException e) {
                            info.error(list_id, "Updating " + change.person + ": " + e);
                            e.printStackTrace();
                        } catch (MailChimpException e) {
                            info.error(list_id, "Updating " + change.person + ": " + e);
                            e.printStackTrace();
                        }
                    }
                } else {
                    // If a person previously didn't have an email address,
                    // add them to all lists which have an "add" sync from a tag
                    // that the person is tagged with.
                    change.person.loadTags();
                    for (Tag t : change.person.tags) {
                        List<MailchimpSync> syncs = MailchimpSync.find.where()
                            .eq("tag", t).eq("sync_local_adds", true).findList();
                        for (MailchimpSync sync : syncs) {
                            try {
                                addPersonToList(client, org.mailchimp_api_key, sync.mailchimp_list_id, change.person);
                                info.add(sync.mailchimp_list_id, change.person);
                            } catch (IOException e) {
                                info.error(sync.mailchimp_list_id, "Changed email, adding " + change.person + ": " + e);
                                e.printStackTrace();
                            } catch (MailChimpException e) {
                                info.error(sync.mailchimp_list_id, "Changed email, adding " + change.person + ": " + e);
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            org.setLastMailChimpSyncTime(new Date());

            for (Tag t : Tag.find.where().eq("organization", org).findList()) {
                for (MailchimpSync sync : t.syncs) {
                    if (sync.last_sync == null) {
                        sync.last_sync = new Date(0, 1, 1);
                    }
                    List<PersonTagChange> tag_changes = PersonTagChange.find.where()
                        .eq("tag", t)
                        .gt("time", sync.last_sync).findList();

                    // todo: find all people in this tag who have a PersonChange where old_email=""
                    //     Treat them as an add.

                    // Go through the list of all changes and save
                    // only the most recent one for each person
                    Map<Person, Boolean> person_to_add_value = new HashMap<Person, Boolean>();
                    for (PersonTagChange change : tag_changes) {
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
                        for (Person p : adds) {
                            if (p.email.equals("")) {
                                continue;
                            }
                            try {
                                addPersonToList(client, org.mailchimp_api_key, sync.mailchimp_list_id, p);
                                info.add(sync.mailchimp_list_id, p);
                            }
                            catch (IOException e) {
                                info.error(sync.mailchimp_list_id, "Adding " + p.first_name + ": " + e);
                                e.printStackTrace();
                            }
                            catch (MailChimpException e) {
                                info.error(sync.mailchimp_list_id, "Adding " + p.first_name + ": " + e);
                                e.printStackTrace();
                            }
                        }
                    }

                    if (removes.size() > 0) {
                        for (Person p : removes) {
                            try {
                                if (removePersonFromList(client, org.mailchimp_api_key, sync.mailchimp_list_id, p.email)) {
                                    info.remove(sync.mailchimp_list_id, p);
                                }
                            }
                            catch (IOException e) {
                                info.error(sync.mailchimp_list_id, "Removing " + p.first_name + ": " + e);
                                e.printStackTrace();
                            }
                            catch (MailChimpException e) {
                                info.error(sync.mailchimp_list_id, "Removing " + p.first_name + ": " + e);
                                e.printStackTrace();
                            }
                        }
                    }

                    sync.updateLastSync(new Date());
                }
            }

            if (!org.mailchimp_updates_email.equals("") &&
                !info.isEmpty()) {
                play.libs.mailer.Email mail = new play.libs.mailer.Email();
                mail.setSubject("People database: Nightly updates");
                mail.addTo(org.mailchimp_updates_email);
                mail.setFrom("Papal DB <noreply@threeriversvillageschool.org>");
                mail.setBodyHtml(views.html.sync_email.render(mc_lists, info).toString());
                play.libs.mailer.MailerPlugin.send(mail);
            }
        }
        return ok("");
    }

    public Result oAuthDenied(String provider)
    {
        return redirect(routes.Public.index());
    }

    public Result index()
    {
		if (Organization.getByHost() == null) {
			return unauthorized("Unknown organization");
		}
        return ok(views.html.login.render(mPlayAuth, flash("notice")));
    }

    public Result doLogin() {
        final Map<String, String[]> values = request().body().asFormUrlEncoded();

        String email = values.get("email")[0];
        User u = User.findByEmail(email);

        String password = values.get("password")[0];

        if (u != null && u.hashed_password.length() > 0) {
            if (BCrypt.checkpw(password, u.hashed_password)) {
                return mPlayAuth.handleAuthentication("evan-auth-provider", Context.current(), u);
            }
        }
        flash("notice", "Failed to login: wrong email address or password");

        return redirect(routes.Public.index());
    }

    public Result authenticate(String provider) {
        return mAuth.authenticate(provider);
    }

    public Result loggedOut() {
        return ok(views.html.logged_out.render());
    }
}
