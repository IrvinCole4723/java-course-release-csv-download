package example;

import java.io.IOException;
import java.util.List;

public final class CsvExportService {
  public record BuildEvent(String buildId, String course, String status, String occurredAt) {}
  public record Download(String objectKey, String responseEnvelope) {}
  interface Storage { String put(String key, String csv) throws IOException, InterruptedException; String sign(String key) throws IOException, InterruptedException; }
  private final Storage storage;

  public CsvExportService(Storage storage) { this.storage = storage; }

  public Download export(String releaseId, List<BuildEvent> events) throws IOException, InterruptedException {
    String key = "exports/" + releaseId + ".csv";
    StringBuilder csv = new StringBuilder("build_id,course,status,occurred_at\n");
    for (BuildEvent e : events) csv.append(row(e.buildId())).append(',').append(row(e.course())).append(',')
        .append(row(e.status())).append(',').append(row(e.occurredAt())).append('\n');
    storage.put(key, csv.toString());
    return new Download(key, storage.sign(key));
  }

  private static String row(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
}
