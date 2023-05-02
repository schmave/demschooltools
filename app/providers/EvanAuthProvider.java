package providers;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.providers.AuthProvider;
import com.feth.play.module.pa.user.AuthUser;
import models.User;
import play.inject.ApplicationLifecycle;
import play.mvc.Http;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EvanAuthProvider extends AuthProvider {

  @Inject
  public EvanAuthProvider(final PlayAuthenticate auth, final ApplicationLifecycle lifecycle) {
    super(auth, lifecycle);
  }

  @Override
  public Object authenticate(final Http.Request request, final Object payload) {
    final User user = (User) payload;
    return new AuthUser() {
      private static final long serialVersionUID = 1L;

      @Override
      public String getId() {
        return user.getEmail();
      }

      @Override
      public String getProvider() {
        return "evan-auth-provider";
      }
    };
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
