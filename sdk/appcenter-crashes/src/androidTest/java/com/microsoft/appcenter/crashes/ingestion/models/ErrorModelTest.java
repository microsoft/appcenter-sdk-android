/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes.ingestion.models;

import com.microsoft.appcenter.crashes.ingestion.models.json.ErrorAttachmentLogFactory;
import com.microsoft.appcenter.crashes.ingestion.models.json.HandledErrorLogFactory;
import com.microsoft.appcenter.crashes.ingestion.models.json.ManagedErrorLogFactory;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.json.DefaultLogSerializer;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.UUID;

import static com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog.CHARSET;
import static com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog.DATA;
import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;
import static com.microsoft.appcenter.test.TestUtils.compareSelfNullClass;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@SuppressWarnings("unused")
public class ErrorModelTest {

    private static void checkSerialization(Log log, LogSerializer serializer) throws JSONException {
        String payload = serializer.serializeLog(log);
        Log deSerializedLog = serializer.deserializeLog(payload, null);
        checkEquals(log, deSerializedLog);
    }

    private static void checkSerializationThrowsException(Log log, LogSerializer serializer, Class expectedException) {
        try {
            checkSerialization(log, serializer);
        } catch (java.lang.Exception ex) {
            checkEquals(ex.getClass(), expectedException);
            return;
        }
        fail();
    }

    private static void checkExceptions(LogSerializer serializer, ManagedErrorLog errorLog1, ManagedErrorLog errorLog2, Exception exception1, Exception exception2) throws JSONException {
        errorLog1.setException(exception1);
        errorLog2.setException(null);
        checkNotEquals(errorLog1, errorLog2);
        checkSerialization(errorLog1, serializer);

        errorLog1.setException(null);
        errorLog2.setException(exception2);
        checkNotEquals(errorLog1, errorLog2);

        errorLog1.setException(exception1);
        checkNotEquals(errorLog1, errorLog2);

        errorLog2.setException(errorLog1.getException());
        checkEquals(errorLog1, errorLog2);

        {
            Exception exception3 = new Exception();
            exception3.setType(exception1.getType());
            exception3.setMessage(exception1.getMessage());
            exception3.setStackTrace(exception1.getStackTrace());
            exception3.setFrames(exception1.getFrames());
            exception3.setWrapperSdkName(exception1.getWrapperSdkName());
            exception3.setMinidumpFilePath(exception1.getMinidumpFilePath());
            errorLog2.setException(exception3);
            checkEquals(errorLog1, errorLog2);

            Exception subException1 = new Exception();
            subException1.setType("s1");
            exception1.setInnerExceptions(singletonList(subException1));
            checkNotEquals(errorLog1, errorLog2);

            Exception subException3 = new Exception();
            subException3.setType("s3");
            exception3.setInnerExceptions(singletonList(subException3));
            checkNotEquals(errorLog1, errorLog2);

            exception3.setInnerExceptions(singletonList(subException1));
            checkEquals(errorLog1, errorLog2);
        }

        errorLog2.setException(errorLog1.getException());
        checkEquals(errorLog1, errorLog2);
        checkSerialization(errorLog1, serializer);
        exception1.setInnerExceptions(null);
    }

