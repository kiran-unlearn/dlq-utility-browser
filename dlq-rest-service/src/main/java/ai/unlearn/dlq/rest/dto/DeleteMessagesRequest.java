package ai.unlearn.dlq.rest.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class DeleteMessagesRequest {

    @NotEmpty(message = "messageIds must not be empty")
    private List<Long> messageIds;

    public List<Long> getMessageIds() {
        return messageIds;
    }

    public void setMessageIds(List<Long> messageIds) {
        this.messageIds = messageIds;
    }
}
