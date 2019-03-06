package com.microsoft.appcenter.storage;

import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.AppCenterHandler;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.http.HttpClientRetryer;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.PrefStorageConstants;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.storage.FileManager;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Before;
import org.junit.Rule;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@PrepareForTest({
        Storage.class,
        SystemClock.class,
        SharedPreferencesManager.class,
        FileManager.class,
        AppCenterLog.class,
        AppCenter.class,
        HandlerUtils.class,
        HttpUtils.class
})

//@RunWith(PowerMockRunner.class)
abstract public class AbstractStorageTest {

    static final String STORAGE_ENABLED_KEY = PrefStorageConstants.KEY_ENABLED + "_" + Storage.getInstance().getServiceName();

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();
    @Mock
    protected HttpClientRetryer mHttpClient;
    @Mock
    AppCenterHandler mAppCenterHandler;
    Channel mChannel;
    Storage mStorage;
    @Mock
    private AppCenterFuture<Boolean> mCoreEnabledFuture;

    @Before
    public void setUp() throws Exception {
        Storage.unsetInstance();
        mockStatic(SystemClock.class);
        mockStatic(AppCenterLog.class);
        mockStatic(AppCenter.class);
        when(AppCenter.getLogLevel()).thenReturn(Log.WARN);
        when(AppCenter.isConfigured()).thenReturn(true);
        when(AppCenter.getInstance()).thenReturn(mock(AppCenter.class));
        when(AppCenter.isEnabled()).thenReturn(mCoreEnabledFuture);
        when(mCoreEnabledFuture.get()).thenReturn(true);
        Answer<Void> runNow = new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        };
        doAnswer(runNow).when(mAppCenterHandler).post(any(Runnable.class), any(Runnable.class));
        mockStatic(HandlerUtils.class);
        doAnswer(runNow).when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));

        /* First call to com.microsoft.appcenter.AppCenter.isEnabled shall return true, initial state. */
        mockStatic(SharedPreferencesManager.class);
        when(SharedPreferencesManager.getBoolean(anyString(), eq(true))).thenReturn(true);

        /* Then simulate further changes to state. */
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) {

                /* Whenever the new state is persisted, make further calls return the new state. */
                String key = (String) invocation.getArguments()[0];
                boolean enabled = (Boolean) invocation.getArguments()[1];
                when(SharedPreferencesManager.getBoolean(eq(key), eq(true))).thenReturn(enabled);
                return null;
            }
        }).when(SharedPreferencesManager.class);
        SharedPreferencesManager.putBoolean(anyString(), anyBoolean());

        /* Mock file storage. */
        mockStatic(FileManager.class);
        mHttpClient = Mockito.mock(HttpClientRetryer.class);
        whenNew(HttpClientRetryer.class).withAnyArguments().thenReturn(mHttpClient);
        when(SharedPreferencesManager.getBoolean(STORAGE_ENABLED_KEY, true)).thenReturn(true);
        mStorage = Storage.getInstance();
        Storage storage = Storage.getInstance();
        mChannel = start(storage);
        Storage.setApiUrl("default");
    }

    @NonNull
    private Channel start(Storage storage) {
        Channel channel = Mockito.mock(Channel.class);
        storage.onStarting(mAppCenterHandler);
        storage.onStarted(Mockito.mock(Context.class), channel, "", null, true);
        return channel;
    }
}
