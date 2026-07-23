package ai.unlearn.dlq.rest.controller;

import ai.unlearn.dlq.core.model.BatchOperationResult;
import ai.unlearn.dlq.core.model.DlqMessage;
import ai.unlearn.dlq.core.service.DlqBrowserService;
import ai.unlearn.dlq.rest.dto.CountResponse;
import ai.unlearn.dlq.rest.dto.DeleteMessagesRequest;
import ai.unlearn.dlq.rest.dto.MoveMessagesRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dlq/queues/{queueName}/messages")
public class DlqController {

    private final DlqBrowserService dlqBrowserService;

    public DlqController(DlqBrowserService dlqBrowserService) {
        this.dlqBrowserService = dlqBrowserService;
    }

    @GetMapping
    public List<DlqMessage> browseMessages(@PathVariable String queueName,
                                            @RequestParam(required = false, defaultValue = "") String filter) {
        return dlqBrowserService.browseMessages(queueName, filter);
    }

    @GetMapping("/count")
    public CountResponse countMessages(@PathVariable String queueName,
                                        @RequestParam(required = false, defaultValue = "") String filter) {
        return new CountResponse(dlqBrowserService.countMessages(queueName, filter));
    }

    @DeleteMapping
    public BatchOperationResult deleteMessages(@PathVariable String queueName,
                                                @Valid @RequestBody DeleteMessagesRequest request) {
        return dlqBrowserService.deleteMessages(queueName, request.getMessageIds());
    }

    @PostMapping("/move")
    public BatchOperationResult moveMessages(@PathVariable String queueName,
                                              @Valid @RequestBody MoveMessagesRequest request) {
        return dlqBrowserService.moveMessages(queueName, request.getMessageIds(), request.getTargetQueue());
    }
}
