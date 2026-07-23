package ai.unlearn.dlq.core.model;

import java.util.Map;

/**
 * A message sitting on a DLQ, as reported by the broker's {@code listMessagesAsJSON} operation.
 *
 * <p>The well-known fields below are extracted defensively since the exact JSON shape can vary
 * slightly across Artemis versions; {@link #getProperties()} always holds the full raw payload
 * so nothing is lost even if a field isn't promoted to a typed getter here.
 */
public class DlqMessage {

    private long messageId;
    private String address;
    private String type;
    private boolean durable;
    private int priority;
    private long timestamp;
    private long expiration;
    private String userId;

    /** Address the message originally lived on before being dead-lettered ({@code _AMQ_ORIG_ADDRESS}). */
    private String originalAddress;

    /** Queue the message originally lived on before being dead-lettered ({@code _AMQ_ORIG_QUEUE}). */
    private String originalQueue;

    /** Message ID assigned before being dead-lettered ({@code _AMQ_ORIG_MESSAGE_ID}). */
    private Long originalMessageId;

    /** Best-effort text preview of the body, if the broker reported one. */
    private String textBodyPreview;

    /** Full set of fields returned by the broker for this message. */
    private Map<String, Object> properties;

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isDurable() {
        return durable;
    }

    public void setDurable(boolean durable) {
        this.durable = durable;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOriginalAddress() {
        return originalAddress;
    }

    public void setOriginalAddress(String originalAddress) {
        this.originalAddress = originalAddress;
    }

    public String getOriginalQueue() {
        return originalQueue;
    }

    public void setOriginalQueue(String originalQueue) {
        this.originalQueue = originalQueue;
    }

    public Long getOriginalMessageId() {
        return originalMessageId;
    }

    public void setOriginalMessageId(Long originalMessageId) {
        this.originalMessageId = originalMessageId;
    }

    public String getTextBodyPreview() {
        return textBodyPreview;
    }

    public void setTextBodyPreview(String textBodyPreview) {
        this.textBodyPreview = textBodyPreview;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
