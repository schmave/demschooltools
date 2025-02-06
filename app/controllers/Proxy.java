package controllers;

import akka.util.ByteString;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
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

/** A sample controller that proxies incoming requests to another server. */
public class Proxy extends Controller {
  private static final List<String> HOP_BY_HOP_HEADERS =
      Arrays.asList("content-encoding", "content-length", "transfer-encoding", "connection");

  public Result proxy0(Http.Request request) {
    return this.proxy(request, "");
  }

  public Result proxy(Http.Request request, String extraPath) {
    System.out.println("proxy: " + request.uri());

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
      if (body != null && body.size() > 0 && proxiedRequest instanceof HttpPost) {
        HttpEntity entity = new ByteArrayEntity(body.toArray(), null);
        ((HttpPost) proxiedRequest).setEntity(entity);
      }

      try (CloseableHttpResponse proxiedResponse = httpClient.execute(proxiedRequest)) {
        HttpEntity entity = proxiedResponse.getEntity();
        byte[] responseBody = entity != null ? EntityUtils.toByteArray(entity) : new byte[0];

        int statusCode = proxiedResponse.getCode();
        Result result = Results.status(statusCode, responseBody);

        for (Header header : proxiedResponse.getHeaders()) {
          String headerName = header.getName();
          if (HOP_BY_HOP_HEADERS.stream().anyMatch(h -> h.equalsIgnoreCase(headerName))) {
            continue;
          }
          final String headerValue = header.getValue();
          if ("Set-Cookie".equalsIgnoreCase(headerName)) {
            System.out.println("response header: " + headerName + ", value: " + headerValue);
          }
          if ("content-type".equalsIgnoreCase(headerName)) {
            result = result.as(headerValue);
          } else {
            result = result.withHeader(headerName, headerValue);
          }
        }
        return result;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return internalServerError("Proxy error: " + e.getMessage());
    }
  }
}
