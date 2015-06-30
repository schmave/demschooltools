package controllers;

import models.OrgConfig;
import models.Organization;

import play.cache.Cache;
import play.libs.F;
import play.mvc.*;
import play.mvc.Http.*;

public class OrgCachedAction extends Action<OrgCached> {

    public F.Promise<Result> call(Context ctx) {
        try {
            final String key = getKey(configuration.key());
            final Integer duration = configuration.duration();
            Result result = (Result) Cache.get(key);
            F.Promise<Result> promise;
            if(result == null) {
                promise = delegate.call(ctx);
                promise.onRedeem(result1 -> Cache.set(key, result1, duration));
            } else {
                promise = F.Promise.pure(result);
            }
            return promise;
        } catch(RuntimeException e) {
            throw e;
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    static String getKey(String key_base) {
        return key_base + "-" + OrgConfig.get().org.id;
    }

    public static void remove(String key) {
        Cache.remove(getKey(key));
    }

}
