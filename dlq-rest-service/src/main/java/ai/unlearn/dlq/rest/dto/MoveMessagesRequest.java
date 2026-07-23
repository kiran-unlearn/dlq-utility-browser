package ai.unlearn.dlq.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class MoveMessagesRequest {

    @NotEmpty(message = "messageIds must not be empty")
    private List<Long> messageIds;

    @NotBlank(message = "targetQueue must not be blank")
    private String targetQueue;

    public List<Long> getMessageIds() {
        return messageIds;
    }

    public void setMessageIds(List<Long> messageIds) {
        this.messageIds = messageIds;
    }

    public String getTargetQueue() {
        return targetQueue;
    }

    public void setTargetQueue(String targetQueue) {
        this.targetQueue = targetQueue;
    }
}
