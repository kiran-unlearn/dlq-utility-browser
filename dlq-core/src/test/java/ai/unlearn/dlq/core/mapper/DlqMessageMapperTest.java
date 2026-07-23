package ai.unlearn.dlq.core.mapper;

import ai.unlearn.dlq.core.model.DlqMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DlqMessageMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsKnownFieldsAndRetainsRawProperties() throws Exception {
        String json = """
                {
                  "messageID": 42,
                  "address": "DLQ",
                  "type": 3,
                  "durable": true,
                  "priority": 4,
                  "timestamp": 1700000000000,
                  "expiration": 0,
                  "userID": "ID:abc-123",
                  "_AMQ_ORIG_ADDRESS": "orders.in",
                  "_AMQ_ORIG_QUEUE": "orders.in",
                  "_AMQ_ORIG_MESSAGE_ID": 17,
                  "text": "hello world"
                }
                """;
        JsonNode node = objectMapper.readTree(json);

        DlqMessage message = DlqMessageMapper.fromJson(node);

        assertThat(message.getMessageId()).isEqualTo(42L);
        assertThat(message.getAddress()).isEqualTo("DLQ");
        assertThat(message.isDurable()).isTrue();
        assertThat(message.getPriority()).isEqualTo(4);
        assertThat(message.getOriginalAddress()).isEqualTo("orders.in");
        assertThat(message.getOriginalQueue()).isEqualTo("orders.in");
        assertThat(message.getOriginalMessageId()).isEqualTo(17L);
        assertThat(message.getTextBodyPreview()).isEqualTo("hello world");
        assertThat(message.getProperties()).containsEntry("address", "DLQ");
    }

    @Test
    void toleratesMissingFields() throws Exception {
        JsonNode node = objectMapper.readTree("{\"messageID\": 1}");

        DlqMessage message = DlqMessageMapper.fromJson(node);

        assertThat(message.getMessageId()).isEqualTo(1L);
        assertThat(message.getOriginalAddress()).isNull();
        assertThat(message.getOriginalMessageId()).isNull();
    }
}
