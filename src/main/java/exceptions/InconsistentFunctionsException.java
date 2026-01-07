package exceptions;

public class InconsistentFunctionsException extends RuntimeException {
    public InconsistentFunctionsException() {
        super();
    }

    public InconsistentFunctionsException(String message) {
        super(message);
    }

    public InconsistentFunctionsException(String message, Throwable cause) {
        super(message, cause);
    }

    public InconsistentFunctionsException(Throwable cause) {
        super(cause);
    }
}