package controllers;

import java.util.ArrayList;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.user.AuthUser;

import models.User;

import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;

public class Secured extends Security.Authenticator {

    ArrayList<String> allowed_users = new ArrayList<String>();

    public Secured()
    {
        allowed_users.add("schmave@gmail.com");
        allowed_users.add("janceybell@gmail.com");
        allowed_users.add("chad.whitco@gmail.com");
        allowed_users.add("sarahbanach@gmail.com");
        allowed_users.add("jeanmarie.sp@gmail.com");
    }

	@Override
	public String getUsername(final Context ctx) {
		final AuthUser u = PlayAuthenticate.getUser(ctx.session());

		if (u != null) {
            User the_user = User.findByAuthUserIdentity(u);
            if (allowed_users.contains(the_user.email)) {
                return u.getId();
            }
        }

        return null;
	}

	@Override
	public Result onUnauthorized(final Context ctx) {
		return redirect(com.feth.play.module.pa.controllers.routes.Authenticate.logout());
	}
}