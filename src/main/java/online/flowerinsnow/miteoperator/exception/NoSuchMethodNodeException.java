package online.flowerinsnow.miteoperator.exception;

public class NoSuchMethodNodeException extends RuntimeException {
    public NoSuchMethodNodeException() {
        super();
    }

    public NoSuchMethodNodeException(String message) {
        super(message);
    }

    public NoSuchMethodNodeException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchMethodNodeException(Throwable cause) {
        super(cause);
    }
}
