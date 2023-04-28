package controllers;

import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;

import com.rollbar.api.payload.data.Person;
import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.config.ConfigBuilder;
import com.typesafe.config.Config;
import java.util.*;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.User;
import play.*;
import play.mvc.*;

public class DumpOnError extends Action.Simple {
    static Logger.ALogger sLogger = Logger.of("application");
    
    ConfigBuilder mRollbarConfig;
    @Inject
    DumpOnError(Config config) {
        mRollbarConfig = withAccessToken(config.getString("rollbar_token"))
                .environment(config.getString("rollbar_environment"))
                .codeVersion("1");
    }

    public CompletionStage<Result> call(Http.Request request) {
        User currentUser = Application.getCurrentUser(request);
        Person.Builder personBuilder =
            new Person.Builder().username(Application.currentUsername(request).orElse(null));
        if (currentUser != null) {
          personBuilder = personBuilder.id(String.valueOf(currentUser.getId()));
        }

        final Person currentPerson = personBuilder.build();

        Rollbar rollbar = Rollbar.init(mRollbarConfig.person(() -> currentPerson).build());
        return delegate.call(request).whenComplete((result, e) -> {
            if (e != null) {
                e.printStackTrace();

                Map<String, Object> rollbarInfo = new HashMap<>();
                StringBuilder sb = new StringBuilder();
                getRequestInfo(request, sb, rollbarInfo);
                rollbar.error(e, rollbarInfo);
                sLogger.error(sb.toString());
            }
        });
    }

    private void getRequestInfo(Http.Request request, StringBuilder sb, Map<String, Object> rollbarInfo) {
        rollbarInfo.put("URL", "https://" + request.host() + request.uri());

        sb.append("Error for request at ").append(request.host()).append(" ").append(request.uri()).append("\n");
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

        Http.RequestBody body = request.body();
        Map<String, String[]> body_vals = body.asFormUrlEncoded();
        if (body_vals != null) {
            sb.append("Body (as form URL encoded): \n");
            for (String key : body_vals.keySet()) {
                sb.append("  ").append(key).append(" --> ");
                rollbarInfo.put(key, body_vals.get(key));
                for (String val : body_vals.get(key)) {
                    sb.append(val).append("|||");
                }
                sb.append("\n");
            }
        }
    }
}


