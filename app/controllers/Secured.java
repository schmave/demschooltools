package controllers;

import java.util.ArrayList;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.user.AuthUser;

import models.User;

import play.Logger;
import play.mvc.Http.Context;
import play.mvc.Http.Session;
import play.mvc.Result;
import play.mvc.Security;

public class Secured extends Security.Authenticator {

	@Override
	public String getUsername(final Context ctx) {
        Logger.debug("Secured::getUsername " + ctx);
		final AuthUser u = PlayAuthenticate.getUser(ctx.session());

        if (u != null) {
            User the_user = User.findByAuthUserIdentity(u);
            if (the_user != null) {
				// If a user is logged in already, check the session timeout.
				// If there is no timeout, or we can't parse it, or it's too old,
				// don't count the user as being logged in.
				Session sess = ctx.session();
				if (sess.get("timeout") != null) {
					try
					{
						long timeout = Long.parseLong(sess.get("timeout"));
						if (System.currentTimeMillis() - timeout < 1000 * 60 * 30) {
							sess.put("timeout", "" + System.currentTimeMillis());
							return u.getId();
						}
					}
					catch (NumberFormatException e) {
					}
				}
			}
        }
        return null;
	}

	@Override
	public Result onUnauthorized(final Context ctx) {
        PlayAuthenticate.storeOriginalUrl(ctx);
		return redirect(controllers.routes.Public.index());
	}
}