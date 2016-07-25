package avalanche.core.ingestion.http;

import junit.framework.Assert;

import org.junit.Test;

public class HttpExceptionTest {
    @Test
    public void equalsAndHashCode() {
        HttpException unauthorized = new HttpException(401);

        Assert.assertEquals(unauthorized.hashCode(), new HttpException(401).hashCode());
        Assert.assertTrue(unauthorized.equals(new HttpException((401))));
    }
}
