package controllers;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.controllers.Authenticate;
import com.typesafe.config.Config;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
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

        if (u != null && u.getHashedPassword().length() > 0) {
            if (BCrypt.checkpw(password, u.getHashedPassword())) {
                return mPlayAuth.handleAuthentication("evan-auth-provider", request, u);
            }
        }

        Result result;
        if (values.get("noredirect") != null) {
            result = unauthorized();
        } else {
            result = redirect(routes.Public.index());
        }

        if (u != null && u.getHashedPassword().length() == 0) {
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
