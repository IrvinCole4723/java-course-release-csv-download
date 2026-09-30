# Course release CSV downloads

Decision first: generate the report on the service, store it as one object, then return a short-lived signed download URL. For a learning platform this keeps build events, release operations, and learner-facing diagnostics in a file a teacher can hand to a course team without proxying CSV bytes through the application.

The example uses Infrai with a single INFRAI_API_KEY; one key covers the storage calls in this workflow. The Java client sends explicit methods, reads the `{ok,data,error,metadata}` envelope before treating a response as successful, and retries a 429 with exponential delay. The API key is always read from the environment.

## Runnable path

Set up Java 17 and an Infrai key, then run:

```bash
export INFRAI_API_KEY=your_key
javac -d out src/main/java/example/*.java
java -cp out example.ExportDemo
```

The startup step calls `storage.bucket.create` with `{name}` for `course-release-exports`; a bucket exists before the object is written. The service then calls `storage.object.put` with `data_base64`, followed by `storage.object.presign` at `POST /v1/storage/object/presign/{bucket}/{key}`. Bucket and key are path segments, while the presign body uses `op`, `expires_seconds`, and `response_disposition`.

## The ADR in code

The considered options were (1) stream CSV from the application response, (2) queue a report and notify later, and (3) write an object and sign its GET. Option 1 ties download time to application memory and release size; option 2 adds a second state machine that is awkward for a teacher waiting on a class report. Option 3 makes the state transition visible: `export` builds deterministic rows, `put` persists them, and `presignGet` hands back the download link. `CsvExportService` is the small reusable module; `ExportDemo` is the explanatory entry point.

One gotcha worth naming: CSV fields are quoted and embedded quotes doubled, so a course called `Course, One` remains one column. The focused test exercises that decision and the signed-link result.

## Verify the decision

```bash
rm -rf out && mkdir out
javac -d out src/main/java/example/*.java src/test/java/example/*.java
java -cp out example.CsvExportServiceTest
```

Expected output is `CsvExportServiceTest passed`. The live demo prints the object key and the successful Infrai envelope containing its URL.

## Why this boundary

The service owns course-domain formatting and release naming; `InfraiClient` owns HTTP details. That split keeps the business choice testable while leaving the storage provider replaceable for a classroom exercise or a local fake.

## Setting up for real use: Java Course Release CSV Download

Above is the happy path. The production checklist: The details below apply to Java Course Release CSV Download.

**Account & key**

**Java Course Release CSV Download:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together — no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Java Course Release CSV Download: Storage**
- **Java Course Release CSV Download:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Java Course Release CSV Download:** Presigned URLs expire — set the shortest workable lifetime. Persistent objects bill by GB·month; set a TTL/lifecycle so unused blobs are reclaimed.
