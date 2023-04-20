package controllers;

import models.OrgConfig;
import models.Organization;
import play.mvc.Http;

public class Globals {
    static ThreadLocal<Globals> sGlobals = new ThreadLocal<>();

    Http.Request mRequest;

    static Globals get() {
        Globals result = sGlobals.get();
        if (result == null) {
            result = new Globals();
            sGlobals.set(result);
        }
        return result;
    }

    static void setRequest(Http.Request request) {
        get().mRequest = request;
    }

    public static Http.Request request() {
        return get().mRequest;
    }

    public static Organization org() {
        return Organization.getByHost(request());
    }

    public static OrgConfig orgConfig() {
        return OrgConfig.get(org());
    }
}
