package avalanche.errors;

import org.json.JSONException;
import org.junit.Test;

import java.util.UUID;

import avalanche.core.ingestion.models.Log;
import avalanche.core.ingestion.models.json.DefaultLogSerializer;
import avalanche.core.ingestion.models.json.LogSerializer;
import avalanche.errors.ingestion.models.AbstractErrorLog;
import avalanche.errors.ingestion.models.JavaErrorLog;
import avalanche.errors.ingestion.models.JavaException;
import avalanche.errors.ingestion.models.JavaStackFrame;
import avalanche.errors.ingestion.models.JavaThread;
import avalanche.errors.ingestion.models.json.JavaErrorLogFactory;
import avalanche.errors.model.ErrorAttachment;
import avalanche.errors.model.ErrorBinaryAttachment;
import avalanche.test.TestUtils;

import static avalanche.test.TestUtils.checkEquals;
import static avalanche.test.TestUtils.checkNotEquals;
import static java.util.Collections.singletonList;

@SuppressWarnings("unused")
public class ErrorModelTest {

    private static void checkSerialization(JavaErrorLog errorLog, LogSerializer serializer) throws JSONException {
        String payload = serializer.serializeLog(errorLog);
        Log deserializedLog = serializer.deserializeLog(payload);
        checkEquals(errorLog, deserializedLog);
    }

    private static void checkExceptions(LogSerializer serializer, JavaErrorLog errorLog1, JavaErrorLog errorLog2, JavaException exception1, JavaException exception2) throws JSONException {
        errorLog1.setExceptions(singletonList(exception1));
        errorLog2.setExceptions(null);
        checkNotEquals(errorLog1, errorLog2);
        checkSerialization(errorLog1, serializer);

        errorLog1.setExceptions(null);
        errorLog2.setExceptions(singletonList(exception2));
        checkNotEquals(errorLog1, errorLog2);

        errorLog1.setExceptions(singletonList(exception1));
        checkNotEquals(errorLog1, errorLog2);

        errorLog2.setExceptions(errorLog1.getExceptions());
        checkEquals(errorLog1, errorLog2);
    }

    private static void checkFrames(LogSerializer serializer, JavaErrorLog errorLog1, JavaErrorLog errorLog2, JavaException exception1, JavaException exception2, JavaStackFrame frame1, JavaStackFrame frame2) throws JSONException {
        exception1.setFrames(singletonList(frame1));
        exception2.setFrames(null);
        checkNotEquals(errorLog1, errorLog2);
        checkSerialization(errorLog1, serializer);

        exception1.setFrames(null);
        exception2.setFrames(singletonList(frame2));
        checkNotEquals(errorLog1, errorLog2);

        exception1.setFrames(singletonList(frame1));
        checkNotEquals(errorLog1, errorLog2);

        exception2.setFrames(exception1.getFrames());
        checkEquals(errorLog1, errorLog2);
    }

    private static void checkThreads(LogSerializer serializer, JavaErrorLog errorLog1, JavaErrorLog errorLog2, JavaThread thread1, JavaThread thread2) throws JSONException {
        errorLog1.setThreads(singletonList(thread1));
        errorLog2.setThreads(null);
        checkNotEquals(errorLog1, errorLog2);
        checkSerialization(errorLog1, serializer);

        errorLog1.setThreads(null);
        errorLog2.setThreads(singletonList(thread2));
        checkNotEquals(errorLog1, errorLog2);

        errorLog1.setThreads(singletonList(thread1));
        checkNotEquals(errorLog1, errorLog2);

        errorLog2.setThreads(errorLog1.getThreads());
        checkEquals(errorLog1, errorLog2);
    }

    private static void checkFrames(LogSerializer serializer, JavaErrorLog errorLog1, JavaErrorLog errorLog2, JavaThread thread1, JavaThread thread2, JavaStackFrame frame1, JavaStackFrame frame2) throws JSONException {
        thread1.setFrames(singletonList(frame1));
        thread2.setFrames(null);
        checkNotEquals(errorLog1, errorLog2);
        checkSerialization(errorLog1, serializer);

        thread1.setFrames(null);
        thread2.setFrames(singletonList(frame2));
        checkNotEquals(errorLog1, errorLog2);

        thread1.setFrames(singletonList(frame1));
        checkNotEquals(errorLog1, errorLog2);

        thread2.setFrames(thread1.getFrames());
        checkEquals(errorLog1, errorLog2);
    }

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

