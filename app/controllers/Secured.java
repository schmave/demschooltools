package controllers;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.user.AuthUser;
import io.ebean.Ebean;
import io.ebean.SqlQuery;
import io.ebean.SqlRow;
import models.Organization;
import models.User;
import models.UserRole;
import play.Logger;
import play.mvc.*;
import play.mvc.Http.Context;
import service.MyUserService;

import javax.inject.Inject;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


// Lifted from play.mvc.Security

public class Secured {
    static Logger.ALogger sLogger = Logger.of("application");

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
     * The username is retrieved from the session cookie, and added to the HTTP request's
     * <code>username</code> attribute.
     */
    public static class AuthenticatedAction extends Action<Auth> {

        PlayAuthenticate mAuth;

        @Inject
        public AuthenticatedAction(final PlayAuthenticate auth) {
            mAuth = auth;
        }

        @Override
        public CompletionStage<Result> call(final Http.Request request) {
            try {
                Authenticator authenticator = new Authenticator(mAuth);
                String username = authenticator.getUsername(request, configuration.value());
                if (username == null) {
                    Result unauthorized = authenticator.onUnauthorized(request);
                    return CompletableFuture.completedFuture(unauthorized);
                } else {
                    Http.Request childRequest = request.withAttrs(
                            request.attrs().put(Security.USERNAME, username));
                    return delegate.call(childRequest);
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

        public String getUsername(final Http.Request request, String role) {
            String username = getUsernameOrIP(request, true);
            User u = User.findByEmail(username);

            if (u == null) {
                // Allow access by IP address to ROLE_VIEW_JC things. username
                // may be null if not from a valid IP, which will deny access.
                if (role.equals(UserRole.ROLE_VIEW_JC) &&
                    // Don't let IP address users change their password
                    request.path().equals(routes.Application.viewPassword().path())) {
                    return username;
                }
            } else if (u.active && u.hasRole(role) &&
                       (u.organization == null || u.organization.equals(Organization.getByHost(request)))) {
                // Allow access if this user belongs to this organization or is a
                // multi-domain admin (null organization). Also, the user must
                // have the required role.
                return username;
            }

            return null;
        }

        public String getUsernameOrIP(final Http.Request request, boolean allow_ip) {
            sLogger.debug("Authenticator::getUsername " + request + ", " + allow_ip);
            final AuthUser u = mAuth.getUser(request.session());

            if (u != null) {
                User the_user = User.findByAuthUserIdentity(u);
                if (the_user != null) {
                    return the_user.email;
                }
            }

            // If we don't have a logged-in user, try going by IP address.
            if (allow_ip && Organization.getByHost(request) != null) {
                String sql = "select ip from allowed_ips where ip like :ip and organization_id=:org_id";
                SqlQuery sqlQuery = Ebean.createSqlQuery(sql);
                String address = Application.getRemoteIp(request);
                sqlQuery.setParameter("ip", address);
                sqlQuery.setParameter("org_id", Organization.getByHost(request).id);

                // execute the query returning a List of MapBean objects
                SqlRow result = sqlQuery.findOne();

                if (result != null) {
                    return address;
                }
            }

            return null;
        }

        public Result onUnauthorized(final Http.Request request) {
            final AuthUser u = mAuth.getUser(request.session());
            if (u != null) {
                User the_user = User.findByAuthUserIdentity(u);
                if (the_user != null) {
                    if (the_user.name.equals(MyUserService.DUMMY_USERNAME)) {
                        return ok(
                            "You logged in with Facebook or Google, but Evan hasn't made a" +
                            " DemSchoolTools account for you yet. Please contact him for help:" +
                            " schmave@gmail.com");
                    }
                    if (!the_user.active) {
                        return unauthorized("Your account with email address " +
                            the_user.email + " is inactive.");
                    }
                }
            }

            if (getUsernameOrIP(request, false) == null) {
                // Only redirect to the login screen if there
                // is no user logged in.
                //
                // If a user is logged in, but they don't have the proper role
                // for the page they are trying to access, logging in again
                // isn't going to help them.
                mAuth.storeOriginalUrl(Context.current());
                return redirect(routes.Public.index());
            } else {
                return unauthorized("You can't access this page.");
            }
        }

    }
}

