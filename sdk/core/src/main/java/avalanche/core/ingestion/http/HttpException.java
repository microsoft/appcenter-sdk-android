package avalanche.core.ingestion.http;

import android.support.annotation.VisibleForTesting;

import java.io.IOException;

@VisibleForTesting
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HttpException exception = (HttpException) o;

        return statusCode == exception.statusCode;
    }

    @Override
    public int hashCode() {
        return statusCode;
    }
}