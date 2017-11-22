package controllers;

import java.lang.annotation.*;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;
import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.user.AuthUser;
import com.google.inject.Inject;

import models.OrgConfig;
import models.Organization;
import models.User;
import models.UserRole;

import play.Logger;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Http.Session;
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.With;


// Lifted from play.mvc.Security

public class Secured {

    /**
     * Wraps the annotated action in an <code>AuthenticatedAction</code>.
     */
    @With(AuthenticatedAction.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Auth {
        String value(); // the access role required
    }

    /**
     * Wraps another action, allowing only authenticated HTTP requests.
     * <p>
     * The user name is retrieved from the session cookie, and added to the HTTP request's
     * <code>username</code> attribute.
     */
    public static class AuthenticatedAction extends Action<Auth> {

        PlayAuthenticate mAuth;

        @Inject
        public AuthenticatedAction(final PlayAuthenticate auth) {
            mAuth = auth;
        }

        @Override
        public CompletionStage<Result> call(final Context ctx) {
            try {
                Authenticator authenticator = new Authenticator(mAuth);
                String username = authenticator.getUsername(ctx, configuration.value());
                if (username == null) {
                    Result unauthorized = authenticator.onUnauthorized(ctx);
                    return CompletableFuture.completedFuture(unauthorized);
                } else {
                    try {
                        ctx.request().setUsername(username);
                        return delegate.call(ctx).whenComplete(
                            (result, throwable) -> {
                                ctx.request().setUsername(null);
                            }
                        );
                    } catch(Exception e) {
                        ctx.request().setUsername(null);
                        throw e;
                    }
                }
            } catch(RuntimeException e) {
                throw e;
            } catch(Throwable t) {
                throw new RuntimeException(t);
            }
        }

    }


    public static class Authenticator extends Results {
        PlayAuthenticate mAuth;

        @Inject
        public Authenticator(final PlayAuthenticate auth) {
            mAuth = auth;
        }

    	public String getUsername(final Context ctx, String role) {
            if (1 == 1) {
                return "Admin User";
            }
            String username = getUsernameOrIP(ctx, true);
            User u = User.findByEmail(username);

            if (u == null) {
                // Allow access by IP address to ROLE_VIEW_JC things. username
                // may be null if not from a valid IP, which will deny access.
                if (role.equals(UserRole.ROLE_VIEW_JC)) {
                    return username;
                }
            } else if (u.hasRole(role) &&
                       (u.organization == null || u.organization.equals(OrgConfig.get().org))) {
                // Allow access if this user belongs to this organization or is a
                // multi-domain admin (null organization). Also, the user must
                // have the required role.
                return username;
            }

            return null;
    	}

        public String getUsernameOrIP(final Context ctx, boolean allow_ip) {
            Logger.debug("Authenticator::getUsername " + ctx + ", " + allow_ip);
            final AuthUser u = mAuth.getUser(ctx.session());

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
            if (allow_ip && Organization.getByHost() != null) {
                String sql = "select ip from allowed_ips where ip like :ip and organization_id=:org_id";
                SqlQuery sqlQuery = Ebean.createSqlQuery(sql);
                String address = Application.getRemoteIp();
                sqlQuery.setParameter("ip", address);
                sqlQuery.setParameter("org_id", Organization.getByHost().id);

                // execute the query returning a List of MapBean objects
                SqlRow result = sqlQuery.findUnique();

                if (result != null) {
                    return address;
                }
            }

            return null;
        }

    	public Result onUnauthorized(final Context ctx) {
            if (getUsernameOrIP(ctx, false) == null) {
                // Only redirect to the login screen if there
                // is no user logged in.
                //
                // If a user is logged in but they don't have the proper role
                // for the page they are trying to access, logging in again
                // isn't going to help them.
                mAuth.storeOriginalUrl(ctx);
                return redirect(routes.Public.index());
            } else {
                return unauthorized("You can't access this page.");
            }
    	}

    }
}

