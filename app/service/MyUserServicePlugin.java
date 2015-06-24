package service;

import com.feth.play.module.pa.service.UserServicePlugin;
import com.feth.play.module.pa.user.AuthUser;
import com.feth.play.module.pa.user.AuthUserIdentity;
import com.feth.play.module.pa.user.EmailIdentity;
import com.google.inject.Inject;

import models.LinkedAccount;
import models.User;

import play.Application;
import play.Logger;

public class MyUserServicePlugin extends UserServicePlugin {

    @Inject
	public MyUserServicePlugin(final Application app) {
		super(app);
	}

    // We do not create new user accounts. If you aren't already approved,
    // you can't log in.
	@Override
	public Object save(final AuthUser authUser) {
        Logger.debug("MyUserServicePlugin::save " + authUser);
		final boolean isLinked = User.existsByAuthUserIdentity(authUser);
		if (!isLinked) {
            if (authUser instanceof EmailIdentity) {
                final EmailIdentity identity = (EmailIdentity) authUser;
                Logger.debug("    is email identity, email='" + identity.getEmail() + "'");
                User u = User.find.where().eq("email", identity.getEmail()).findUnique();
                if (u != null) {
                    Logger.debug("    found user by email");
                    u.linkedAccounts.add(LinkedAccount.create(authUser));
                    u.save();
                    return u;
                }
            }
		}

        return null;
	}

	@Override
	public Object getLocalIdentity(final AuthUserIdentity identity) {
        Logger.debug("MyUserServicePlugin::getLocalIdentity " + identity);
		// For production: Caching might be a good idea here...
		// ...and dont forget to sync the cache when users get deactivated/deleted
		final User u = User.findByAuthUserIdentity(identity);
		if(u != null) {
            Logger.debug("    found user by auth identity");
			return u.id;
		} else {
			return null;
		}
	}

	@Override
	public AuthUser merge(final AuthUser newUser, final AuthUser oldUser) {
		if (!oldUser.equals(newUser)) {
			User.merge(oldUser, newUser);
		}
		return oldUser;
	}

	@Override
	public AuthUser link(final AuthUser oldUser, final AuthUser newUser) {
		User.addLinkedAccount(oldUser, newUser);
		return null;
	}

}
