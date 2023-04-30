package module;

import com.feth.play.module.pa.Resolver;
import com.feth.play.module.pa.providers.oauth2.facebook.FacebookAuthProvider;
import com.feth.play.module.pa.providers.oauth2.google.GoogleAuthProvider;
import com.google.inject.AbstractModule;
import controllers.Application;
import providers.EvanAuthProvider;
import service.MyResolver;
import service.MyUserService;

/**
 * Initial DI module.
 */
public class MyModule extends AbstractModule {

    @Override
    protected void configure() {
        // install(new FactoryModuleBuilder().implement(IMailer.class, Mailer.class).build(MailerFactory.class));

        bind(Resolver.class).to(MyResolver.class);
        // bind(DataInitializer.class).asEagerSingleton();

        bind(MyUserService.class).asEagerSingleton();
        bind(GoogleAuthProvider.class).asEagerSingleton();
        bind(FacebookAuthProvider.class).asEagerSingleton();
        bind(Application.class).asEagerSingleton();
        bind(EvanAuthProvider.class).asEagerSingleton();
    }
}
