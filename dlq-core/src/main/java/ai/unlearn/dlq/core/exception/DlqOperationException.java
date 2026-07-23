package ai.unlearn.dlq.core.exception;

/**
 * Raised whenever a management call against the broker fails: connection problems,
 * a timed-out request/reply, or an operation the broker explicitly reports as failed.
 */
public class DlqOperationException extends RuntimeException {

    public DlqOperationException(String message) {
        super(message);
    }

    public DlqOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
