package ai.unlearn.dlq.rest.controller;

import ai.unlearn.dlq.core.model.BatchOperationResult;
import ai.unlearn.dlq.core.model.DlqMessage;
import ai.unlearn.dlq.core.service.DlqBrowserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DlqController.class)
class DlqControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DlqBrowserService dlqBrowserService;

    @Test
    void browseMessagesReturnsMessagesFromService() throws Exception {
        DlqMessage message = new DlqMessage();
        message.setMessageId(1L);
        message.setAddress("DLQ");
        when(dlqBrowserService.browseMessages(eq("DLQ"), anyString())).thenReturn(List.of(message));

        mockMvc.perform(get("/api/dlq/queues/DLQ/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageId").value(1))
                .andExpect(jsonPath("$[0].address").value("DLQ"));
    }

    @Test
    void deleteMessagesRejectsEmptyIdList() throws Exception {
        mockMvc.perform(delete("/api/dlq/queues/DLQ/messages")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("messageIds", List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void moveMessagesDelegatesToService() throws Exception {
        when(dlqBrowserService.moveMessages(eq("DLQ"), any(), eq("orders.in")))
                .thenReturn(new BatchOperationResult(List.of(1L), Map.of()));

        mockMvc.perform(post("/api/dlq/queues/DLQ/messages/move")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "messageIds", List.of(1L),
                                "targetQueue", "orders.in"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.succeeded[0]").value(1));
    }
}
