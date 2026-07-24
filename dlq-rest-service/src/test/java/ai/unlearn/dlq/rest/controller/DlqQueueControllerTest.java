package ai.unlearn.dlq.rest.controller;

import ai.unlearn.dlq.core.service.DlqBrowserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DlqQueueController.class)
class DlqQueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DlqBrowserService dlqBrowserService;

    @Test
    void listQueueNamesReturnsAllQueuesWhenNoSearchGiven() throws Exception {
        when(dlqBrowserService.listQueueNames(eq(""))).thenReturn(List.of("DLQ", "ExpiryQueue", "orders.in"));

        mockMvc.perform(get("/api/dlq/queues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("DLQ"))
                .andExpect(jsonPath("$[1]").value("ExpiryQueue"))
                .andExpect(jsonPath("$[2]").value("orders.in"));
    }

    @Test
    void listQueueNamesPassesSearchParamThrough() throws Exception {
        when(dlqBrowserService.listQueueNames(eq("dlq"))).thenReturn(List.of("DLQ"));

        mockMvc.perform(get("/api/dlq/queues").param("search", "dlq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("DLQ"))
                .andExpect(jsonPath("$.length()").value(1));
    }
}
