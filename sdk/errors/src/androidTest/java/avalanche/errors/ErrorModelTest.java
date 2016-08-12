package avalanche.errors;

import org.json.JSONException;
import org.junit.Test;

import java.util.UUID;

import avalanche.core.ingestion.models.Log;
import avalanche.core.ingestion.models.json.DefaultLogSerializer;
import avalanche.core.ingestion.models.json.LogSerializer;
import avalanche.errors.ingestion.models.AbstractErrorLog;
import avalanche.errors.ingestion.models.JavaErrorLog;
import avalanche.errors.ingestion.models.json.JavaErrorLogFactory;
import avalanche.test.TestUtils;

import static avalanche.test.TestUtils.checkEquals;
import static avalanche.test.TestUtils.checkNotEquals;

public class ErrorModelTest {

    @Test
    public void abstractErrorLog() {
        MockErrorLog mockErrorLog = new MockErrorLog();
        TestUtils.compareSelfNullClass(mockErrorLog);
        mockErrorLog.setToffset(1L);
        checkNotEquals(mockErrorLog, new MockErrorLog());
    }

    @Test
    public void javaErrorLog() throws JSONException {

        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(JavaErrorLog.TYPE, JavaErrorLogFactory.getInstance());

        JavaErrorLog errorLog1 = new JavaErrorLog();
        JavaErrorLog errorLog2 = new JavaErrorLog();

        TestUtils.compareSelfNullClass(errorLog1);
        checkEquals(errorLog1, errorLog2);

        {
            errorLog1.setId(UUID.randomUUID());
            checkNotEquals(errorLog1, errorLog2);
            checkSerialization(errorLog1, serializer);

            errorLog1.setId(null);
            errorLog2.setId(UUID.randomUUID());
            checkNotEquals(errorLog1, errorLog2);

            errorLog1.setId(UUID.randomUUID());
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setId(errorLog1.getId());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setProcessId(1);
            checkNotEquals(errorLog1, errorLog2);
            checkSerialization(errorLog1, serializer);

            errorLog1.setProcessId(null);
            errorLog2.setProcessId(2);
            checkNotEquals(errorLog1, errorLog2);

            errorLog1.setProcessId(1);
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setProcessId(errorLog1.getProcessId());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setProcessName("1");
            checkNotEquals(errorLog1, errorLog2);
            checkSerialization(errorLog1, serializer);

            errorLog1.setProcessName(null);
            errorLog2.setProcessName("2");
            checkNotEquals(errorLog1, errorLog2);

            errorLog1.setProcessName("1");
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setProcessName(errorLog1.getProcessName());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setParentProcessId(1);
            checkNotEquals(errorLog1, errorLog2);
            checkSerialization(errorLog1, serializer);

            errorLog1.setParentProcessId(null);
            errorLog2.setParentProcessId(2);
            checkNotEquals(errorLog1, errorLog2);

            errorLog1.setParentProcessId(1);
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setParentProcessId(errorLog1.getParentProcessId());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setParentProcessName("1");
            checkNotEquals(errorLog1, errorLog2);
            checkSerialization(errorLog1, serializer);

            errorLog1.setParentProcessName(null);
            errorLog2.setParentProcessName("2");
            checkNotEquals(errorLog1, errorLog2);

            errorLog1.setParentProcessName("1");
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setParentProcessName(errorLog1.getParentProcessName());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setErrorThreadId(1L);
            checkNotEquals(errorLog1, errorLog2);
            checkSerialization(errorLog1, serializer);

            errorLog1.setErrorThreadId(null);
            errorLog2.setErrorThreadId(2L);
            checkNotEquals(errorLog1, errorLog2);

            errorLog1.setErrorThreadId(1L);
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setErrorThreadId(errorLog1.getErrorThreadId());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setErrorThreadName("1");
            checkNotEquals(errorLog1, errorLog2);
            checkSerialization(errorLog1, serializer);

            errorLog1.setErrorThreadName(null);
            errorLog2.setErrorThreadName("2");
            checkNotEquals(errorLog1, errorLog2);

            errorLog1.setErrorThreadName("1");
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setErrorThreadName(errorLog1.getErrorThreadName());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setFatal(true);
            checkNotEquals(errorLog1, errorLog2);
            checkSerialization(errorLog1, serializer);

            errorLog1.setFatal(null);
            errorLog2.setFatal(false);
            checkNotEquals(errorLog1, errorLog2);

            errorLog1.setFatal(true);
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setFatal(errorLog1.getFatal());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setAppLaunchTOffset(1L);
            checkNotEquals(errorLog1, errorLog2);
            checkSerialization(errorLog1, serializer);

            errorLog1.setAppLaunchTOffset(null);
            errorLog2.setAppLaunchTOffset(2L);
            checkNotEquals(errorLog1, errorLog2);

            errorLog1.setAppLaunchTOffset(1L);
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setAppLaunchTOffset(errorLog1.getAppLaunchTOffset());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setArchitecture("1");
            checkNotEquals(errorLog1, errorLog2);
            checkSerialization(errorLog1, serializer);

            errorLog1.setArchitecture(null);
            errorLog2.setArchitecture("2");
            checkNotEquals(errorLog1, errorLog2);

            errorLog1.setArchitecture("1");
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setArchitecture(errorLog1.getArchitecture());
            checkEquals(errorLog1, errorLog2);
        }
    }

    private void checkSerialization(JavaErrorLog errorLog, LogSerializer serializer) throws JSONException {
        String payload = serializer.serializeLog(errorLog);
        Log deserializedLog = serializer.deserializeLog(payload);
        checkEquals(errorLog, deserializedLog);
    }

    private static class MockErrorLog extends AbstractErrorLog {

        @Override
        public String getType() {
            return "mockError";
        }
    }
}
