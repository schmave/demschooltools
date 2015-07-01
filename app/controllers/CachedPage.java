package controllers;

import java.util.*;

import models.*;

import play.cache.Cache;

public abstract class CachedPage {
    public String title;
    public String menu;
    public String selected_button;
    public String cache_key;

    static String getKey(String key_base) {
        return key_base + "-" + OrgConfig.get().org.id;
    }

    public static void remove(String key_base) {
        Cache.remove(getKey(key_base));
    }

    public CachedPage(String key_base, String title, String menu,
        String selected_button) {
        this.title = title;
        this.cache_key = getKey(key_base);
        this.menu = menu;
        this.selected_button = selected_button;
    }

    public String getPage() {
        String result = (String)Cache.get(cache_key);
        if (result != null) {
            return result;
        }

        result = render();
        // cache it for 12 hours
        Cache.set(cache_key, result, 60 * 60 * 12);
        return result;
    }

    abstract String render();
}
