/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.microsoft.appcenter.channel.DefaultChannel;
import com.microsoft.appcenter.ingestion.models.StartServiceLog;
import com.microsoft.appcenter.ingestion.models.json.JSONUtils;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.IdHelper;
import com.microsoft.appcenter.utils.InstrumentationRegistryHelper;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.ShutdownHelper;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.storage.DatabaseManager;
import com.microsoft.appcenter.utils.storage.FileManager;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.util.HashMap;
import java.util.Map;

import static com.microsoft.appcenter.AppCenter.KEY_VALUE_DELIMITER;
import static com.microsoft.appcenter.AppCenter.TRANSMISSION_TARGET_TOKEN_KEY;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@PrepareForTest({
        AppCenter.class,
        UncaughtExceptionHandler.class,
        DefaultChannel.class,
        Constants.class,
        AppCenterLog.class,
        StartServiceLog.class,
        FileManager.class,
        SharedPreferencesManager.class,
        IdHelper.class,
        DeviceInfoHelper.class,
        Thread.class,
        ShutdownHelper.class,
        CustomProperties.class,
        InstrumentationRegistryHelper.class,
        NetworkStateHelper.class,
        JSONUtils.class
})
public class AbstractAppCenterTest {

    static final String DUMMY_APP_SECRET = "123e4567-e89b-12d3-a456-426655440000";

    static final String DUMMY_TRANSMISSION_TARGET_TOKEN = "snfbse234jknf";

    static final String DUMMY_TARGET_TOKEN_STRING = TRANSMISSION_TARGET_TOKEN_KEY + KEY_VALUE_DELIMITER + DUMMY_TRANSMISSION_TARGET_TOKEN;

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    DefaultChannel mChannel;

    @Mock
    NetworkStateHelper mNetworkStateHelper;

    @Mock
    StartServiceLog mStartServiceLog;

    @Mock
    Application mApplication;

    ApplicationInfo mApplicationInfo;

    static void addArgumentToRegistry(String value) {
        Bundle mockBundle = mock(Bundle.class);
        when(mockBundle.getString(ServiceInstrumentationUtils.DISABLE_SERVICES)).thenReturn(value);
        when(InstrumentationRegistryHelper.getArguments()).thenReturn(mockBundle);
    }

    static void addRunningInAppCenterToRegistry(String value) {
        Bundle mockBundle = mock(Bundle.class);
        when(mockBundle.getString(AppCenter.RUNNING_IN_APP_CENTER)).thenReturn(value);
        when(InstrumentationRegistryHelper.getArguments()).thenReturn(mockBundle);
    }

    @Before
    public void setUp() throws Exception {
        AppCenter.unsetInstance();
        DummyService.sharedInstance = null;
        AnotherDummyService.sharedInstance = null;

        whenNew(DefaultChannel.class).withAnyArguments().thenReturn(mChannel);
        whenNew(StartServiceLog.class).withAnyArguments().thenReturn(mStartServiceLog);

        when(mApplication.getApplicationContext()).thenReturn(mApplication);
        mApplicationInfo = new ApplicationInfo();
        mApplicationInfo.flags = ApplicationInfo.FLAG_DEBUGGABLE;
        when(mApplication.getApplicationInfo()).thenReturn(mApplicationInfo);

        mockStatic(Constants.class);
        mockStatic(AppCenterLog.class);
        mockStatic(FileManager.class);
        mockStatic(SharedPreferencesManager.class);
        mockStatic(IdHelper.class);
        mockStatic(Thread.class);
        mockStatic(ShutdownHelper.class);
        mockStatic(DeviceInfoHelper.class);
        mockStatic(InstrumentationRegistryHelper.class);
        mockStatic(NetworkStateHelper.class);
        mockStatic(JSONUtils.class);

        /* Mock handlers. */
        Handler handler = mock(Handler.class);
        whenNew(Handler.class).withAnyArguments().thenReturn(handler);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(handler).post(any(Runnable.class));
        HandlerThread handlerThread = mock(HandlerThread.class);
        whenNew(HandlerThread.class).withAnyArguments().thenReturn(handlerThread);
        when(handlerThread.getLooper()).thenReturn(mock(Looper.class));
        addArgumentToRegistry(null);

        /* First call to com.microsoft.appcenter.AppCenter.isEnabled shall return true, initial state. */
        when(SharedPreferencesManager.getBoolean(anyString(), eq(true))).thenReturn(true);

        /* Then simulate further changes to state. */
        PowerMockito.doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {

                /* Whenever the new state is persisted, make further calls return the new state. */
                String key = (String) invocation.getArguments()[0];
                boolean enabled = (Boolean) invocation.getArguments()[1];
                when(SharedPreferencesManager.getBoolean(key, true)).thenReturn(enabled);
                return null;
            }
        }).when(SharedPreferencesManager.class);
        SharedPreferencesManager.putBoolean(anyString(), anyBoolean());

        /* Mock empty database. */
        DatabaseManager databaseManager = mock(DatabaseManager.class);
        whenNew(DatabaseManager.class).withAnyArguments().thenReturn(databaseManager);
        when(databaseManager.getCursor(any(SQLiteQueryBuilder.class), any(String[].class), any(String[].class), anyString()))
                .thenReturn(mock(Cursor.class));

        /* Mock network state helper. */
        when(NetworkStateHelper.getSharedInstance(any(Context.class))).thenReturn(mNetworkStateHelper);
    }

    @After
    public void tearDown() {
        Constants.APPLICATION_DEBUGGABLE = false;
    }

    static class DummyService extends AbstractAppCenterService {

        private static DummyService sharedInstance;

        @SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
        public static DummyService getInstance() {
            if (sharedInstance == null) {
                sharedInstance = spy(new DummyService());
            }
            return sharedInstance;
        }

        static AppCenterFuture<Boolean> isEnabled() {
            return getInstance().isInstanceEnabledAsync();
        }

        @Override
        public boolean isAppSecretRequired() {
            return false;
        }

        @Override
        protected String getGroupName() {
            return "group_dummy";
        }

        @Override
        public String getServiceName() {
            return "Dummy";
        }

        @Override
        protected String getLoggerTag() {
            return "DummyLog";
        }
    }

    static class AnotherDummyService extends AbstractAppCenterService {

        private static AnotherDummyService sharedInstance;

        @SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
        public static AnotherDummyService getInstance() {
            if (sharedInstance == null) {
                sharedInstance = spy(new AnotherDummyService());
            }
            return sharedInstance;
        }

        static AppCenterFuture<Boolean> isEnabled() {
            return getInstance().isInstanceEnabledAsync();
        }

        @Override
        public Map<String, LogFactory> getLogFactories() {
            HashMap<String, LogFactory> logFactories = new HashMap<>();
            logFactories.put("mock", mock(LogFactory.class));
            return logFactories;
        }

        @Override
        protected String getGroupName() {
            return "group_another_dummy";
        }

        @Override
        public String getServiceName() {
            return "AnotherDummy";
        }

        @Override
        protected String getLoggerTag() {
            return "AnotherDummyLog";
        }
    }

    static class InvalidService extends AbstractAppCenterService {

        @Override
        protected String getGroupName() {
            return "group_invalid";
        }

        @Override
        public String getServiceName() {
            return "Invalid";
        }

        @Override
        protected String getLoggerTag() {
            return "InvalidLog";
        }
    }
}
