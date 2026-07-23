package ai.unlearn.dlq.core.client;

import ai.unlearn.dlq.core.config.ArtemisConnectionProperties;
import ai.unlearn.dlq.core.exception.DlqOperationException;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientRequestor;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.api.core.management.ManagementHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thin wrapper around the Artemis core client's management request/reply channel.
 *
 * <p>Rather than opening a JMX/RMI port, this sends management messages to the broker's
 * management address (see {@link ArtemisConnectionProperties#getManagementAddress()}) over the
 * same core protocol connection used for everything else. The broker dispatches each message to
 * the resource's {@code QueueControl} MBean and replies with the result.
 *
 * <p>A single {@link ClientSession}/{@link ClientRequestor} pair is shared across all calls and
 * is not safe for concurrent use, so requests are serialized with a lock. That is a reasonable
 * trade-off for an admin/browsing tool; a session pool could replace this if throughput ever
 * becomes a concern.
 */
public class ArtemisManagementClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ArtemisManagementClient.class);

    private final ArtemisConnectionProperties properties;
    private final Object lock = new Object();

    private ServerLocator locator;
    private ClientSessionFactory sessionFactory;
    private ClientSession session;
    private ClientRequestor requestor;

    public ArtemisManagementClient(ArtemisConnectionProperties properties) {
        this.properties = properties;
    }

    public void start() {
        synchronized (lock) {
            try {
                locator = ActiveMQClient.createServerLocator(properties.getBrokerUrl());
                locator.setCallTimeout(properties.getRequestTimeoutMs());
                sessionFactory = locator.createSessionFactory();
                session = hasCredentials()
                        ? sessionFactory.createSession(properties.getUsername(), properties.getPassword(),
                                false, true, true, false, 0)
                        : sessionFactory.createSession();
                requestor = new ClientRequestor(session, properties.getManagementAddress());
                session.start();
                log.info("Connected to Artemis broker at {}", properties.getBrokerUrl());
            } catch (Exception e) {
                throw new DlqOperationException(
                        "Failed to connect to Artemis broker at " + properties.getBrokerUrl(), e);
            }
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            closeQuietly(requestor);
            closeQuietly(session);
            closeQuietly(sessionFactory);
            if (locator != null) {
                locator.close();
            }
        }
    }

    /**
     * Invokes a management operation on a broker resource (e.g. {@code queue.MY_DLQ}) and
     * returns the deserialized result, or {@code null} if the operation has no return value.
     */
    public Object invokeOperation(String resourceName, String operationName, Object... params) {
        synchronized (lock) {
            try {
                ClientMessage message = session.createMessage(false);
                ManagementHelper.putOperationInvocation(message, resourceName, operationName, params);
                ClientMessage reply = requestor.request(message, properties.getRequestTimeoutMs());
                if (reply == null) {
                    throw new DlqOperationException("Timed out waiting for management reply for operation '"
                            + operationName + "' on resource '" + resourceName + "'");
                }
                if (!ManagementHelper.hasOperationSucceeded(reply)) {
                    Object failure = ManagementHelper.getResult(reply);
                    throw new DlqOperationException("Management operation '" + operationName + "' on resource '"
                            + resourceName + "' failed: " + failure);
                }
                return ManagementHelper.getResult(reply);
            } catch (DlqOperationException e) {
                throw e;
            } catch (ActiveMQException | RuntimeException e) {
                throw new DlqOperationException("Error invoking management operation '" + operationName
                        + "' on resource '" + resourceName + "'", e);
            }
        }
    }

    private boolean hasCredentials() {
        return properties.getUsername() != null && !properties.getUsername().isBlank();
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception e) {
            log.warn("Error closing {}", closeable.getClass().getSimpleName(), e);
        }
    }
}
