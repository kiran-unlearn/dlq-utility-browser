package ai.unlearn.dlq.core.service;

import ai.unlearn.dlq.core.client.ArtemisManagementClient;
import ai.unlearn.dlq.core.model.BatchOperationResult;
import ai.unlearn.dlq.core.model.DlqMessage;
import ai.unlearn.dlq.core.service.impl.DlqBrowserServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DlqBrowserServiceImplTest {

    @Mock
    private ArtemisManagementClient client;

    private DlqBrowserService service;

    @BeforeEach
    void setUp() {
        service = new DlqBrowserServiceImpl(client, new ObjectMapper());
    }

    @Test
    void browseMessagesParsesJsonFromBroker() {
        when(client.invokeOperation(eq("queue.DLQ"), eq("listMessagesAsJSON"), any()))
                .thenReturn("[{\"messageID\": 1, \"address\": \"DLQ\"}, {\"messageID\": 2, \"address\": \"DLQ\"}]");

        List<DlqMessage> messages = service.browseMessages("DLQ", null);

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getMessageId()).isEqualTo(1L);
        assertThat(messages.get(1).getMessageId()).isEqualTo(2L);
    }

    @Test
    void deleteMessagesReportsPartialFailure() {
        when(client.invokeOperation(eq("queue.DLQ"), eq("removeMessage"), eq(1L))).thenReturn(true);
        when(client.invokeOperation(eq("queue.DLQ"), eq("removeMessage"), eq(2L))).thenReturn(false);

        BatchOperationResult result = service.deleteMessages("DLQ", List.of(1L, 2L));

        assertThat(result.getSucceeded()).containsExactly(1L);
        assertThat(result.getFailed()).containsOnlyKeys(2L);
    }

    @Test
    void moveMessagesInvokesMoveMessageOperation() {
        when(client.invokeOperation(eq("queue.DLQ"), eq("moveMessage"), eq(1L), eq("orders.in"), eq(false)))
                .thenReturn(true);

        BatchOperationResult result = service.moveMessages("DLQ", List.of(1L), "orders.in");

        assertThat(result.getSucceeded()).containsExactly(1L);
        assertThat(result.getFailed()).isEmpty();
    }
}
