package ai.unlearn.dlq.core.model;

import java.util.List;
import java.util.Map;

/**
 * Outcome of a batch operation (delete or move) applied to a set of message IDs. Partial failure
 * is expected and normal: a message might already have been consumed or re-delivered by the time
 * a browser user acts on it, so each ID is reported individually rather than failing the whole call.
 */
public class BatchOperationResult {

    private final List<Long> succeeded;
    private final Map<Long, String> failed;

    public BatchOperationResult(List<Long> succeeded, Map<Long, String> failed) {
        this.succeeded = succeeded;
        this.failed = failed;
    }

    public List<Long> getSucceeded() {
        return succeeded;
    }

    public Map<Long, String> getFailed() {
        return failed;
    }
}
