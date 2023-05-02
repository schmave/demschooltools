import com.rollbar.api.payload.data.Person;
import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.config.ConfigBuilder;
import com.typesafe.config.Config;
import controllers.Application;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import models.User;
import play.*;
import play.api.OptionalSourceMapper;
import play.api.UsefulException;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.mvc.*;

@Singleton
public class ErrorHandler extends DefaultHttpErrorHandler {
  static Logger.ALogger sLogger = Logger.of("application");
  ConfigBuilder mRollbarConfig;

  @Inject
  public ErrorHandler(
      Config config,
      Environment environment,
      OptionalSourceMapper sourceMapper,
      Provider<Router> routes) {
    super(config, environment, sourceMapper, routes);
    mRollbarConfig =
        ConfigBuilder.withAccessToken(config.getString("rollbar_token"))
            .environment(config.getString("rollbar_environment"))
            .codeVersion("1");
  }

  @Override
  //    public CompletionStage<Result> onServerError(
  //            Http.RequestHeader request, Throwable e) {
  public CompletionStage<Result> onProdServerError(Http.RequestHeader request, UsefulException e) {
    User currentUser = Application.getCurrentUser(request);
    final Person.Builder personBuilder =
        new Person.Builder().username(Application.currentUsername(request).orElse(null));
    if (currentUser != null) {
      personBuilder.id(String.valueOf(currentUser.getId()));
    }

    Rollbar rollbar = Rollbar.init(mRollbarConfig.person(personBuilder::build).build());

    Map<String, Object> rollbarInfo = new HashMap<>();
    StringBuilder sb = new StringBuilder();
    getRequestInfo(request, sb, rollbarInfo);
    rollbar.error(e, rollbarInfo);
    sLogger.error(sb.toString());
    e.printStackTrace();

    return CompletableFuture.completedFuture(
        Results.internalServerError(
                "<html><body style='font-size: 1.3em; background-color: #ebe2ff'>"
                    + "<h2>Server Error :(</h2><p>A server error occurred. "
                    + "Sorry for the inconvenience. The problem has been reported to Evan. "
                    + "</p><p>If the error continues to happen, please let him know by emailing "
                    + "<a href=\"mailto:schmave@gmail.com\">schmave@gmail.com</a>.</p>")
            .as("text/html"));
  }

  private static void getRequestInfo(
      Http.RequestHeader request, StringBuilder sb, Map<String, Object> rollbarInfo) {
    final String url = "https://" + request.host() + request.uri();
    rollbarInfo.put("URL", url);
    sb.append("Error for request at ").append(url).append("\n");
    sb.append("Headers: \n");
    Map<String, List<String>> headers = request.getHeaders().asMap();
    for (String key : headers.keySet()) {
      sb.append("  ").append(key).append(" --> ");
      for (String val : headers.get(key)) {
        sb.append(val).append("|||");
      }
      sb.append("\n");
    }

    sb.append("Cookies: \n");
    for (Http.Cookie cookie : request.cookies()) {
      sb.append("  ").append(cookie.name()).append(" --> ").append(cookie.value()).append("\n");
    }
  }
}
