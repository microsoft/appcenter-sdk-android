package avalanche.core.ingestion.models;

import org.junit.Test;

import java.util.UUID;

import avalanche.test.TestUtils;

import static avalanche.test.TestUtils.checkEquals;
import static avalanche.test.TestUtils.checkNotEquals;
import static org.mockito.Mockito.mock;

public class AbstractLogTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(mock(AbstractLog.class));
    }

    @Test
    public void compare() {

        /* Empty objects. */
        AbstractLog a = new MockLog();
        AbstractLog b = new MockLog();
        checkEquals(a, b);

        /* Toffset. */
        a.setToffset(1);
        checkNotEquals(a, b);
        b.setToffset(1);
        checkEquals(a, b);

        /* Sid. */
        UUID sid1 = UUID.randomUUID();
        UUID sid2 = UUID.randomUUID();
        a.setSid(sid1);
        checkNotEquals(a, b);
        a.setSid(null);
        b.setSid(sid1);
        checkNotEquals(a, b);
        a.setSid(sid2);
        checkNotEquals(a, b);
        a.setSid(sid1);
        checkEquals(a, b);

        /* Device. */
        Device d1 = new Device();
        d1.setLocale("a");
        Device d2 = new Device();
        d2.setSdkVersion("a");
        a.setDevice(d1);
        checkNotEquals(a, b);
        a.setDevice(null);
        b.setDevice(d1);
        checkNotEquals(a, b);
        a.setDevice(d2);
        checkNotEquals(a, b);
        a.setDevice(d1);
        checkEquals(a, b);
    }

    private static class MockLog extends AbstractLog {

        @Override
        public String getType() {
            return null;
        }
    }
}
