package ai.unlearn.dlq.core.service.impl;

import ai.unlearn.dlq.core.client.ArtemisManagementClient;
import ai.unlearn.dlq.core.exception.DlqOperationException;
import ai.unlearn.dlq.core.mapper.DlqMessageMapper;
import ai.unlearn.dlq.core.model.BatchOperationResult;
import ai.unlearn.dlq.core.model.DlqMessage;
import ai.unlearn.dlq.core.service.DlqBrowserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.artemis.api.core.management.ResourceNames;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DlqBrowserServiceImpl implements DlqBrowserService {

    private final ArtemisManagementClient client;
    private final ObjectMapper objectMapper;

    public DlqBrowserServiceImpl(ArtemisManagementClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<DlqMessage> browseMessages(String queueName, String filter) {
        Object result = client.invokeOperation(resourceName(queueName), "listMessagesAsJSON", nullToEmpty(filter));
        String json = result == null ? "[]" : result.toString();
        try {
            JsonNode root = objectMapper.readTree(json);
            List<DlqMessage> messages = new ArrayList<>();
            if (root.isArray()) {
                root.forEach(node -> messages.add(DlqMessageMapper.fromJson(node)));
            }
            return messages;
        } catch (Exception e) {
            throw new DlqOperationException("Failed to parse messages returned for queue '" + queueName + "'", e);
        }
    }

    @Override
    public long countMessages(String queueName, String filter) {
        Object result = client.invokeOperation(resourceName(queueName), "countMessages", nullToEmpty(filter));
        return result == null ? 0L : ((Number) result).longValue();
    }

    @Override
    public BatchOperationResult deleteMessages(String queueName, List<Long> messageIds) {
        String resource = resourceName(queueName);
        List<Long> succeeded = new ArrayList<>();
        Map<Long, String> failed = new LinkedHashMap<>();

        for (Long messageId : messageIds) {
            try {
                Object result = client.invokeOperation(resource, "removeMessage", messageId);
                if (Boolean.TRUE.equals(result)) {
                    succeeded.add(messageId);
                } else {
                    failed.put(messageId, "Message not found on queue (already consumed or removed)");
                }
            } catch (DlqOperationException e) {
                failed.put(messageId, e.getMessage());
            }
        }

        return new BatchOperationResult(succeeded, failed);
    }

    @Override
    public BatchOperationResult moveMessages(String queueName, List<Long> messageIds, String targetQueueName) {
        String resource = resourceName(queueName);
        List<Long> succeeded = new ArrayList<>();
        Map<Long, String> failed = new LinkedHashMap<>();

        for (Long messageId : messageIds) {
            try {
                Object result = client.invokeOperation(resource, "moveMessage", messageId, targetQueueName, false);
                if (Boolean.TRUE.equals(result)) {
                    succeeded.add(messageId);
                } else {
                    failed.put(messageId, "Message not found on queue (already consumed or removed)");
                }
            } catch (DlqOperationException e) {
                failed.put(messageId, e.getMessage());
            }
        }

        return new BatchOperationResult(succeeded, failed);
    }

    private String resourceName(String queueName) {
        return ResourceNames.QUEUE + queueName;
    }

    private String nullToEmpty(String filter) {
        return filter == null ? "" : filter;
    }
}
