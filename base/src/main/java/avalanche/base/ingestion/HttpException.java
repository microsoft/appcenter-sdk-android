package avalanche.base.ingestion;

import java.io.IOException;

public class HttpException extends IOException {

    private final int statusCode;

    public HttpException(int status) {
        super(String.valueOf(status));
        this.statusCode = status;
    }

    /**
     * Get the HTTP status code.
     *
     * @return HTTP status code.
     */
    public int getStatusCode() {
        return statusCode;
    }
}