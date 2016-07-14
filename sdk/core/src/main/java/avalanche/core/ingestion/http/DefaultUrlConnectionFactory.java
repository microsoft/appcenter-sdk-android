package avalanche.core.ingestion.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class DefaultUrlConnectionFactory implements UrlConnectionFactory {

    @Override
    public HttpURLConnection openConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }
}
