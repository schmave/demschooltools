package controllers;

import java.util.*;
import java.util.concurrent.CompletionStage;

import play.*;
import play.mvc.*;


public class DumpOnError extends Action.Simple {
    public CompletionStage<Result> call(Http.Context ctx) {
        return delegate.call(ctx).whenComplete((result, e) -> {
            if (e != null) {
                e.printStackTrace();
                StringBuilder sb = new StringBuilder();

                sb.append("Error for request at ").append(ctx.request().host()).append(" ").append(ctx.request().uri()).append("\n");
                sb.append("Headers: \n");
                Map<String, List<String>> headers = ctx.request().getHeaders().toMap();
                for (String key : headers.keySet()) {
                    sb.append("  ").append(key).append(" --> ");
                    for (String val : headers.get(key)) {
                        sb.append(val).append("|||");
                    }
                    sb.append("\n");
                }

                sb.append("Cookies: \n");
                for (Http.Cookie cookie : ctx.request().cookies()) {
                    sb.append("  ").append(cookie.name()).append(" --> ").append(cookie.value()).append("\n");
                }

                Http.RequestBody body = ctx.request().body();
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

                Logger.error(sb.toString());
            }
        });
    }
}


