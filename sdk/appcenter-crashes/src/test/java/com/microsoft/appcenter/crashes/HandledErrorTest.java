/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes;

import android.content.Context;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.appcenter.crashes.ingestion.models.Exception;
import com.microsoft.appcenter.crashes.ingestion.models.HandledErrorLog;
import com.microsoft.appcenter.crashes.ingestion.models.StackFrame;
import com.microsoft.appcenter.crashes.model.ErrorReport;
import com.microsoft.appcenter.crashes.utils.ErrorLogHelper;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.context.UserIdContext;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.appcenter.Flags.DEFAULTS;
import static com.microsoft.appcenter.test.TestUtils.generateString;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

public class HandledErrorTest extends AbstractCrashesTest {

    private Crashes mCrashes;

    @Mock
    private Channel mChannel;

    private void startCrashes() {
        mCrashes = Crashes.getInstance();
        mCrashes.onStarting(mAppCenterHandler);
        mCrashes.onStarted(mock(Context.class), mChannel, "mock", null, true);
    }

    @Test
    public void notInit() {
        Crashes.trackException(EXCEPTION, null, null);
        verifyStatic();
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());
    }

    @Test
    public void trackException() {
        startCrashes();
        Crashes.trackException(EXCEPTION);
        verify(mChannel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                return item instanceof HandledErrorLog && EXCEPTION.getMessage() != null
                        && EXCEPTION.getMessage().equals(((HandledErrorLog) item).getException().getMessage());
            }
        }), eq(mCrashes.getGroupName()), eq(DEFAULTS));
        reset(mChannel);
        Crashes.trackException(EXCEPTION, new HashMap<String, String>() {{
            put(null, null);
            put("", null);
            put(generateString(ErrorLogHelper.MAX_PROPERTY_ITEM_LENGTH + 1, '*'), null);
            put("1", null);
        }}, null);
        verify(mChannel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                return item instanceof HandledErrorLog && EXCEPTION.getMessage() != null
                        && EXCEPTION.getMessage().equals(((HandledErrorLog) item).getException().getMessage())
                        && ((HandledErrorLog) item).getProperties().size() == 0;
            }
        }), eq(mCrashes.getGroupName()), eq(DEFAULTS));
        reset(mChannel);
        Crashes.trackException(EXCEPTION, new HashMap<String, String>() {{
            for (int i = 0; i < 30; i++) {
                put("valid" + i, "valid");
            }
        }}, null);
        verify(mChannel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                return item instanceof HandledErrorLog && EXCEPTION.getMessage() != null
                        && EXCEPTION.getMessage().equals(((HandledErrorLog) item).getException().getMessage())
                        && ((HandledErrorLog) item).getProperties().size() == 20;
            }
        }), eq(mCrashes.getGroupName()), eq(DEFAULTS));
        reset(mChannel);
        final String longerMapItem = generateString(ErrorLogHelper.MAX_PROPERTY_ITEM_LENGTH + 1, '*');
        Crashes.trackException(EXCEPTION, new HashMap<String, String>() {{
            put(longerMapItem, longerMapItem);
        }}, null);
        verify(mChannel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof HandledErrorLog) {
                    HandledErrorLog errorLog = (HandledErrorLog) item;
                    if (EXCEPTION.getMessage() != null && EXCEPTION.getMessage().equals((errorLog.getException().getMessage()))) {
                        if (errorLog.getProperties().size() == 1) {
                            Map.Entry<String, String> entry = errorLog.getProperties().entrySet().iterator().next();
                            return entry.getKey().length() == ErrorLogHelper.MAX_PROPERTY_ITEM_LENGTH && entry.getValue().length() == ErrorLogHelper.MAX_PROPERTY_ITEM_LENGTH;
                        }
                    }
                }
                return false;
            }
        }), eq(mCrashes.getGroupName()), eq(DEFAULTS));

        HandledErrorLog mockLog = mock(HandledErrorLog.class);
        CrashesListener mockListener = mock(CrashesListener.class);
        mCrashes.setInstanceListener(mockListener);

        /* mCrashes callback test for trackException. */
        mCrashes.getChannelListener().onBeforeSending(mockLog);
        verify(mockListener, never()).onBeforeSending(any(ErrorReport.class));
        mCrashes.getChannelListener().onSuccess(mockLog);
        verify(mockListener, never()).onSendingSucceeded(any(ErrorReport.class));
        mCrashes.getChannelListener().onFailure(mockLog, EXCEPTION);
        verify(mockListener, never()).onSendingFailed(any(ErrorReport.class), eq(EXCEPTION));

        ErrorAttachmentLog attachmentLog = mock(ErrorAttachmentLog.class);
        mCrashes.getChannelListener().onBeforeSending(attachmentLog);
        verify(mockListener, never()).onBeforeSending(any(ErrorReport.class));
        mCrashes.getChannelListener().onSuccess(attachmentLog);
        verify(mockListener, never()).onSendingSucceeded(any(ErrorReport.class));
        mCrashes.getChannelListener().onFailure(attachmentLog, EXCEPTION);
        verify(mockListener, never()).onSendingFailed(any(ErrorReport.class), eq(EXCEPTION));
    }

    @Test
    public void trackExceptionForWrapperSdk() {
        StackFrame frame = new StackFrame();
        frame.setClassName("1");
        frame.setFileName("2");
        frame.setLineNumber(3);
        frame.setMethodName("4");
        final com.microsoft.appcenter.crashes.ingestion.models.Exception exception = new com.microsoft.appcenter.crashes.ingestion.models.Exception();
        exception.setType("5");
        exception.setMessage("6");
        exception.setFrames(singletonList(frame));

        mCrashes = Crashes.getInstance();
        mChannel = mock(Channel.class);

        WrapperSdkExceptionManager.trackException(exception, null, null);
        verify(mChannel, never()).enqueue(any(Log.class), eq(mCrashes.getGroupName()), anyInt());
        mCrashes.onStarting(mAppCenterHandler);
        mCrashes.onStarted(mock(Context.class), mChannel, "", null, true);
        WrapperSdkExceptionManager.trackException(exception, null, null);
        verify(mChannel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                return item instanceof HandledErrorLog && exception.equals(((HandledErrorLog) item).getException());
            }
        }), eq(mCrashes.getGroupName()), eq(DEFAULTS));
        reset(mChannel);
        WrapperSdkExceptionManager.trackException(exception, new HashMap<String, String>() {{
            put(null, null);
            put("", null);
            put(generateString(ErrorLogHelper.MAX_PROPERTY_ITEM_LENGTH + 1, '*'), null);
            put("1", null);
        }}, null);
        verify(mChannel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                return item instanceof HandledErrorLog && exception.equals(((HandledErrorLog) item).getException())
                        && ((HandledErrorLog) item).getProperties().size() == 0;
            }
        }), eq(mCrashes.getGroupName()), eq(DEFAULTS));
        reset(mChannel);
        WrapperSdkExceptionManager.trackException(exception, new HashMap<String, String>() {{
            for (int i = 0; i < 30; i++) {
                put("valid" + i, "valid");
            }
        }}, null);
        verify(mChannel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                return item instanceof HandledErrorLog && exception.equals(((HandledErrorLog) item).getException())
                        && ((HandledErrorLog) item).getProperties().size() == 20;
            }
        }), eq(mCrashes.getGroupName()), eq(DEFAULTS));
        reset(mChannel);
        final String longerMapItem = generateString(ErrorLogHelper.MAX_PROPERTY_ITEM_LENGTH + 1, '*');
        WrapperSdkExceptionManager.trackException(exception, new HashMap<String, String>() {{
            put(longerMapItem, longerMapItem);
        }}, null);
        verify(mChannel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof HandledErrorLog) {
                    HandledErrorLog errorLog = (HandledErrorLog) item;
                    if (exception.equals((errorLog.getException()))) {
                        if (errorLog.getProperties().size() == 1) {
                            Map.Entry<String, String> entry = errorLog.getProperties().entrySet().iterator().next();
                            return entry.getKey().length() == ErrorLogHelper.MAX_PROPERTY_ITEM_LENGTH && entry.getValue().length() == ErrorLogHelper.MAX_PROPERTY_ITEM_LENGTH;
                        }
                    }
                }
                return false;
            }
        }), eq(mCrashes.getGroupName()), eq(DEFAULTS));
    }

    @Test
    public void trackExceptionWithUserId() {
        startCrashes();
        UserIdContext.getInstance().setUserId("charlie");
        Crashes.trackException(EXCEPTION);
        ArgumentCaptor<HandledErrorLog> log = ArgumentCaptor.forClass(HandledErrorLog.class);
        verify(mChannel).enqueue(log.capture(), eq(mCrashes.getGroupName()), eq(DEFAULTS));
        assertNotNull(log.getValue());
        assertEquals(EXCEPTION.getMessage(), log.getValue().getException().getMessage());
        assertEquals("charlie", log.getValue().getUserId());
    }

    @Test
    public void trackExceptionWithOneAttachment() {

        /* If we start crashes. */
        startCrashes();

        /* When we track error with an attachment. */
        ErrorAttachmentLog textAttachment = ErrorAttachmentLog.attachmentWithText("text", null);
        Crashes.trackException(EXCEPTION, null, Collections.singleton(textAttachment));

        /* Then we send the handled error. */
        ArgumentCaptor<Log> log = ArgumentCaptor.forClass(Log.class);
        verify(mChannel, times(2)).enqueue(log.capture(), eq(mCrashes.getGroupName()), eq(DEFAULTS));
        assertNotNull(log.getAllValues());
        assertEquals(2, log.getAllValues().size());
        assertTrue(log.getAllValues().get(0) instanceof HandledErrorLog);
        HandledErrorLog handledErrorLog = (HandledErrorLog) log.getAllValues().get(0);
        assertEquals(EXCEPTION.getMessage(), handledErrorLog.getException().getMessage());

        /* Then the attachment. */
        assertSame(textAttachment, log.getAllValues().get(1));
    }

    @Test
    public void trackExceptionWithOneAttachmentFromWrapper() {

        /* If we start crashes. */
        startCrashes();

        /* When we track error with an attachment. */
        ErrorAttachmentLog textAttachment = ErrorAttachmentLog.attachmentWithText("text", null);
        Exception exception = new Exception();
        String errorId = WrapperSdkExceptionManager.trackException(exception, null, Collections.singleton(textAttachment));
        assertNotNull(errorId);

        /* Then we send the handled error. */
        ArgumentCaptor<Log> log = ArgumentCaptor.forClass(Log.class);
        verify(mChannel, times(2)).enqueue(log.capture(), eq(mCrashes.getGroupName()), eq(DEFAULTS));
        assertNotNull(log.getAllValues());
        assertEquals(2, log.getAllValues().size());
        assertTrue(log.getAllValues().get(0) instanceof HandledErrorLog);
        HandledErrorLog handledErrorLog = (HandledErrorLog) log.getAllValues().get(0);
        assertEquals(exception, handledErrorLog.getException());
        assertEquals(errorId, String.valueOf(handledErrorLog.getId()));

        /* Then the attachment. */
        assertSame(textAttachment, log.getAllValues().get(1));
    }

    @Test
    public void trackExceptionWithEverything() {

        /* If we start crashes. */
        startCrashes();

        /* And set a userId. */
        UserIdContext.getInstance().setUserId("omega");

        /* When we track error. */
        ErrorAttachmentLog textAttachment = ErrorAttachmentLog.attachmentWithText("text", null);
        ErrorAttachmentLog binaryAttachment = ErrorAttachmentLog.attachmentWithBinary("hello".getBytes(), "hello.so", "application/octet-stream");
        HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put("a", "b");
            }
        };
        Crashes.trackException(EXCEPTION, properties, Arrays.asList(textAttachment, binaryAttachment));

        /* Then we send the handled error. */
        ArgumentCaptor<Log> logs = ArgumentCaptor.forClass(Log.class);
        verify(mChannel, times(3)).enqueue(logs.capture(), eq(mCrashes.getGroupName()), eq(DEFAULTS));
        assertNotNull(logs.getAllValues());
        assertEquals(3, logs.getAllValues().size());
        assertTrue(logs.getAllValues().get(0) instanceof HandledErrorLog);
        HandledErrorLog handledErrorLog = (HandledErrorLog) logs.getAllValues().get(0);
        assertEquals(EXCEPTION.getMessage(), handledErrorLog.getException().getMessage());
        assertEquals(properties, handledErrorLog.getProperties());

        /* Then the attachments. */
        assertSame(textAttachment, logs.getAllValues().get(1));
        assertSame(binaryAttachment, logs.getAllValues().get(2));

        /* We send userId only in the error log. */
        assertEquals("omega", handledErrorLog.getUserId());
        assertNull(logs.getAllValues().get(1).getUserId());
        assertNull(logs.getAllValues().get(2).getUserId());
    }

    @Test
    public void trackExceptionWithEverythingFromWrapper() {

        /* If we start crashes. */
        startCrashes();

        /* And set a userId. */
        UserIdContext.getInstance().setUserId("omega");

        /* When we track error. */
        ErrorAttachmentLog textAttachment = ErrorAttachmentLog.attachmentWithText("text", null);
        ErrorAttachmentLog binaryAttachment = ErrorAttachmentLog.attachmentWithBinary("hello".getBytes(), "hello.so", "application/octet-stream");
        HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put("a", "b");
            }
        };
        Exception modelException = new Exception();
        String errorId = WrapperSdkExceptionManager.trackException(modelException, properties, Arrays.asList(textAttachment, binaryAttachment));
        assertNotNull(errorId);

        /* Then we send the handled error. */
        ArgumentCaptor<Log> logs = ArgumentCaptor.forClass(Log.class);
        verify(mChannel, times(3)).enqueue(logs.capture(), eq(mCrashes.getGroupName()), eq(DEFAULTS));
        assertNotNull(logs.getAllValues());
        assertEquals(3, logs.getAllValues().size());
        assertTrue(logs.getAllValues().get(0) instanceof HandledErrorLog);
        HandledErrorLog handledErrorLog = (HandledErrorLog) logs.getAllValues().get(0);
        assertEquals(modelException, handledErrorLog.getException());
        assertEquals(properties, handledErrorLog.getProperties());
        assertEquals(errorId, String.valueOf(handledErrorLog.getId()));

        /* Then the attachments. */
        assertSame(textAttachment, logs.getAllValues().get(1));
        assertSame(binaryAttachment, logs.getAllValues().get(2));

        /* We send userId only in the error log. */
        assertEquals("omega", handledErrorLog.getUserId());
        assertNull(logs.getAllValues().get(1).getUserId());
        assertNull(logs.getAllValues().get(2).getUserId());
    }
}
