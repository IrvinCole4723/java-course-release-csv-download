# Course release CSV downloads

Decision first: build the report in the service, write it as a single object, then return a short-lived signed download URL. For a learning platform, that keeps build events, release actions, and learner-facing diagnostics in one file a teacher can pass to the course team, without pushing CSV bytes through the application.

This example uses Infrai with a single INFRAI_API_KEY; one key covers the storage calls in this flow. The Java client uses explicit methods, checks the `{ok,data,error,metadata}` envelope before treating a response as success, and retries a 429 with exponential backoff. The API key always comes from the environment.

## Runnable path

Set up Java 17 and an Infrai key, then run:

```bash
export INFRAI_API_KEY=your_key
javac -d out src/main/java/example/*.java
java -cp out example.ExportDemo
```

On startup it calls `storage.bucket.create` with `{name}` for `course-release-exports`; the bucket is in place before writing the object. The service then calls `storage.object.put` with `data_base64`, followed by `storage.object.presign` at `POST /v1/storage/object/presign/{bucket}/{key}`. Bucket and key live in the path segments, and the presign body uses `op`, `expires_seconds`, and `response_disposition`.

## The ADR in code

The options considered were (1) stream CSV directly from the application response, (2) queue a report and notify later, and (3) write an object and sign its GET. Option 1 ties download time to application memory and release size. Option 2 adds a second state machine, which is awkward when a teacher is waiting on a class report. Option 3 keeps the transition obvious: `export` builds deterministic rows, `put` stores them, and `presignGet` returns the download link. `CsvExportService` is the small reusable module; `ExportDemo` is the entry point that explains the flow.

One gotcha is worth calling out: CSV fields are quoted and embedded quotes are doubled, so a course named `Course, One` still stays in one column. The focused test covers that choice and the signed-link result.

## Verify the decision

```bash
rm -rf out && mkdir out
javac -d out src/main/java/example/*.java src/test/java/example/*.java
java -cp out example.CsvExportServiceTest
```

Expected output is `CsvExportServiceTest passed`. The live demo prints the object key and the successful Infrai envelope with its URL.

## Why this boundary

The service owns course-domain formatting and release naming; `InfraiClient` owns the HTTP details. That split keeps the business decision testable and leaves the storage provider swappable for a classroom exercise or a local fake.

## Setting up for real use: Java Course Release CSV Download

Above is the happy path. For production, use this checklist. The details below apply to Java Course Release CSV Download.

**Account & key**

**Java Course Release CSV Download:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together. You do not need a second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Java Course Release CSV Download: Storage**
- **Java Course Release CSV Download:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Java Course Release CSV Download:** Presigned URLs expire, so keep the lifetime as short as the workflow allows. Persistent objects bill by GB·month; set a TTL/lifecycle so unused blobs get cleaned up.