package controllers;

import akka.util.ByteString;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import play.mvc.*;

/** A sample controller that proxies incoming requests to another server. */
public class Proxy extends Controller {

  // Define the list of hop-by-hop headers that we do not forward in the response.
  private static final List<String> EXCLUDED_RESPONSE_HEADERS =
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
    // targetUrlBuilder.append(request.queryString());

    String targetUrl = targetUrlBuilder.toString();

    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

      // Build the proxied request using the same HTTP method.
      RequestBuilder reqBuilder = RequestBuilder.create(request.method()).setUri(targetUrl);

      // Copy all headers from the original request except the "Host" header.
      for (Map.Entry<String, List<String>> entry : request.getHeaders().asMap().entrySet()) {
        String headerName = entry.getKey();
        for (String headerValue : entry.getValue()) {
          reqBuilder.addHeader(headerName, headerValue);
        }
      }

      // If the original request has a body (e.g. for POST or PUT), set it on the new request.
      // Play’s request().body().asBytes() returns a byte array (or null if none).
      ByteString body = request.body().asBytes();
      if (body != null && body.size() > 0) {
        reqBuilder.setEntity(new ByteArrayEntity(body.toArray()));
      }

      HttpUriRequest proxiedRequest = reqBuilder.build();
      HttpResponse proxiedResponse = httpClient.execute(proxiedRequest);

      // Read the response body as a byte array.
      HttpEntity entity = proxiedResponse.getEntity();
      byte[] responseBody = entity != null ? EntityUtils.toByteArray(entity) : new byte[0];

      // Build the Play response with the same status code.
      int statusCode = proxiedResponse.getStatusLine().getStatusCode();
      Result result = Results.status(statusCode, responseBody);

      // Copy all headers from the proxied response, excluding hop-by-hop headers.
      for (Header header : proxiedResponse.getAllHeaders()) {
        String headerName = header.getName();
        if (EXCLUDED_RESPONSE_HEADERS.stream().anyMatch(h -> h.equalsIgnoreCase(headerName))) {
          continue;
        }
        final String headerValue = header.getValue();
        if ("content-type".equalsIgnoreCase(headerName)) {
          // Instead of using withHeader, set the content type with .as()
          result = result.as(headerValue);
        } else {
          // Note: If the same header occurs more than once, you may need to handle that
          // accordingly.
          result = result.withHeader(headerName, headerValue);
        }
      }
      return result;

    } catch (Exception e) {
      e.printStackTrace();
      return internalServerError("Proxy error: " + e.getMessage());
    }
  }
}
