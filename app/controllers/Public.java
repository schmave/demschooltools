package controllers;

import com.ecwid.mailchimp.MailChimpClient;
import com.ecwid.mailchimp.MailChimpException;
import com.ecwid.mailchimp.MailChimpObject;
import com.ecwid.mailchimp.method.v2_0.lists.*;
import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.controllers.Authenticate;
import com.typesafe.config.Config;
import models.*;
import org.mindrot.jbcrypt.BCrypt;
import play.Environment;
import play.api.libs.mailer.MailerClient;
import play.cache.SyncCacheApi;
import play.i18n.MessagesApi;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;
import views.html.logged_out;
import views.html.login;
import views.html.sync_email;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;

@Singleton
@With(DumpOnError.class)
public class Public extends Controller {

    PlayAuthenticate mPlayAuth;
    Authenticate mAuth;

    public static SyncCacheApi sCache;
    public static Environment sEnvironment;
    public static Config sConfig;
    MailerClient mMailer;
    final MessagesApi mMessagesApi;


    @Inject
    public Public(final PlayAuthenticate playAuth, final Authenticate auth, final SyncCacheApi cache,
                  final Environment environment, final Config config, final MailerClient mailer,
                  MessagesApi messagesApi) {
        mPlayAuth = playAuth;
        mAuth = auth;
        sCache = cache;
        sEnvironment = environment;
        sConfig = config;
        mMailer = mailer;
        mMessagesApi = messagesApi;
    }

    public Result facebookDeleteInfo() {
        return ok("Hello Facebook user!\n\n" +
            "If you would like to delete your user info that is stored with DemSchoolTools,\n" +
            "please send an email with your request to schmave@gmail.com.");
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

    // Return true iff person was previously subscribed to this list, and
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

        Map<String, ListMethodResult.Data> result = new HashMap<>();

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
        catch (IOException | MailChimpException e) {
            e.printStackTrace();
        }

        return result;
    }

    public Result syncMailchimp(Http.Request request) {
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
                PersonChange.find.query().where().eq("person.organization", org)
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
                    change.person.person_id.equals(last_changed_person.person_id)) {
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
                        } catch (IOException | MailChimpException e) {
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
                        List<MailchimpSync> syncs = MailchimpSync.find.query().where()
                            .eq("tag", t).eq("sync_local_adds", true).findList();
                        for (MailchimpSync sync : syncs) {
                            try {
                                addPersonToList(client, org.mailchimp_api_key, sync.mailchimp_list_id, change.person);
                                info.add(sync.mailchimp_list_id, change.person);
                            } catch (IOException | MailChimpException e) {
                                info.error(sync.mailchimp_list_id, "Changed email, adding " + change.person + ": " + e);
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            org.setLastMailChimpSyncTime(new Date());

            for (Tag t : Tag.find.query().where().eq("organization", org).findList()) {
                for (MailchimpSync sync : t.syncs) {
                    if (sync.last_sync == null) {
                        sync.last_sync = new Date(0, Calendar.JANUARY, 1);
                    }
                    List<PersonTagChange> tag_changes = PersonTagChange.find.query().where()
                        .eq("tag", t)
                        .gt("time", sync.last_sync).findList();

                    // todo: find all people in this tag who have a PersonChange where old_email=""
                    //     Treat them as an add.

                    // Go through the list of all changes and save
                    // only the most recent one for each person
                    Map<Person, Boolean> person_to_add_value = new HashMap<>();
                    for (PersonTagChange change : tag_changes) {
                        person_to_add_value.put(change.person, change.was_add);
                    }

                    List<Person> adds = new ArrayList<>();
                    List<Person> removes = new ArrayList<>();

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
                            catch (IOException | MailChimpException e) {
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
                            catch (IOException | MailChimpException e) {
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
                mail.setBodyHtml(sync_email.render(mc_lists, info, request, mMessagesApi.preferred(request)).toString());
                mMailer.send(mail);
            }
        }
        return ok("");
    }

    public Result oAuthDenied(String ignoredProvider)
    {
        return redirect(routes.Public.index());
    }

    public Result checkin() {
        return redirect("/assets/checkin/app.html");
    }

    public Result index(Http.Request request)
    {
		if (Utils.getOrg(request) == null) {
			return unauthorized("Unknown organization");
		}
        return ok(login.render(mPlayAuth,
                request.flash().get("notice").orElse(null),
                Application.getRemoteIp(request), request, mMessagesApi.preferred(request)));
    }

    public Result doLogin(Http.Request request) {
        final Map<String, String[]> values = request.body().asFormUrlEncoded();

        String email = values.get("email")[0];
        User u = User.findByEmail(email);

        String password = values.get("password")[0];

        if (u != null && u.hashed_password.length() > 0) {
            if (BCrypt.checkpw(password, u.hashed_password)) {
                return mPlayAuth.handleAuthentication("evan-auth-provider", request, u);
            }
        }

        Result result;
        if (values.get("noredirect") != null) {
            result = unauthorized();
        } else {
            result = redirect(routes.Public.index());
        }

        if (u != null && u.hashed_password.length() == 0) {
            result = result.flashing("notice", "Failed to login: password login is not enabled for your account");
        } else {
            result = result.flashing("notice", "Failed to login: wrong email address or password");
        }
        return result;
    }

    public Result authenticate(String provider, Http.Request request) {
        return mAuth.authenticate(provider, request);
    }

    public Result loggedOut(Http.Request request) {
        return ok(logged_out.render(request, mMessagesApi.preferred(request)));
    }
}