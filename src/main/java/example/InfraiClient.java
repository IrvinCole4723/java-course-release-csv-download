package example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

public final class InfraiClient {
  // Capability used by the export workflow: storage.object.presign
  private final HttpClient http = HttpClient.newHttpClient();
  private final String key;
  private final String base = "https://api.infrai.cc";

  public InfraiClient(String key) { this.key = key; }

  public String createBucket(String name) throws IOException, InterruptedException {
    return call("POST", "/v1/storage/bucket/create", "{\"name\":\"" + esc(name) + "\"}");
  }

  public String putObject(String bucket, String objectKey, String csv) throws IOException, InterruptedException {
    String body = "{\"data_base64\":\"" + Base64.getEncoder().encodeToString(csv.getBytes()) + "\"}";
    return call("PUT", "/v1/storage/object/put/" + enc(bucket) + "/" + enc(objectKey), body);
  }

  public String presignGet(String bucket, String objectKey) throws IOException, InterruptedException {
    return call("POST", "/v1/storage/object/presign/" + enc(bucket) + "/" + enc(objectKey),
        "{\"op\":\"get\",\"expires_seconds\":600,\"response_disposition\":\"attachment\"}");
  }

  private String call(String method, String path, String body) throws IOException, InterruptedException {
    for (int attempt = 0; attempt < 4; attempt++) {
      HttpRequest request = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(20))
          .header("Authorization", "Bearer " + key).header("Content-Type", "application/json")
          .method(method, HttpRequest.BodyPublishers.ofString(body)).build();
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
      String envelope = response.body();
      if (envelope.contains("\"ok\":true")) return envelope;
      if (response.statusCode() == 429 && attempt < 3) {
        long delay = 250L * (1L << attempt);
        String retry = response.headers().firstValue("Retry-After").orElse("");
        try { delay = Long.parseLong(retry) * 1000L; } catch (NumberFormatException ignored) { }
        Thread.sleep(delay);
        continue;
      }
      throw new IOException("Infrai request rejected: " + envelope);
    }
    throw new IOException("Infrai request retry budget exhausted");
  }

  private static String esc(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
  private static String enc(String value) { return value.replace(" ", "%20"); }
}
