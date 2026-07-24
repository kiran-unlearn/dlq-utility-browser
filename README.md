# DLQ Utility Browser

A REST utility for browsing, deleting, and moving messages on AMQ7 (Artemis) dead letter queues.

## Modules

- **dlq-core** — Talks to the broker. Connects over the Artemis core protocol and drives the
  broker's management API (`QueueControl` operations: `listMessagesAsJSON`, `countMessages`,
  `removeMessage`, `moveMessage`) via `ManagementHelper` request/reply messages sent to the
  `activemq.management` address. This avoids needing a JMX/RMI port open — only the normal core
  protocol port (e.g. `61616`) is required. Ships as a Spring Boot auto-configuration: any app
  that depends on it gets a `DlqBrowserService` bean for free once `dlq.artemis.*` properties are set.
- **dlq-rest-service** — Spring Boot app exposing `dlq-core` over REST. This is what a browser
  (or any HTTP client) talks to.

A future browser UI module would call `dlq-rest-service`.

## Configuration

Set these in `dlq-rest-service/src/main/resources/application.yml` (or via environment
variables / a config server):

```yaml
dlq:
  artemis:
    broker-url: tcp://localhost:61616
    username: admin
    password: admin
    management-address: activemq.management
    connect-timeout-ms: 5000
    request-timeout-ms: 5000
```

### TLS (SSL) with JKS keystore/truststore

`ArtemisManagementClient` connects using an explicit `TransportConfiguration` built from
`org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants` — the same param names
Artemis's own Netty connector expects — rather than a URL query string, so there's no risk of
password/path characters breaking URL parsing. To enable TLS, set `dlq.artemis.ssl.*`:

```yaml
dlq:
  artemis:
    broker-url: tcp://localhost:61617   # point at the broker's SSL acceptor, not the plain one
    ssl:
      enabled: true
      trust-store-path: /path/to/client-truststore.jks   # verifies the broker's certificate
      trust-store-password: ${DLQ_ARTEMIS_TRUSTSTORE_PASSWORD}
      trust-store-type: JKS                                # default
      key-store-path: /path/to/client-keystore.jks         # only needed for mutual TLS
      key-store-password: ${DLQ_ARTEMIS_KEYSTORE_PASSWORD}  # (broker acceptor has needClientAuth=true)
      key-store-type: JKS                                  # default
      verify-host: true                                    # default; checks the broker cert's hostname
```

`key-store-*` is the client's own identity and is only required if the broker's acceptor requires
client certificates (`needClientAuth=true`, i.e. mutual TLS). For one-way TLS (client verifies the
broker but doesn't authenticate with a cert), omit `key-store-*` and just set `trust-store-*`.

This is client-side config only — the broker's acceptor also needs to be configured for SSL (its
own `keyStorePath`/`keyStorePassword`, and `trustStorePath`/`needClientAuth=true` if requiring
client certs) in `broker.xml`, e.g.:

```xml
<acceptor name="ssl-acceptor">tcp://0.0.0.0:61617?sslEnabled=true;keyStorePath=/path/to/broker-keystore.jks;keyStorePassword=...;trustStorePath=/path/to/broker-truststore.jks;trustStorePassword=...;needClientAuth=true</acceptor>
```

Verified end-to-end against a real broker: generated a broker keystore and a client keystore with
`keytool`, cross-imported each side's certificate into the other's truststore, added a
`needClientAuth=true` SSL acceptor to `broker.xml`, and confirmed `dlq-rest-service` connects and
browses successfully with `dlq.artemis.ssl.*` set — and, as a sanity check, that a client without
`key-store-*` set is correctly rejected by the broker's mutual-TLS requirement.

## Running

```bash
mvn install -DskipTests          # installs dlq-core to the local repo
cd dlq-rest-service && mvn spring-boot:run
```

(Running `spring-boot:run` directly from the reactor root with `-pl -am` doesn't work — Maven
applies the goal to every reactor project including the parent aggregator pom, which has no main
class. Install once, then run from inside `dlq-rest-service`.)

Swagger UI is available at `http://localhost:8080/swagger-ui.html` once running.

## Testing with Docker (no local JDK/Maven/Artemis needed)

`Dockerfile` at the repo root builds a fully self-contained image: JDK 17, Maven, a standalone
Apache Artemis broker (version matched to `artemis-core-client` in the parent `pom.xml`), and the
project source itself baked in and pre-built (`mvn install` runs at image-build time, including
tests, as a sanity check). No volume mount is needed — everything needed to build and run the app
already lives in the image.

