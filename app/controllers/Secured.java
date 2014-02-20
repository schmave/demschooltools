package controllers;

import java.util.ArrayList;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.user.AuthUser;

import models.User;

import play.Logger;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;

public class Secured extends Security.Authenticator {

	@Override
	public String getUsername(final Context ctx) {
        Logger.debug("Secured::getUsername " + ctx);
		final AuthUser u = PlayAuthenticate.getUser(ctx.session());

		if (u != null) {
            User the_user = User.findByAuthUserIdentity(u);
            if (the_user == null) {
                return null;
            }

            return u.getId();
        }

        PlayAuthenticate.storeOriginalUrl(ctx);

        return null;
	}

	@Override
	public Result onUnauthorized(final Context ctx) {
		return redirect(controllers.routes.Public.index());
	}
}