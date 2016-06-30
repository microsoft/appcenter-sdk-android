package avalanche.base.ingestion.http;

import java.io.EOFException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;

public class HttpUtils {

    private static final Class[] RECOVERABLE_EXCEPTIONS = {
            EOFException.class,
            InterruptedIOException.class,
            SocketException.class,
            UnknownHostException.class
    };

    public static boolean isRecoverableError(Throwable t) {
        if (t instanceof HttpException) {
            HttpException exception = (HttpException) t;
            int code = exception.getStatusCode();
            return code >= 500 || code == 408 || code == 429;
        }
        for (Class type : RECOVERABLE_EXCEPTIONS)
            if (t.getClass().isAssignableFrom(type))
                return true;
        return false;
    }
}
