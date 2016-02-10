package controllers;

import java.util.*;

import play.*;
import play.data.*;
import play.libs.F;
import play.libs.Json;
import play.mvc.*;
import play.mvc.Http.Context;

public class DumpOnError extends Action.Simple {
    public F.Promise<Result> call(Http.Context ctx) throws Throwable {
        try {
            return delegate.call(ctx);
        }
        catch (Exception e) {
            e.printStackTrace();
            StringBuilder sb = new StringBuilder();

            sb.append("Error for request at " + ctx.request().uri() + "\n");
            sb.append("Headers: \n");
            Map<String, String[]> headers = ctx.request().headers();
            for (String key : headers.keySet()) {
                sb.append("  " + key + " --> ");
                for (String val : headers.get(key)) {
                    sb.append(val + "|||");
                }
                sb.append("\n");
            }

            sb.append("Cookies: \n");
            for (Http.Cookie cookie : ctx.request().cookies()) {
                sb.append("  " + cookie.name() + " --> " + cookie.value() + "\n");
            }

            Http.RequestBody body = ctx.request().body();
            Map<String, String[]> body_vals = body.asFormUrlEncoded();
            if (body_vals != null) {
                sb.append("Body (as form URL encoded): \n");
                for (String key : body_vals.keySet()) {
                    sb.append("  " + key + " --> ");
                    for (String val : body_vals.get(key)) {
                        sb.append(val + "|||");
                    }
                    sb.append("\n");
                }
            }

            Logger.error(sb.toString());
            throw e;
        }
    }
}


