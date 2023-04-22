package com.feth.play.module.pa.providers;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.exceptions.AuthException;
import com.feth.play.module.pa.user.AuthUser;
import com.feth.play.module.pa.user.SessionAuthUser;
import com.typesafe.config.Config;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.mvc.Http.Request;
import play.mvc.Http.Session;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class AuthProvider {
	static Logger.ALogger sLogger = Logger.of("application");

	public abstract static class Registry {
		private static final Map<String, AuthProvider> providers = new HashMap<>();

		public static void register(final String provider, final AuthProvider p) {
			final Object previous = providers.put(provider, p);
			if (previous != null) {
				sLogger.warn("There are multiple AuthProviders registered for key '"
						+ provider + "'");
			}
		}

		public static void unregister(final String provider) {
			providers.remove(provider);
		}

		public static AuthProvider get(final String provider) {
			return providers.get(provider);
		}

		public static Collection<AuthProvider> getProviders() {
			return providers.values();
		}

		public static boolean hasProvider(final String provider) {
			return providers.containsKey(provider);
		}
	}


	protected PlayAuthenticate auth;

	public PlayAuthenticate getAuth() {
		return auth;
	}

	public AuthProvider(final PlayAuthenticate auth, final ApplicationLifecycle lifecycle) {
		this.auth = auth;
		onStart();
		lifecycle.addStopHook(() -> {
			onStop();
			return CompletableFuture.completedFuture(null);
		});
	}

	protected void onStart() {

		final List<String> neededSettings = neededSettingKeys();
		if (neededSettings != null) {
			final Config c = getConfiguration();
			if (c == null) {
				throw new RuntimeException("No settings for provider '"
						+ getKey() + "' available at all!");
			}
			for (final String key : neededSettings) {
				if (!c.hasPath(key) || c.getString(key).isEmpty()) {
					throw new RuntimeException("Provider '" + getKey()
							+ "' missing needed setting '" + key + "'");
				}
			}
		}

		Registry.register(getKey(), this);
		sLogger.debug("Registered AuthProvider '" + getKey() + "'");
	}

	protected void onStop() {
		Registry.unregister(getKey());
	}

	public String getUrl() {
		return this.auth.getResolver().auth(getKey()).url();
	}

	public abstract String getKey();

	protected Config getConfiguration() {
		return this.auth.getConfiguration().getConfig(getKey());
	}

	/**
	 *
	 * @param request
	 *            The current request
	 * @param payload
	 *            Some arbitrary payload that shall get passed into the
	 *            authentication process
	 * @return either an AuthUser object or a String (URL)
	 * @throws AuthException
	 */
	public abstract Object authenticate(final Request request,
			final Object payload) throws AuthException;

	protected List<String> neededSettingKeys() {
		return null;
	}

	public AuthUser getSessionAuthUser(final String id, final long expires) {
		return new SessionAuthUser(getKey(), id, expires);
	}

	public abstract boolean isExternal();

    /**
     * This gets called after a successful 'save' operation of the UserService.
     *
     * @param user The user object
     * @param identity The user identity returned fro, the save operation in the UserService
     * @param session The session
     */
    public void afterSave(final AuthUser user, final Object identity, final Session session) {

    }
}
