package controllers;

import java.util.*;
import java.util.concurrent.CompletionStage;

import play.*;
import play.mvc.*;


public class DumpOnError extends Action.Simple {
    static Logger.ALogger sLogger = Logger.of("application");
    public CompletionStage<Result> call(Http.Request request) {
        return delegate.call(request).whenComplete((result, e) -> {
            if (e != null) {
                e.printStackTrace();
                StringBuilder sb = new StringBuilder();

                sb.append("Error for request at ").append(request.host()).append(" ").append(request.uri()).append("\n");
                sb.append("Headers: \n");
                Map<String, List<String>> headers = request.getHeaders().toMap();
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
                        for (String val : body_vals.get(key)) {
                            sb.append(val).append("|||");
                        }
                        sb.append("\n");
                    }
                }

                sLogger.error(sb.toString());
            }
        });
    }
}


