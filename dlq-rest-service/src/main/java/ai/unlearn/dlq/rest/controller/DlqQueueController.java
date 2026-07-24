package ai.unlearn.dlq.rest.controller;

import ai.unlearn.dlq.core.service.DlqBrowserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lists queues deployed on the broker, for populating a queue picker/typeahead in a UI so a user
 * doesn't need to already know a queue's exact name before browsing or acting on it.
 */
@RestController
@RequestMapping("/api/dlq/queues")
public class DlqQueueController {

    private final DlqBrowserService dlqBrowserService;

    public DlqQueueController(DlqBrowserService dlqBrowserService) {
        this.dlqBrowserService = dlqBrowserService;
    }

    @GetMapping
    public List<String> listQueueNames(@RequestParam(required = false, defaultValue = "") String search) {
        return dlqBrowserService.listQueueNames(search);
    }
}
