package com.microsoft.azure.mobile.channel;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.microsoft.azure.mobile.http.ServiceCallback;
import com.microsoft.azure.mobile.ingestion.models.Device;
import com.microsoft.azure.mobile.ingestion.models.Log;
import com.microsoft.azure.mobile.persistence.DatabasePersistenceAsync;
import com.microsoft.azure.mobile.utils.DeviceInfoHelper;
import com.microsoft.azure.mobile.utils.HandlerUtils;
import com.microsoft.azure.mobile.utils.IdHelper;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.UUIDUtils;

import org.junit.Before;
import org.junit.Rule;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.Returns;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.util.ArrayList;

import static com.microsoft.azure.mobile.persistence.DatabasePersistenceAsync.THREAD_NAME;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("WeakerAccess")
@PrepareForTest({DefaultChannel.class, IdHelper.class, DeviceInfoHelper.class, DatabasePersistenceAsync.class, MobileCenterLog.class, HandlerUtils.class})
public class AbstractDefaultChannelTest {

    static final String TEST_GROUP = "group_test";

    static final long BATCH_TIME_INTERVAL = 500;

    static final int MAX_PARALLEL_BATCHES = 3;

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    protected Handler mHandler;

    static Answer<String> getGetLogsAnswer() {
        return getGetLogsAnswer(-1);
    }

    static Answer<String> getGetLogsAnswer(final int size) {
        return new Answer<String>() {

            @Override
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[2] instanceof ArrayList) {
                    ArrayList logs = (ArrayList) args[2];
                    int length = size >= 0 ? size : (int) args[1];
                    for (int i = 0; i < length; i++) {
                        logs.add(mock(Log.class));
                    }
                }
                return UUIDUtils.randomUUID().toString();
            }
        };
    }

    static Answer<Object> getSendAsyncAnswer() {
        return getSendAsyncAnswer(null);
    }

    static Answer<Object> getSendAsyncAnswer(final Exception e) {
        return new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[3] instanceof ServiceCallback) {
                    if (e == null)
                        ((ServiceCallback) invocation.getArguments()[3]).onCallSucceeded("");
                    else
                        ((ServiceCallback) invocation.getArguments()[3]).onCallFailed(e);
                }
                return null;
            }
        };
    }

    @Before
    public void setUp() throws Exception {
        mockStatic(MobileCenterLog.class);
        mockStatic(IdHelper.class, new Returns(UUIDUtils.randomUUID()));
        mockStatic(DeviceInfoHelper.class);
        when(DeviceInfoHelper.getDeviceInfo(any(Context.class))).thenReturn(mock(Device.class));
        mHandler = mock(Handler.class);
        whenNew(Handler.class).withParameterTypes(Looper.class).withArguments(Looper.getMainLooper()).thenReturn(mHandler);

        /* Mock handler for asynchronous Persistence */
        HandlerThread mockHandlerThread = mock(HandlerThread.class);
        Looper mockLooper = mock(Looper.class);
        whenNew(HandlerThread.class).withArguments(THREAD_NAME).thenReturn(mockHandlerThread);
        when(mockHandlerThread.getLooper()).thenReturn(mockLooper);
        Handler mockPersistenceHandler = mock(Handler.class);
        whenNew(Handler.class).withArguments(mockLooper).thenReturn(mockPersistenceHandler);
        when(mockPersistenceHandler.post(any(Runnable.class))).then(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return true;
            }
        });
        mockStatic(HandlerUtils.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));
    }
}
