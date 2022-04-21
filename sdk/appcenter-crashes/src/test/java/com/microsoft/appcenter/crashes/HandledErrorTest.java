/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes;

import static com.microsoft.appcenter.Flags.DEFAULTS;
import static com.microsoft.appcenter.test.TestUtils.generateString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static java.util.Collections.singletonList;

import android.content.Context;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.appcenter.crashes.ingestion.models.Exception;
import com.microsoft.appcenter.crashes.ingestion.models.HandledErrorLog;
import com.microsoft.appcenter.crashes.ingestion.models.StackFrame;
import com.microsoft.appcenter.crashes.utils.ErrorLogHelper;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.context.UserIdContext;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HandledErrorTest extends AbstractCrashesTest {

    private Crashes mCrashes;

    @Mock
    private Channel mChannel;

    @Captor
    private ArgumentCaptor<Log> mLog;

    private void startCrashes() {
        mCrashes = Crashes.getInstance();
        mCrashes.onStarting(mAppCenterHandler);
        mCrashes.onStarted(mock(Context.class), mChannel, "mock", null, true);
    }

    @Test
    public void notInit() {
        Crashes.trackError(EXCEPTION, null, null);
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());
    }

    @Test
    public void trackErrorWithoutProperties() {
        startCrashes();
        Crashes.trackError(EXCEPTION);
        verify(mChannel).enqueue(mLog.capture(), eq(mCrashes.getGroupName()), eq(DEFAULTS));
        assertTrue(mLog.getValue() instanceof HandledErrorLog);
        HandledErrorLog errorLog = (HandledErrorLog) mLog.getValue();
        assertEquals(EXCEPTION.getMessage(), errorLog.getException().getMessage());
        assertNull(errorLog.getProperties());
    }

    @Test
    public void trackErrorWithInvalidProperties() {
        startCrashes();
        Crashes.trackError(EXCEPTION, new HashMap<String, String>() {{
            put(null, null);
            put("", null);
            put(generateString(ErrorLogHelper.MAX_PROPERTY_ITEM_LENGTH + 1, '*'), null);
            put("1", null);
        }}, null);
        verify(mChannel).enqueue(mLog.capture(), eq(mCrashes.getGroupName()), eq(DEFAULTS));
        assertTrue(mLog.getValue() instanceof HandledErrorLog);
        HandledErrorLog errorLog = (HandledErrorLog) mLog.getValue();
        assertEquals(EXCEPTION.getMessage(), errorLog.getException().getMessage());
        assertNotNull(errorLog.getProperties());
        assertEquals(0, errorLog.getProperties().size());
    }

    @Test
    public void trackErrorWithTooManyProperties() {
        startCrashes();
        Crashes.trackError(EXCEPTION, new HashMap<String, String>() {{
            for (int i = 0; i < 30; i++) {
                put("valid" + i, "valid");
            }
        }}, null);
        verify(mChannel).enqueue(mLog.capture(), eq(mCrashes.getGroupName()), eq(DEFAULTS));
        assertTrue(mLog.getValue() instanceof HandledErrorLog);
        HandledErrorLog errorLog = (HandledErrorLog) mLog.getValue();
        assertEquals(EXCEPTION.getMessage(), errorLog.getException().getMessage());
        assertNotNull(errorLog.getProperties());
        assertEquals(20, errorLog.getProperties().size());
    }

    @Test
    public void trackErrorWithTooLongProperty() {
        startCrashes();
        final String longerMapItem = generateString(ErrorLogHelper.MAX_PROPERTY_ITEM_LENGTH + 1, '*');
        Crashes.trackError(EXCEPTION, new HashMap<String, String>() {{
            put(longerMapItem, longerMapItem);
        }}, null);
        verify(mChannel).enqueue(mLog.capture(), eq(mCrashes.getGroupName()), eq(DEFAULTS));
        assertTrue(mLog.getValue() instanceof HandledErrorLog);
        HandledErrorLog errorLog = (HandledErrorLog) mLog.getValue();
        assertEquals(EXCEPTION.getMessage(), errorLog.getException().getMessage());
        assertNotNull(errorLog.getProperties());
        assertEquals(1, errorLog.getProperties().size());
        Map.Entry<String, String> entry = errorLog.getProperties().entrySet().iterator().next();
        assertEquals(ErrorLogHelper.MAX_PROPERTY_ITEM_LENGTH, entry.getKey().length());
        assertEquals(ErrorLogHelper.MAX_PROPERTY_ITEM_LENGTH, entry.getValue().length());
    }

    @Test
    public void noCallbacksOnHandledErrorLog() {
        startCrashes();
        CrashesListener mockListener = mock(CrashesListener.class);
        mCrashes.setInstanceListener(mockListener);
        Channel.GroupListener channelListener = mCrashes.getChannelListener();
        HandledErrorLog errorLogLog = mock(HandledErrorLog.class);
        channelListener.onBeforeSending(errorLogLog);
        channelListener.onSuccess(errorLogLog);
        channelListener.onFailure(errorLogLog, EXCEPTION);
        verifyNoInteractions(mockListener);
    }

    @Test
    public void noCallbacksOnErrorAttachmentLog() {
        startCrashes();
        CrashesListener mockListener = mock(CrashesListener.class);
        mCrashes.setInstanceListener(mockListener);
        Channel.GroupListener channelListener = mCrashes.getChannelListener();
        ErrorAttachmentLog attachmentLog = mock(ErrorAttachmentLog.class);
        channelListener.onBeforeSending(attachmentLog);
        channelListener.onSuccess(attachmentLog);
        channelListener.onFailure(attachmentLog, EXCEPTION);
        verifyNoInteractions(mockListener);
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
        verify(mChannel).enqueue(mLog.capture(), eq(mCrashes.getGroupName()), eq(DEFAULTS));
        assertTrue(mLog.getValue() instanceof HandledErrorLog);
        HandledErrorLog errorLog = (HandledErrorLog) mLog.getValue();
        assertEquals(exception, errorLog.getException());
        assertNull(errorLog.getProperties());
        reset(mChannel);

        WrapperSdkExceptionManager.trackException(exception, new HashMap<String, String>() {{
            put(null, null);
            put("", null);
            put(generateString(ErrorLogHelper.MAX_PROPERTY_ITEM_LENGTH + 1, '*'), null);
            put("1", null);
        }}, null);
        verify(mChannel).enqueue(mLog.capture(), eq(mCrashes.getGroupName()), eq(DEFAULTS));
        assertTrue(mLog.getValue() instanceof HandledErrorLog);
        errorLog = (HandledErrorLog) mLog.getValue();
        assertEquals(exception, errorLog.getException());
        assertNotNull(errorLog.getProperties());
        assertEquals(0, errorLog.getProperties().size());
        reset(mChannel);

        WrapperSdkExceptionManager.trackException(exception, new HashMap<String, String>() {{
            for (int i = 0; i < 30; i++) {
                put("valid" + i, "valid");
            }
        }}, null);
        verify(mChannel).enqueue(mLog.capture(), eq(mCrashes.getGroupName()), eq(DEFAULTS));
        assertTrue(mLog.getValue() instanceof HandledErrorLog);
        errorLog = (HandledErrorLog) mLog.getValue();
        assertEquals(exception, errorLog.getException());
        assertNotNull(errorLog.getProperties());
        assertEquals(20, errorLog.getProperties().size());
        reset(mChannel);

        final String longerMapItem = generateString(ErrorLogHelper.MAX_PROPERTY_ITEM_LENGTH + 1, '*');
        WrapperSdkExceptionManager.trackException(exception, new HashMap<String, String>() {{
            put(longerMapItem, longerMapItem);
        }}, null);
        verify(mChannel).enqueue(mLog.capture(), eq(mCrashes.getGroupName()), eq(DEFAULTS));
        assertTrue(mLog.getValue() instanceof HandledErrorLog);
        errorLog = (HandledErrorLog) mLog.getValue();
        assertEquals(exception, errorLog.getException());
        assertNotNull(errorLog.getProperties());
        assertEquals(1, errorLog.getProperties().size());
        Map.Entry<String, String> entry = errorLog.getProperties().entrySet().iterator().next();
        assertEquals(ErrorLogHelper.MAX_PROPERTY_ITEM_LENGTH, entry.getKey().length());
        assertEquals(ErrorLogHelper.MAX_PROPERTY_ITEM_LENGTH, entry.getValue().length());
    }

    @Test
    public void trackErrorWithUserId() {
        startCrashes();
        UserIdContext.getInstance().setUserId("charlie");
        Crashes.trackError(EXCEPTION);
        ArgumentCaptor<HandledErrorLog> log = ArgumentCaptor.forClass(HandledErrorLog.class);
        verify(mChannel).enqueue(log.capture(), eq(mCrashes.getGroupName()), eq(DEFAULTS));
        assertNotNull(log.getValue());
        assertEquals(EXCEPTION.getMessage(), log.getValue().getException().getMessage());
        assertEquals("charlie", log.getValue().getUserId());
    }

    @Test
    public void trackErrorWithOneAttachment() {

        /* If we start crashes. */
        startCrashes();

        /* When we track error with an attachment. */
        ErrorAttachmentLog textAttachment = ErrorAttachmentLog.attachmentWithText("text", null);
        Crashes.trackError(EXCEPTION, null, Collections.singleton(textAttachment));

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
    public void trackErrorWithEverything() {

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
        Crashes.trackError(EXCEPTION, properties, Arrays.asList(textAttachment, binaryAttachment));

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
