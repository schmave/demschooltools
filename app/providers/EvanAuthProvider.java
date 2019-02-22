package providers;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.providers.AuthProvider;
import com.feth.play.module.pa.user.AuthUser;
import play.inject.ApplicationLifecycle;
import play.mvc.Http.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

import models.LinkedAccount;
import models.User;

@Singleton
public class EvanAuthProvider extends AuthProvider {

    @Inject
    public EvanAuthProvider(final PlayAuthenticate auth,
                            final ApplicationLifecycle lifecycle) {
        super(auth, lifecycle);
    }

    @Override
    public Object authenticate(final Context context, final Object payload) {
        final User user = (User) payload;
        AuthUser result = new AuthUser() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getId() {
                return user.email;
            }

            @Override
            public String getProvider() {
                return "evan-auth-provider";
            }
        };
        return result;
    }

    @Override
    public boolean isExternal() {
        return false;
    }

    @Override
    public String getKey() {
        return "evan-auth-provider";
    }
}
