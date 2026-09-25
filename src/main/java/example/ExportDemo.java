package example;

import java.util.List;

public final class ExportDemo {
  public static void main(String[] args) throws Exception {
    String key = System.getenv("INFRAI_API_KEY");
    if (key == null || key.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY first");
    String bucket = "course-release-exports";
    InfraiClient client = new InfraiClient(key);
    client.createBucket(bucket);
    CsvExportService service = new CsvExportService(new CsvExportService.Storage() {
      public String put(String objectKey, String csv) throws java.io.IOException, InterruptedException { return client.putObject(bucket, objectKey, csv); }
      public String sign(String objectKey) throws java.io.IOException, InterruptedException { return client.presignGet(bucket, objectKey); }
    });
    var result = service.export("release-2026-09-11", List.of(
        new CsvExportService.BuildEvent("b-104", "Algebra I", "passed", "2026-09-11T08:30:00Z"),
        new CsvExportService.BuildEvent("b-105", "Physics Lab", "diagnostic", "2026-09-11T09:10:00Z")));
    System.out.println("CSV ready: " + result.objectKey());
    System.out.println("Download envelope: " + result.responseEnvelope());
  }
}
