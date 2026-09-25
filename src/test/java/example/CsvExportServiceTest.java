package example;

import java.util.ArrayList;
import java.util.List;

public final class CsvExportServiceTest {
  public static void main(String[] args) throws Exception {
    List<String> writes = new ArrayList<>();
    CsvExportService service = new CsvExportService(new CsvExportService.Storage() {
      public String put(String key, String csv) { writes.add(key + "\n" + csv); return "ok"; }
      public String sign(String key) { return "{\"ok\":true,\"data\":{\"url\":\"https://download.example/" + key + "\"}}"; }
    });
    var result = service.export("r1", List.of(new CsvExportService.BuildEvent("b1", "Course, One", "passed", "now")));
    if (!result.objectKey().equals("exports/r1.csv") || !writes.get(0).contains("\"Course, One\"") || !result.responseEnvelope().contains("\"ok\":true"))
      throw new AssertionError("export decision did not produce a signed CSV");
    System.out.println("CsvExportServiceTest passed");
  }
}