Build once, then start the container. `start.sh` (the image's `CMD`) launches the Artemis broker
and `dlq-rest-service` together — no `docker exec` needed, the REST API is live as soon as the
container is:

```bash
docker build -t dlq-test-broker .
docker run -d --name dlq-test-broker -p 61616:61616 -p 8161:8161 -p 8080:8080 dlq-test-broker
```

(If port 8080 is already taken on your host, e.g. by something else you have running, map to a
different host port instead: `-p 18080:8080`.)

The broker comes up with a `DLQ` anycast queue/address already deployed (Artemis creates it by
default) — exactly what the example commands below expect — reachable at `tcp://localhost:61616`
with admin/admin, matching the defaults in `application.yml`. A web console is at
`http://localhost:8161/console`. `HEALTHCHECK` polls the REST API itself, so `docker ps`/`docker
inspect` reflect whether the whole stack (broker + REST service) is actually up, not just the
container process.

`start.sh` ties the two processes together: if either the broker or `dlq-rest-service` dies, the
other is killed and the container exits, instead of silently limping along with only half the
stack running.

From the host, hit it with curl or Postman at `http://localhost:8080` (or whatever host port you
mapped). Seed some test messages first, directly onto `DLQ`, using the broker's bundled producer
CLI (simplest way to get data to browse — no need to wire up real redelivery/dead-lettering):

```bash
docker exec dlq-test-broker /opt/artemis/bin/artemis producer \
  --url tcp://localhost:61616 --user admin --password admin \
  --destination queue://DLQ --message-count 5
```

Note `moveMessage` requires the target queue to already exist on the broker:

```bash
docker exec dlq-test-broker /opt/artemis/bin/artemis queue create --user admin --password admin \
  --url tcp://localhost:61616 --name <queue> --address <queue> --anycast --auto-create-address
```

If you change source and want the image to pick it up, `docker build` again — the poms-then-source
layering means only the module you touched gets rebuilt, not a full re-download of dependencies.

`docker exec -it dlq-test-broker bash` is still there if you want a shell in the running container
(e.g. to run `mvn test` ad hoc, or restart just `dlq-rest-service` after copying in a change without
rebuilding the image — copy with `docker cp <local-dir>/. dlq-test-broker:/workspace/<module>/src`,
note the trailing `/.` so it overwrites in place instead of nesting).

## API

| Method | Path                                              | Description                          |
|--------|----------------------------------------------------|---------------------------------------|
| GET    | `/api/dlq/queues`                                  | List/search queue names (optional `?search=`) |
| GET    | `/api/dlq/queues/{queueName}/messages`             | List messages (optional `?filter=`)   |
| GET    | `/api/dlq/queues/{queueName}/messages/count`       | Count messages (optional `?filter=`)  |
| DELETE | `/api/dlq/queues/{queueName}/messages`             | Delete selected messages by ID        |
| POST   | `/api/dlq/queues/{queueName}/messages/move`        | Move selected messages to another queue |

The `{queueName}`-scoped endpoints work against any queue on the broker, not just DLQs — e.g. a
target queue you `move` messages onto. `GET /api/dlq/queues` is meant for populating a queue
picker/typeahead in a UI, so a user doesn't need to already know a queue's exact name up front:
type a few characters, get matches, pick one, then call the `{queueName}` endpoints against it.
It excludes the broker's own internal per-connection management queues (`activemq.management.*`)
since those are never something a user would want to select. Results are sorted alphabetically
(case-insensitive).

`filter` is an Artemis core filter expression (SQL-92-like syntax over message properties).

### Examples

```bash
# List all queues (for a picker/typeahead)
curl http://localhost:8080/api/dlq/queues

# Search queues by substring, case-insensitive (e.g. as the user types "dlq")
curl "http://localhost:8080/api/dlq/queues?search=dlq"

# Browse
curl http://localhost:8080/api/dlq/queues/DLQ/messages

# Count
curl http://localhost:8080/api/dlq/queues/DLQ/messages/count

# Delete selected messages
curl -X DELETE http://localhost:8080/api/dlq/queues/DLQ/messages \
  -H "Content-Type: application/json" \
  -d '{"messageIds": [123, 456]}'

# Move selected messages back to the original queue
curl -X POST http://localhost:8080/api/dlq/queues/DLQ/messages/move \
  -H "Content-Type: application/json" \
  -d '{"messageIds": [123, 456], "targetQueue": "orders.in"}'
```

## Notes / follow-ups

- `artemis-core-client` version in the parent `pom.xml` should be matched to the Artemis version
  underlying your specific AMQ7 broker release.
- `ArtemisManagementClient` serializes all management calls through a single session (a
  reasonable trade-off for an admin tool). If this becomes a throughput bottleneck, replace it
  with a small session pool.
- Delete/move operations report partial success per message ID (`succeeded` / `failed` in the
  response), since a message may already have been consumed or redelivered between browsing and
  acting on it.
- No authentication/authorization is implemented yet on the REST layer itself — add Spring
  Security before exposing this outside a trusted network, since it can delete production
  messages.
