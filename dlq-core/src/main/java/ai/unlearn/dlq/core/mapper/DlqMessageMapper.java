package ai.unlearn.dlq.core.mapper;

import ai.unlearn.dlq.core.model.DlqMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts the JSON produced by {@code QueueControl#listMessagesAsJSON} into {@link DlqMessage}
 * instances. Field extraction is defensive: an Artemis version that omits or renames a field
 * simply leaves the corresponding typed getter null rather than failing the whole browse.
 */
public final class DlqMessageMapper {

    private DlqMessageMapper() {
    }

    public static DlqMessage fromJson(JsonNode node) {
        DlqMessage message = new DlqMessage();
        message.setMessageId(longValue(node, "messageID", 0L));
        message.setAddress(textValue(node, "address"));
        message.setType(textValue(node, "type"));
        message.setDurable(boolValue(node, "durable"));
        message.setPriority((int) longValue(node, "priority", 0));
        message.setTimestamp(longValue(node, "timestamp", 0));
        message.setExpiration(longValue(node, "expiration", 0));
        message.setUserId(textValue(node, "userID"));
        message.setOriginalAddress(textValue(node, "_AMQ_ORIG_ADDRESS"));
        message.setOriginalQueue(textValue(node, "_AMQ_ORIG_QUEUE"));
        message.setOriginalMessageId(longOrNull(node, "_AMQ_ORIG_MESSAGE_ID"));
        message.setTextBodyPreview(textValue(node, "text"));

        Map<String, Object> raw = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> raw.put(entry.getKey(), jsonNodeToObject(entry.getValue())));
        message.setProperties(raw);

        return message;
    }

    public static Iterable<DlqMessage> fromJsonArray(JsonNode arrayNode) {
        if (arrayNode == null || !(arrayNode instanceof ArrayNode)) {
            return java.util.List.of();
        }
        java.util.List<DlqMessage> messages = new java.util.ArrayList<>();
        arrayNode.forEach(node -> messages.add(fromJson(node)));
        return messages;
    }

    private static String textValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static long longValue(JsonNode node, String field, long defaultValue) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? defaultValue : value.asLong(defaultValue);
    }

    private static Long longOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asLong();
    }

    private static boolean boolValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.asBoolean(false);
    }

    private static Object jsonNodeToObject(JsonNode value) {
        if (value.isNull()) {
            return null;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isIntegralNumber()) {
            return value.asLong();
        }
        if (value.isFloatingPointNumber()) {
            return value.asDouble();
        }
        return value.asText();
    }
}
