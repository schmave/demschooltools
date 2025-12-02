package controllers;

import akka.util.ByteString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import play.mvc.*;
import play.mvc.Http.Cookie;
import play.mvc.Http.Cookie.SameSite;

class CookieParser {
  public static Cookie parseSetCookieHeader(String headerValue) {
    String[] parts = headerValue.split(";");
    String[] nameValue = parts[0].split("=", 2);
    if (nameValue.length < 2) {
      throw new IllegalArgumentException("Invalid Set-Cookie header: " + headerValue);
    }

    String name = nameValue[0].trim();
    String value = nameValue[1].trim();
    String domain = null;
    String path = "/";
    boolean secure = false;
    boolean httpOnly = false;
    Integer maxAge = null;
    SameSite sameSite = null;

    for (int i = 1; i < parts.length; i++) {
      String[] attribute = parts[i].trim().split("=", 2);
      String attrName = attribute[0].trim().toLowerCase();
      String attrValue = attribute.length > 1 ? attribute[1].trim() : "";

      switch (attrName) {
        case "domain":
          domain = attrValue;
          break;
        case "path":
          path = attrValue;
          break;
        case "secure":
          secure = true;
          break;
        case "httponly":
          httpOnly = true;
          break;
        case "max-age":
          try {
            maxAge = Integer.parseInt(attrValue);
          } catch (NumberFormatException ignored) {
          }
          break;
        case "samesite":
          sameSite = parseSameSite(attrValue);
          break;
      }
    }

    return new Cookie(name, value, maxAge, path, domain, secure, httpOnly, sameSite);
  }

  private static SameSite parseSameSite(String value) {
    if (value.equalsIgnoreCase("strict")) {
      return SameSite.STRICT;
    } else if (value.equalsIgnoreCase("lax")) {
      return SameSite.LAX;
    } else if (value.equalsIgnoreCase("none")) {
      return SameSite.NONE;
    }
    return null; // Default to null if not specified or invalid
  }
}

/** A sample controller that proxies incoming requests to another server. */
public class Proxy extends Controller {
  private static final List<String> HOP_BY_HOP_HEADERS =
      Arrays.asList("content-encoding", "content-length", "transfer-encoding", "connection");

  public Result proxy0(Http.Request request) {
    return this.proxy(request, "");
  }

  public Result proxy(Http.Request request, String extraPath) {
    System.out.println("proxy: " + request.method() + " " + request.uri());

    final String path = request.uri();
    StringBuilder targetUrlBuilder = new StringBuilder();
    targetUrlBuilder.append("http://localhost:8000");
    if (!path.startsWith("/")) {
      targetUrlBuilder.append("/");
    }
    targetUrlBuilder.append(path);

    String targetUrl = targetUrlBuilder.toString();
    RequestConfig requestConfig = RequestConfig.custom().setRedirectsEnabled(false).build();

    try (CloseableHttpClient httpClient =
        HttpClients.custom().setDefaultRequestConfig(requestConfig).build()) {
      HttpUriRequestBase proxiedRequest;

      // Determine HTTP method
      if ("POST".equalsIgnoreCase(request.method())) {
        proxiedRequest = new HttpPost(targetUrl);
      } else if ("GET".equalsIgnoreCase(request.method())) {
        proxiedRequest = new HttpGet(targetUrl);
      } else if ("PUT".equalsIgnoreCase(request.method())) {
        proxiedRequest = new HttpPut(targetUrl);
      } else if ("PATCH".equalsIgnoreCase(request.method())) {
        proxiedRequest = new HttpPatch(targetUrl);
      } else if ("DELETE".equalsIgnoreCase(request.method())) {
        proxiedRequest = new HttpDelete(targetUrl);
      } else {
        throw new RuntimeException("Unknown method");
      }

      for (Map.Entry<String, List<String>> entry : request.getHeaders().asMap().entrySet()) {
        String headerName = entry.getKey();
        for (String headerValue : entry.getValue()) {
          if (HOP_BY_HOP_HEADERS.stream().anyMatch(h -> h.equalsIgnoreCase(headerName))) {
            continue;
          }
          proxiedRequest.addHeader(headerName, headerValue);
        }
      }

      // Handle request body if present
      ByteString body = request.body().asBytes();
      if (body != null && body.size() > 0) {
        HttpEntity entity = new ByteArrayEntity(body.toArray(), null);
        if (proxiedRequest instanceof HttpPost) {
          ((HttpPost) proxiedRequest).setEntity(entity);
        } else if (proxiedRequest instanceof HttpPut) {
          ((HttpPut) proxiedRequest).setEntity(entity);
        } else if (proxiedRequest instanceof HttpPatch) {
          ((HttpPatch) proxiedRequest).setEntity(entity);
        }
      }

      try (CloseableHttpResponse proxiedResponse = httpClient.execute(proxiedRequest)) {
        HttpEntity entity = proxiedResponse.getEntity();
        byte[] responseBody = entity != null ? EntityUtils.toByteArray(entity) : new byte[0];

        int statusCode = proxiedResponse.getCode();
        Result result = Results.status(statusCode, responseBody);

        List<Cookie> cookies = new ArrayList<>();

        for (Header header : proxiedResponse.getHeaders()) {
          String headerName = header.getName();
          if (HOP_BY_HOP_HEADERS.stream().anyMatch(h -> h.equalsIgnoreCase(headerName))) {
            continue;
          }
          final String headerValue = header.getValue();
          if ("content-type".equalsIgnoreCase(headerName)) {
            result = result.as(headerValue);
          } else if ("set-cookie".equalsIgnoreCase(headerName)) {
            cookies.add(CookieParser.parseSetCookieHeader(headerValue));
          } else {
            result = result.withHeader(headerName, headerValue);
          }
        }

        if (!cookies.isEmpty()) {
          result = result.withCookies(cookies.toArray(new Cookie[0]));
        }
        return result;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return internalServerError("Proxy error: " + e.getMessage());
    }
  }
}
