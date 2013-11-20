package controllers;

import java.util.ArrayList;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;
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
        return getUsername(ctx, true);
	}

    public String getUsername(final Context ctx, boolean allow_ip) {
        Logger.debug("Secured::getUsername " + ctx + ", " + allow_ip);
        final AuthUser u = PlayAuthenticate.getUser(ctx.session());

        if (u != null) {
            User the_user = User.findByAuthUserIdentity(u);
            if (the_user == null) {
                return null;
            }

            return the_user.email;
        }

        // If we don't have a logged-in user, try going by IP address.
        if (allow_ip) {
            String sql = "select ip from allowed_ips where ip like :ip";
            SqlQuery sqlQuery = Ebean.createSqlQuery(sql);
            sqlQuery.setParameter("ip", Application.getRemoteIp());

            // execute the query returning a List of MapBean objects
            SqlRow result = sqlQuery.findUnique();

            if (result != null) {
                return ctx.request().remoteAddress();
            }
        }

        return null;
    }

	@Override
	public Result onUnauthorized(final Context ctx) {
		return redirect(com.feth.play.module.pa.controllers.routes.Authenticate.logout());
	}
}