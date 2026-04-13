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
import play.data.Form;
import play.api.libs.mailer.MailerClient;
import play.cache.SyncCacheApi;
import play.i18n.MessagesApi;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.html.logged_out;
import views.html.login;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import play.data.FormFactory;
import play.data.validation.Constraints;


@Singleton
public class Public extends Controller {

  PlayAuthenticate mPlayAuth;
  Authenticate mAuth;

  public static SyncCacheApi sCache;
  public static Environment sEnvironment;
  public static Config sConfig;
  MailerClient mMailer;
  final MessagesApi mMessagesApi;
  FormFactory mFormFactory;

  @Inject
  public Public(
      final PlayAuthenticate playAuth,
      final Authenticate auth,
      final SyncCacheApi cache,
      final Environment environment,
      final Config config,
      final MailerClient mailer,
      MessagesApi messagesApi,
    final FormFactory formFactory) {
    mPlayAuth = playAuth;
    mAuth = auth;
    sCache = cache;
    sEnvironment = environment;
    sConfig = config;
    mMailer = mailer;
    mMessagesApi = messagesApi;
    mFormFactory = formFactory;
  }

  public Result facebookDeleteInfo() {
    return ok(
        "Hello Facebook user!\n\n"
            + "If you would like to delete your user info that is stored with DemSchoolTools,\n"
            + "please send an email with your request to schmave@gmail.com.");
  }

  public Result oAuthDenied(String ignoredProvider) {
    return redirect(routes.Public.index());
  }

  public Result checkin() {
    return redirect("/assets/checkin/app.html");
  }

  public Result index(Http.Request request) {
    if (Utils.getOrg(request) == null) {
      return unauthorized("Unknown organization");
    }
    return ok(
        login.render(
            mPlayAuth,
            request.flash().get("notice").orElse(null),
            Application.getRemoteIp(request),
            request,
            mMessagesApi.preferred(request)));
  }


  @Data
  @NoArgsConstructor
  public static class LoginData {
      @Constraints.Required
      private String email;

      @Constraints.Required
      private String password;

      private String noredirect;
  }

  public Result doLogin(Http.Request request) {
    Form<LoginData> loginForm = mFormFactory.form(LoginData.class).bindFromRequest(request);
    if (loginForm.hasErrors()) {
        return badRequest("Invalid data");
    }

    LoginData data = loginForm.get();

    String email = data.getEmail();
    User u = User.findByEmail(email);

    String password = data.getPassword();

    if (u != null && u.getHashedPassword().length() > 0) {
      if (BCrypt.checkpw(password, u.getHashedPassword())) {
        return mPlayAuth.handleAuthentication("evan-auth-provider", request, u);
      }
    }

    Result result;

    if (data.getNoredirect() != null) {
      result = unauthorized();
    } else {
      result = redirect(routes.Public.index());
    }

    if (u != null && u.getHashedPassword().length() == 0) {
      result =
          result.flashing(
              "notice", "Failed to login: password login is not enabled for your account");
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
