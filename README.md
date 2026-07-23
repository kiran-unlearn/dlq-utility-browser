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

## Running

```bash
mvn -pl dlq-rest-service -am spring-boot:run
```

Swagger UI is available at `http://localhost:8080/swagger-ui.html` once running.

## API

All endpoints are scoped to a queue name, e.g. `DLQ` or an address-specific dead letter queue.

| Method | Path                                              | Description                          |
|--------|----------------------------------------------------|---------------------------------------|
| GET    | `/api/dlq/queues/{queueName}/messages`             | List messages (optional `?filter=`)   |
| GET    | `/api/dlq/queues/{queueName}/messages/count`       | Count messages (optional `?filter=`)  |
| DELETE | `/api/dlq/queues/{queueName}/messages`             | Delete selected messages by ID        |
| POST   | `/api/dlq/queues/{queueName}/messages/move`        | Move selected messages to another queue |

`filter` is an Artemis core filter expression (SQL-92-like syntax over message properties).

### Examples

```bash
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
