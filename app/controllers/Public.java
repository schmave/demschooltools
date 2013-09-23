package controllers;

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

    public static Result oAuthDenied(String provider)
    {
        return redirect(routes.Public.index());
    }

    public static Result index()
    {
        final AuthUser u = PlayAuthenticate.getUser(Context.current().session());
        if (u != null) {
            return redirect(routes.Application.index());
        }
        return ok(views.html.login.render());
    }
}