    private static void checkFrames(LogSerializer serializer, ManagedErrorLog errorLog1, ManagedErrorLog errorLog2, Exception exception1, Exception exception2, StackFrame frame1, StackFrame frame2) throws JSONException {
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

    private static void checkThreads(LogSerializer serializer, ManagedErrorLog errorLog1, ManagedErrorLog errorLog2, Thread thread1, Thread thread2) throws JSONException {
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

    private static void checkFrames(LogSerializer serializer, ManagedErrorLog errorLog1, ManagedErrorLog errorLog2, Thread thread1, Thread thread2, StackFrame frame1, StackFrame frame2) throws JSONException {
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
        compareSelfNullClass(mockErrorLog);
        mockErrorLog.setTimestamp(new Date(1L));
        checkNotEquals(mockErrorLog, new MockErrorLog());
    }

    @Test
    public void managedErrorLog() throws JSONException {

        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(ManagedErrorLog.TYPE, ManagedErrorLogFactory.getInstance());

        Date timestamp = new Date();
        ManagedErrorLog errorLog1 = new ManagedErrorLog();
        errorLog1.setTimestamp(timestamp);
        ManagedErrorLog errorLog2 = new ManagedErrorLog();
        errorLog2.setTimestamp(timestamp);

        compareSelfNullClass(errorLog1);
        checkEquals(errorLog1, errorLog2);

        {
            errorLog1.setId(UUID.randomUUID());
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setId(UUID.randomUUID());
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setId(errorLog1.getId());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setProcessId(1);
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setProcessId(2);
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setProcessId(errorLog1.getProcessId());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setProcessName("1");
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setProcessName("2");
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setProcessName(errorLog1.getProcessName());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setParentProcessId(1);
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setParentProcessId(2);
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setParentProcessId(errorLog1.getParentProcessId());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setParentProcessName("1");
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setParentProcessName("2");
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setParentProcessName(errorLog1.getParentProcessName());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setErrorThreadId(1L);
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setErrorThreadId(2L);
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setErrorThreadId(errorLog1.getErrorThreadId());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setErrorThreadName("1");
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setErrorThreadName("2");
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setErrorThreadName(errorLog1.getErrorThreadName());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setFatal(true);
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setFatal(false);
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setFatal(errorLog1.getFatal());
            checkEquals(errorLog1, errorLog2);
        }
        {
            errorLog1.setAppLaunchTimestamp(new Date(1L));
            checkNotEquals(errorLog1, errorLog2);
            checkSerialization(errorLog1, serializer);

            errorLog2.setAppLaunchTimestamp(new Date(2L));
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setAppLaunchTimestamp(errorLog1.getAppLaunchTimestamp());
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
            Exception exception1 = new Exception();
            Exception exception2 = new Exception();

            compareSelfNullClass(exception1);
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
                exception1.setStackTrace("1");
                checkNotEquals(exception1, exception2);
                checkExceptions(serializer, errorLog1, errorLog2, exception1, exception2);

                exception2.setStackTrace("2");
                checkNotEquals(exception1, exception2);

                exception2.setStackTrace(exception1.getStackTrace());
                checkEquals(exception1, exception2);
            }
            {
                exception1.setMinidumpFilePath("1");
                checkNotEquals(exception1, exception2);
                checkExceptions(serializer, errorLog1, errorLog2, exception1, exception2);

                exception2.setMinidumpFilePath("2");
                checkNotEquals(exception1, exception2);

                exception2.setMinidumpFilePath(exception1.getMinidumpFilePath());
                checkEquals(exception1, exception2);
            }
            {
                errorLog1.setException(exception1);
                errorLog2.setException(exception2);

                StackFrame frame1 = new StackFrame();
                StackFrame frame2 = new StackFrame();

                compareSelfNullClass(frame1);
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
            {
                exception1.setWrapperSdkName("1");
                checkNotEquals(exception1, exception2);
                checkExceptions(serializer, errorLog1, errorLog2, exception1, exception2);

                exception2.setWrapperSdkName("2");
                checkNotEquals(exception1, exception2);

                exception2.setWrapperSdkName(exception1.getWrapperSdkName());
                checkEquals(exception1, exception2);
            }
        }
        {
            Thread thread1 = new Thread();
            Thread thread2 = new Thread();

            compareSelfNullClass(thread1);
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

                StackFrame frame1 = new StackFrame();
                StackFrame frame2 = new StackFrame();

                compareSelfNullClass(frame1);
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
        checkSerialization(errorLog1, serializer);
    }

    @Test
    public void handledErrorLog() throws JSONException {
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(HandledErrorLog.TYPE, HandledErrorLogFactory.getInstance());

        HandledErrorLog errorLog1 = new HandledErrorLog();
        compareSelfNullClass(errorLog1);

        Date timestamp = new Date();
        errorLog1.setTimestamp(timestamp);
        HandledErrorLog errorLog2 = new HandledErrorLog();
        checkNotEquals(errorLog1, errorLog2);

        errorLog2.setTimestamp(timestamp);
        checkEquals(errorLog1, errorLog2);
        {
            errorLog1.setId(UUID.randomUUID());
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setId(UUID.randomUUID());
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setId(errorLog1.getId());
            checkEquals(errorLog1, errorLog2);
        }
        checkSerialization(errorLog1, serializer);
        {
            Exception exception1 = new Exception();
            exception1.setMessage("1");
            Exception exception2 = new Exception();
            exception2.setMessage("2");

            errorLog1.setException(exception1);
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setException(exception2);
            checkNotEquals(errorLog1, errorLog2);

            errorLog2.setException(exception1);
            checkEquals(errorLog1, errorLog2);
        }
        checkSerialization(errorLog1, serializer);
    }

    @Test
    public void errorAttachmentLog() throws JSONException {
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(ErrorAttachmentLog.TYPE, ErrorAttachmentLogFactory.getInstance());

        Date timestamp = new Date();
        ErrorAttachmentLog attachmentLog1 = new ErrorAttachmentLog();
        attachmentLog1.setTimestamp(timestamp);
        ErrorAttachmentLog attachmentLog2 = new ErrorAttachmentLog();
        attachmentLog2.setTimestamp(timestamp);

        compareSelfNullClass(attachmentLog1);
        checkEquals(attachmentLog1, attachmentLog2);
        checkEquals(attachmentLog1.getType(), ErrorAttachmentLog.TYPE);

        {
            attachmentLog1.setId(UUID.randomUUID());
            checkNotEquals(attachmentLog1, attachmentLog2);

            attachmentLog2.setId(UUID.randomUUID());
            checkNotEquals(attachmentLog1, attachmentLog2);

            attachmentLog2.setId(attachmentLog1.getId());
            checkEquals(attachmentLog1, attachmentLog2);
        }
        {
            attachmentLog1.setErrorId(UUID.randomUUID());
            checkNotEquals(attachmentLog1, attachmentLog2);

            attachmentLog2.setErrorId(UUID.randomUUID());
            checkNotEquals(attachmentLog1, attachmentLog2);

            attachmentLog2.setErrorId(attachmentLog1.getErrorId());
            checkEquals(attachmentLog1, attachmentLog2);
        }
        {
            attachmentLog1.setContentType("1");
            checkNotEquals(attachmentLog1, attachmentLog2);

            attachmentLog2.setContentType("2");
            checkNotEquals(attachmentLog1, attachmentLog2);

            attachmentLog2.setContentType(attachmentLog1.getContentType());
            checkEquals(attachmentLog1, attachmentLog2);
        }
        {
            attachmentLog1.setFileName("1");
            checkNotEquals(attachmentLog1, attachmentLog2);

            attachmentLog2.setFileName("2");
            checkNotEquals(attachmentLog1, attachmentLog2);

            attachmentLog2.setFileName(attachmentLog1.getFileName());
            checkEquals(attachmentLog1, attachmentLog2);
        }
        {
            attachmentLog1.setData("1".getBytes(CHARSET));
            checkNotEquals(attachmentLog1, attachmentLog2);

            attachmentLog2.setData("2".getBytes(CHARSET));
            checkNotEquals(attachmentLog1, attachmentLog2);

            attachmentLog2.setData(attachmentLog1.getData());
            checkEquals(attachmentLog1, attachmentLog2);
        }
        {
            attachmentLog1.setSid(UUID.randomUUID());
            checkNotEquals(attachmentLog1, attachmentLog2);

            attachmentLog2.setSid(UUID.randomUUID());
            checkNotEquals(attachmentLog1, attachmentLog2);

            attachmentLog2.setSid(attachmentLog1.getSid());
            checkEquals(attachmentLog1, attachmentLog2);
        }
        {

            /* Check serialization without filename. */
            attachmentLog2.setFileName(null);
        }
        {
            checkSerialization(attachmentLog1, serializer);
            checkSerialization(attachmentLog2, serializer);
        }
    }

    @Test
    public void deserializeInvalidBase64forErrorAttachment() throws JSONException {
        ErrorAttachmentLog log = new ErrorAttachmentLog();
        log.setTimestamp(new Date());
        log.setId(UUID.randomUUID());
        log.setErrorId(UUID.randomUUID());
        log.setData(new byte[0]);
        log.setContentType("text/plain");
        JSONStringer jsonWriter = new JSONStringer();
        jsonWriter.object();
        log.write(jsonWriter);
        jsonWriter.endObject();
        JSONObject json = new JSONObject(jsonWriter.toString());
        json.put(DATA, "a");
        try {
            new ErrorAttachmentLog().read(json);
            Assert.fail("Expected json exception here");
        } catch (JSONException e) {
            assertEquals("bad base-64", e.getMessage());
        }
    }

    private static class MockErrorLog extends AbstractErrorLog {

        @Override
        public String getType() {
            return "mockError";
        }
    }
}
