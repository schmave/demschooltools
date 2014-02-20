package controllers;

import java.util.ArrayList;
import java.util.Date;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;
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
        return getUsername(ctx, true);
	}

    public String getUsername(final Context ctx, boolean allow_ip) {
        Logger.debug("Secured::getUsername " + ctx + ", " + allow_ip);
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
							return the_user.email;
						}
					}
					catch (NumberFormatException e) {
					}
				}
			}
        }

        // If we don't have a logged-in user, try going by IP address.
        if (allow_ip) {
            String sql = "select ip from allowed_ips where ip like :ip";
            SqlQuery sqlQuery = Ebean.createSqlQuery(sql);
            String address = Application.getRemoteIp();
            sqlQuery.setParameter("ip", address);

            // execute the query returning a List of MapBean objects
            SqlRow result = sqlQuery.findUnique();

            if (result != null) {
                return address;
            }
        }

        PlayAuthenticate.storeOriginalUrl(ctx);

        return null;
    }

	@Override
	public Result onUnauthorized(final Context ctx) {
		return redirect(routes.Public.index());
	}
}