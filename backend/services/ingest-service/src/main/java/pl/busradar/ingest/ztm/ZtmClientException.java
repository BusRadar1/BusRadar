package pl.busradar.ingest.ztm;

public class ZtmClientException extends RuntimeException {

    public ZtmClientException(String message) {
        super(message);
    }

    public ZtmClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
