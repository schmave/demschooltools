package controllers;

import java.util.Arrays;
import java.util.Enumeration;
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
import play.libs.Json;
import play.mvc.*;

/** A sample controller that proxies incoming requests to another server. */
public class Proxy extends Controller {

  // Define the list of hop-by-hop headers that we do not forward in the response.
  private static final List<String> EXCLUDED_RESPONSE_HEADERS =
      Arrays.asList("content-encoding", "content-length", "transfer-encoding", "connection");

  /**
   * This action proxies any request with an optional path.
   *
   * <p>For example, a request to /proxy/some/path will be forwarded to {API_HOST}/some/path.
   *
   * @param path the dynamic path portion (may be empty)
   * @return a Result that forwards the response from the target server
   */
  public Result proxy(Http.Request request) {
    final String targetUrl = request.uri().replace(request.host(), "localhost:8000");

    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

      // Build the proxied request using the same HTTP method.
      RequestBuilder reqBuilder = RequestBuilder.create(request().method()).setUri(targetUrl);

      // Copy all headers from the original request except the "Host" header.
      for (Map.Entry<String, String[]> entry : request.headers().toMap().entrySet()) {
        String headerName = entry.getKey();
        for (String headerValue : entry.getValue()) {
          reqBuilder.addHeader(headerName, headerValue);
        }
      }

      // If the original request has a body (e.g. for POST or PUT), set it on the new request.
      // Play’s request().body().asBytes() returns a byte array (or null if none).
      byte[] body = request().body().asBytes();
      if (body != null && body.length > 0) {
        reqBuilder.setEntity(new ByteArrayEntity(body));
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
        // Note: If the same header occurs more than once, you may need to handle that accordingly.
        result = result.withHeader(headerName, header.getValue());
      }
      return result;

    } catch (Exception e) {
      return internalServerError("Proxy error: " + e.getMessage());
    }
  }
}
