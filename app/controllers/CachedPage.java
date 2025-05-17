package controllers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import models.*;

public abstract class CachedPage {
  public static final String JC_INDEX = "Application-index-";
  public static final String JC_INDEX_LOGGED_OUT = "Application-index-lo-";
  public static final String RECENT_COMMENTS = "CRM-recentComments-";

  public String title;
  public String menu;
  public String selected_button;
  public String cache_key;

  static String getKey(String key_base, Organization org) {
    return key_base + "-" + org.getId();
  }

  public static void remove(String key_base, Organization org) {
    Public.sCache.remove(getKey(key_base, org));
  }

  public CachedPage(
      String key_base, String title, String menu, String selected_button, Organization org) {
    this.title = title;
    this.cache_key = getKey(key_base, org);
    this.menu = menu;
    this.selected_button = selected_button;
  }

  public String getPage() {
    Optional<byte[]> cached_bytes = Public.sCache.get(cache_key);
    if (cached_bytes.isPresent()) {
      try {
        ByteArrayInputStream bais = new ByteArrayInputStream(cached_bytes.get());
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

    String result;
    // Only render one cached page at a time (per server process)
    synchronized (JC_INDEX) {
      result = render();
    }

    try {
      byte[] bytes_to_compress = result.getBytes(StandardCharsets.UTF_8);
      ByteArrayOutputStream baos = new ByteArrayOutputStream(result.length());
      GZIPOutputStream gzos = new GZIPOutputStream(baos);
      gzos.write(bytes_to_compress);
      gzos.finish();

      byte[] compressed_bytes = baos.toByteArray();

      System.out.println(
          "Cache "
              + cache_key
              + ": orig size "
              + bytes_to_compress.length
              + "; compressed: "
              + compressed_bytes.length);

      // cache it for 12 hours
      Public.sCache.set(cache_key, compressed_bytes, 60 * 60 * 12);
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("Exception writing cached bytes");
    }

    return result;
  }

  abstract String render();

  public static void clearAll(Organization org) {
    remove(JC_INDEX, org);
    remove(RECENT_COMMENTS, org);
  }

  public static void onPeopleChanged(Organization org) {
    remove(JC_INDEX, org);
    remove(RECENT_COMMENTS, org);
  }
}
