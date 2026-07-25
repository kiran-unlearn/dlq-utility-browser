package ai.unlearn.dlq.rest.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the web UI's HTML shell. Deliberately a plain Thymeleaf-rendered view (rather than
 * relying on Spring Boot's implicit static/index.html welcome-page resolution) so it keeps working
 * even in environments that layer custom request handling on top of Spring Boot and don't
 * preserve every default auto-configuration behavior.
 */
@Controller
public class UiController {

    @GetMapping("/")
    public String index() {
        return "index";
    }
}
