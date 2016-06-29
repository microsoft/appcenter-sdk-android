package avalanche.base.ingestion.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public interface UrlConnectionFactory {

    HttpURLConnection openConnection(URL url) throws IOException;
}
