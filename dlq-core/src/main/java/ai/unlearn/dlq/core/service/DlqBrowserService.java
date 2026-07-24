package ai.unlearn.dlq.core.service;

import ai.unlearn.dlq.core.model.BatchOperationResult;
import ai.unlearn.dlq.core.model.DlqMessage;

import java.util.List;

/**
 * Browses and manages messages sitting on an AMQ7 (Artemis) dead letter queue.
 */
public interface DlqBrowserService {

    /**
     * Lists the names of queues deployed on the broker, for populating a queue picker/typeahead
     * so callers don't need to already know the exact queue name. Broker-internal management
     * queues (e.g. {@code activemq.management.*}) are excluded since they're never something a
     * user would want to browse or act on.
     *
     * @param search case-insensitive substring to filter queue names by, or {@code null}/empty for all queues
     * @return matching queue names, sorted alphabetically
     */
    List<String> listQueueNames(String search);

    /**
     * Lists messages currently on the given queue, without consuming them.
     *
     * @param queueName the DLQ to browse
     * @param filter    an Artemis core filter expression (SQL-92-like syntax), or {@code null}/empty for all messages
     */
    List<DlqMessage> browseMessages(String queueName, String filter);

    /**
     * Counts messages on the given queue matching an optional filter.
     */
    long countMessages(String queueName, String filter);

    /**
     * Permanently removes the given messages from the queue.
     */
    BatchOperationResult deleteMessages(String queueName, List<Long> messageIds);

    /**
     * Moves the given messages from the queue to another queue (e.g. back to the original
     * destination, or to a holding queue for manual inspection).
     */
    BatchOperationResult moveMessages(String queueName, List<Long> messageIds, String targetQueueName);
}