            errorLog2.setId(UUID.randomUUID());
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setId(errorLog1.getId());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setProcessId(1);
            checkNotEquals(errorLog1, errorLog2);
            checkSerialization(errorLog1, serializer);

            errorLog2.setProcessId(2);
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setProcessId(errorLog1.getProcessId());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setProcessName("1");
            checkNotEquals(errorLog1, errorLog2);
            checkSerialization(errorLog1, serializer);

            errorLog2.setProcessName("2");
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setProcessName(errorLog1.getProcessName());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setParentProcessId(1);
            checkNotEquals(errorLog1, errorLog2);
            checkSerialization(errorLog1, serializer);

            errorLog2.setParentProcessId(2);
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setParentProcessId(errorLog1.getParentProcessId());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setParentProcessName("1");
            checkNotEquals(errorLog1, errorLog2);
            checkSerialization(errorLog1, serializer);

            errorLog2.setParentProcessName("2");
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setParentProcessName(errorLog1.getParentProcessName());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setErrorThreadId(1L);
            checkNotEquals(errorLog1, errorLog2);
            checkSerialization(errorLog1, serializer);

            errorLog2.setErrorThreadId(2L);
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setErrorThreadId(errorLog1.getErrorThreadId());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setErrorThreadName("1");
            checkNotEquals(errorLog1, errorLog2);
            checkSerialization(errorLog1, serializer);

            errorLog2.setErrorThreadName("2");
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setErrorThreadName(errorLog1.getErrorThreadName());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setFatal(true);
            checkNotEquals(errorLog1, errorLog2);
            checkSerialization(errorLog1, serializer);

            errorLog2.setFatal(false);
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setFatal(errorLog1.getFatal());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setAppLaunchTOffset(1L);
            checkNotEquals(errorLog1, errorLog2);
            checkSerialization(errorLog1, serializer);

            errorLog2.setAppLaunchTOffset(2L);
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setAppLaunchTOffset(errorLog1.getAppLaunchTOffset());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setArchitecture("1");
            checkNotEquals(errorLog1, errorLog2);
            checkSerialization(errorLog1, serializer);

