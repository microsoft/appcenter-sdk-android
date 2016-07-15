package avalanche.core.ingestion.http;

import junit.framework.Assert;

import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class DefaultUrlConnectionFactoryTest {
    @Test
    public void openConnection() throws IOException {
        //noinspection SpellCheckingInspection
        URL url = new URL("http://www.contoso.com");
        HttpURLConnection connection = new DefaultUrlConnectionFactory().openConnection(url);
        Assert.assertEquals(url, connection.getURL());
    }
}
