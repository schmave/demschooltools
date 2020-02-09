package controllers;

import models.*;

import play.cache.Cache;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public abstract class CachedPage {
    public static final String ATTENDANCE_INDEX = "Attendance-index-";
    public static final String JC_INDEX = "Application-index-";
    public static final String MANUAL_INDEX = "Application-viewManual-";
    public static final String RECENT_COMMENTS = "CRM-recentComments-";

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
        byte[] cached_bytes = (byte[])Cache.get(cache_key);
        if (cached_bytes != null) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(cached_bytes);
                GZIPInputStream gzis = new GZIPInputStream(bais);
                BufferedReader reader = new BufferedReader(new InputStreamReader(gzis));

                StringBuilder sb = new StringBuilder();
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    sb.append(line);
                }
                return sb.toString();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Exception reading cached bytes");
            }
        }

        String result = null;
        // Only render one cached page at a time (per server process)
        synchronized(JC_INDEX) {
            result = render();
        }

        try {
            byte[] bytes_to_compress = result.getBytes("UTF-8");
            ByteArrayOutputStream baos = new ByteArrayOutputStream(result.length());
            GZIPOutputStream gzos = new GZIPOutputStream(baos);
            gzos.write(bytes_to_compress);
            gzos.finish();

            byte[] compressed_bytes = baos.toByteArray();

            System.out.println("Cache " + cache_key + ": orig size " + bytes_to_compress.length
                + "; compressed: " + compressed_bytes.length);

            // cache it for 12 hours
            Cache.set(cache_key, compressed_bytes, 60 * 60 * 12);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Exception writing cached bytes");
        }

        return result;
    }

    abstract String render();

    public static void clearAll() {
        remove(ATTENDANCE_INDEX);
        remove(JC_INDEX);
        remove(MANUAL_INDEX);
        remove(RECENT_COMMENTS);
        Utils.updateCustodia();
    }

    public static void onPeopleChanged() {
        remove(ATTENDANCE_INDEX);
        remove(JC_INDEX);
        remove(RECENT_COMMENTS);
        Utils.updateCustodia();
    }
}