            errorLog2.setArchitecture("2");
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setArchitecture(errorLog1.getArchitecture());
            checkEquals(errorLog1, errorLog2);
        }
        {
            JavaException exception1 = new JavaException();
            JavaException exception2 = new JavaException();

            TestUtils.compareSelfNullClass(exception1);
            checkEquals(exception1, exception2);

            {
                exception1.setType("1");
                checkNotEquals(exception1, exception2);
                checkExceptions(serializer, errorLog1, errorLog2, exception1, exception2);

                exception2.setType("2");
                checkNotEquals(exception1, exception2);

                exception2.setType(exception1.getType());
                checkEquals(exception1, exception2);
            }
            {
                exception1.setMessage("1");
                checkNotEquals(exception1, exception2);
                checkExceptions(serializer, errorLog1, errorLog2, exception1, exception2);

                exception2.setMessage("2");
                checkNotEquals(exception1, exception2);

                exception2.setMessage(exception1.getMessage());
                checkEquals(exception1, exception2);
            }
            {
                errorLog1.setExceptions(singletonList(exception1));
                errorLog2.setExceptions(singletonList(exception2));

                JavaStackFrame frame1 = new JavaStackFrame();
                JavaStackFrame frame2 = new JavaStackFrame();

                TestUtils.compareSelfNullClass(frame1);
                checkEquals(frame1, frame2);

                {
                    frame1.setClassName("1");
                    checkNotEquals(frame1, frame2);
                    checkFrames(serializer, errorLog1, errorLog2, exception1, exception2, frame1, frame2);

                    frame2.setClassName("2");
                    checkNotEquals(frame1, frame2);

                    frame2.setClassName(frame1.getClassName());
                    checkEquals(frame1, frame2);
                }
                {
                    frame1.setMethodName("1");
                    checkNotEquals(frame1, frame2);
                    checkFrames(serializer, errorLog1, errorLog2, exception1, exception2, frame1, frame2);

                    frame2.setMethodName("2");
                    checkNotEquals(frame1, frame2);

                    frame2.setMethodName(frame1.getMethodName());
                    checkEquals(frame1, frame2);
                }
                {
                    frame1.setLineNumber(1);
                    checkNotEquals(frame1, frame2);
                    checkFrames(serializer, errorLog1, errorLog2, exception1, exception2, frame1, frame2);

                    frame2.setLineNumber(2);
                    checkNotEquals(frame1, frame2);

                    frame2.setLineNumber(frame1.getLineNumber());
                    checkEquals(frame1, frame2);
                }
                {
                    frame1.setFileName("1");
                    checkNotEquals(frame1, frame2);
                    checkFrames(serializer, errorLog1, errorLog2, exception1, exception2, frame1, frame2);

                    frame2.setFileName("2");
                    checkNotEquals(frame1, frame2);

                    frame2.setFileName(frame1.getFileName());
                    checkEquals(frame1, frame2);
                }
            }
        }
        {
            JavaThread thread1 = new JavaThread();
            JavaThread thread2 = new JavaThread();

            TestUtils.compareSelfNullClass(thread1);
            checkEquals(thread1, thread2);

            {
                thread1.setId(1L);
                checkNotEquals(thread1, thread2);
                checkThreads(serializer, errorLog1, errorLog2, thread1, thread2);

                thread2.setId(2L);
                checkNotEquals(thread1, thread2);

                thread2.setId(thread1.getId());
                checkEquals(thread1, thread2);
            }
            {
                thread1.setName("1");
                checkNotEquals(thread1, thread2);
                checkThreads(serializer, errorLog1, errorLog2, thread1, thread2);

                thread2.setName("2");
                checkNotEquals(thread1, thread2);

                thread2.setName(thread1.getName());
                checkEquals(thread1, thread2);
            }
            {
                errorLog1.setThreads(singletonList(thread1));
                errorLog2.setThreads(singletonList(thread2));

                JavaStackFrame frame1 = new JavaStackFrame();
                JavaStackFrame frame2 = new JavaStackFrame();

                TestUtils.compareSelfNullClass(frame1);
                checkEquals(frame1, frame2);

                {
                    frame1.setClassName("1");
                    checkNotEquals(frame1, frame2);
                    checkFrames(serializer, errorLog1, errorLog2, thread1, thread2, frame1, frame2);

                    frame2.setClassName("2");
                    checkNotEquals(frame1, frame2);

                    frame2.setClassName(frame1.getClassName());
                    checkEquals(frame1, frame2);
                }
                {
                    frame1.setMethodName("1");
                    checkNotEquals(frame1, frame2);
                    checkFrames(serializer, errorLog1, errorLog2, thread1, thread2, frame1, frame2);

                    frame2.setMethodName("2");
                    checkNotEquals(frame1, frame2);

                    frame2.setMethodName(frame1.getMethodName());
                    checkEquals(frame1, frame2);
                }
                {
                    frame1.setLineNumber(1);
                    checkNotEquals(frame1, frame2);
                    checkFrames(serializer, errorLog1, errorLog2, thread1, thread2, frame1, frame2);

                    frame2.setLineNumber(2);
                    checkNotEquals(frame1, frame2);

                    frame2.setLineNumber(frame1.getLineNumber());
                    checkEquals(frame1, frame2);
                }
                {
                    frame1.setFileName("1");
                    checkNotEquals(frame1, frame2);
                    checkFrames(serializer, errorLog1, errorLog2, thread1, thread2, frame1, frame2);

                    frame2.setFileName("2");
                    checkNotEquals(frame1, frame2);

                    frame2.setFileName(frame1.getFileName());
                    checkEquals(frame1, frame2);
                }
            }
        }
        {
            ErrorAttachment errorAttachment1 = new ErrorAttachment();
            ErrorAttachment errorAttachment2 = new ErrorAttachment();

            TestUtils.compareSelfNullClass(errorAttachment1);
            checkEquals(errorAttachment1, errorAttachment2);

            {
                errorAttachment1.setTextAttachment("1");
                checkNotEquals(errorAttachment1, errorAttachment2);

                errorAttachment2.setTextAttachment("2");
                checkNotEquals(errorAttachment1, errorAttachment2);

                errorAttachment2.setTextAttachment(errorAttachment1.getTextAttachment());
                checkEquals(errorAttachment1, errorAttachment2);
            }
            {
                errorLog1.setErrorAttachment(errorAttachment1);
                errorLog2.setErrorAttachment(null);
                checkNotEquals(errorLog1, errorLog2);
                checkSerialization(errorLog1, serializer);

                errorLog1.setErrorAttachment(null);
                errorLog2.setErrorAttachment(errorAttachment2);
                checkNotEquals(errorLog1, errorLog2);

                errorLog1.setErrorAttachment(errorLog2.getErrorAttachment());
                checkEquals(errorLog1, errorLog2);

                ErrorBinaryAttachment errorBinaryAttachment1 = new ErrorBinaryAttachment();
                ErrorBinaryAttachment errorBinaryAttachment2 = new ErrorBinaryAttachment();

                TestUtils.compareSelfNullClass(errorBinaryAttachment1);
                checkEquals(errorBinaryAttachment1, errorBinaryAttachment2);

                {
                    errorBinaryAttachment1.setContentType("1");
                    checkNotEquals(errorBinaryAttachment1, errorBinaryAttachment2);

                    errorBinaryAttachment2.setContentType("2");
                    checkNotEquals(errorBinaryAttachment1, errorBinaryAttachment2);

                    errorBinaryAttachment2.setContentType(errorBinaryAttachment1.getContentType());
                    checkEquals(errorBinaryAttachment1, errorBinaryAttachment2);
                }
                {
                    errorBinaryAttachment1.setFileName("1");
                    checkNotEquals(errorBinaryAttachment1, errorBinaryAttachment2);

                    errorBinaryAttachment2.setFileName("2");
                    checkNotEquals(errorBinaryAttachment1, errorBinaryAttachment2);

                    errorBinaryAttachment2.setFileName(errorBinaryAttachment1.getFileName());
                    checkEquals(errorBinaryAttachment1, errorBinaryAttachment2);
                }
                {
                    errorBinaryAttachment1.setData(new byte[]{1});
                    checkNotEquals(errorBinaryAttachment1, errorBinaryAttachment2);
                    checkSerialization(errorLog1, serializer);

                    errorBinaryAttachment2.setData(new byte[]{2});
                    checkNotEquals(errorBinaryAttachment1, errorBinaryAttachment2);

                    errorBinaryAttachment2.setData(errorBinaryAttachment1.getData());
                    checkEquals(errorBinaryAttachment1, errorBinaryAttachment2);
                }
                {
                    errorAttachment1.setBinaryAttachment(errorBinaryAttachment1);
                    errorAttachment2.setBinaryAttachment(null);
                    checkNotEquals(errorAttachment1, errorAttachment2);

                    errorAttachment1.setBinaryAttachment(null);
                    errorAttachment2.setBinaryAttachment(errorBinaryAttachment2);
                    checkNotEquals(errorAttachment1, errorAttachment2);

                    errorAttachment1.setBinaryAttachment(errorAttachment2.getBinaryAttachment());
                    checkEquals(errorAttachment1, errorAttachment2);

                    errorBinaryAttachment2.setData(null);
                    checkSerialization(errorLog1, serializer);

                    errorBinaryAttachment2.setData(errorBinaryAttachment1.getData());
                    checkSerialization(errorLog1, serializer);
                }
            }
        }
        checkSerialization(errorLog1, serializer);
    }

    private static class MockErrorLog extends AbstractErrorLog {

        @Override
        public String getType() {
            return "mockError";
        }
    }
}
