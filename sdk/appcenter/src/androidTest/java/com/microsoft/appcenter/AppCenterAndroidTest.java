package com.microsoft.appcenter;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.PrefStorageConstants;
import com.microsoft.appcenter.utils.UUIDUtils;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.storage.StorageHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unused")
public class AppCenterAndroidTest {

    private static final String DUMMY_APP_SECRET = "123e4567-e89b-12d3-a456-426655440000";

    private static final String DUMMY_TRANSMISSION_TARGET_TOKEN = "snfbse234jknf";

    private Application mApplication;

    @Before
    public void setUp() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        AppCenter.unsetInstance();
        DummyService.sharedInstance = null;
        Constants.APPLICATION_DEBUGGABLE = false;
        mApplication = Instrumentation.newApplication(Application.class, InstrumentationRegistry.getTargetContext());
    }

    @After
    public void tearDown() {
        AppCenter.setEnabled(true).get();
    }

    @Test
    public void getInstallId() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        assertNull(AppCenter.getInstallId().get());
        StorageHelper.initialize(mApplication);
        StorageHelper.PreferencesStorage.remove(PrefStorageConstants.KEY_INSTALL_ID);
        AppCenter.start(mApplication, UUIDUtils.randomUUID().toString(), DummyService.class);
        UUID installId = AppCenter.getInstallId().get();
        assertNotNull(installId);
        assertEquals(installId, AppCenter.getInstallId().get());
        assertEquals(installId, DummyService.getInstallId().get());
        StorageHelper.PreferencesStorage.remove(PrefStorageConstants.KEY_INSTALL_ID);
        final UUID installId2 = AppCenter.getInstallId().get();
        assertNotNull(installId2);
        assertNotEquals(installId2, installId);
        final Semaphore lock = new Semaphore(0);
        final AtomicReference<UUID> asyncUUID = new AtomicReference<>();
        AppCenter.getInstallId().thenAccept(new AppCenterConsumer<UUID>() {

            @Override
            public void accept(UUID uuid) {
                asyncUUID.set(uuid);
                lock.release();
            }
        });
        lock.acquireUninterruptibly();
        assertEquals(installId2, asyncUUID.get());
        AppCenter.setEnabled(false);
        assertNull(AppCenter.getInstallId().get());
    }

    @Test
    public void setDefaultLogLevelDebug() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        AppCenterLog.setLogLevel(Log.ASSERT);
        AppCenter.start(mApplication, UUIDUtils.randomUUID().toString());
        assertEquals(Log.WARN, AppCenter.getLogLevel());
    }

    @Test
    public void appSecretWithDelimiter() {
        String secret = DUMMY_APP_SECRET + AppCenter.PAIR_DELIMITER;
        AppCenter.start(mApplication, secret, DummyService.class);
        assertTrue(AppCenter.isEnabled().get());
        verify(DummyService.getInstance()).onStarted(any(Context.class), eq(DUMMY_APP_SECRET), isNull(String.class), any(Channel.class));
    }

    @Test
    public void appSecretWithTargetTokenTest() {
        String secret = DUMMY_APP_SECRET + AppCenter.PAIR_DELIMITER + AppCenter.TRANSMISSION_TARGET_TOKEN_KEY + AppCenter.KEY_VALUE_DELIMITER + DUMMY_TRANSMISSION_TARGET_TOKEN;
        AppCenter.start(mApplication, secret, DummyService.class);
        assertTrue(AppCenter.isEnabled().get());
        verify(DummyService.getInstance()).onStarted(any(Context.class), eq(DUMMY_APP_SECRET), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), any(Channel.class));
    }

    @Test
    public void keyedAppSecretTest() {
        String secret = AppCenter.APP_SECRET_KEY + AppCenter.KEY_VALUE_DELIMITER + DUMMY_APP_SECRET;
        AppCenter.start(mApplication, secret, DummyService.class);
        assertTrue(AppCenter.isEnabled().get());
        verify(DummyService.getInstance()).onStarted(any(Context.class), eq(DUMMY_APP_SECRET), isNull(String.class), any(Channel.class));
    }

    @Test
    public void keyedAppSecretWithDelimiterTest() {
        String secret = AppCenter.APP_SECRET_KEY + AppCenter.KEY_VALUE_DELIMITER + DUMMY_APP_SECRET + AppCenter.PAIR_DELIMITER;
        AppCenter.start(mApplication, secret, DummyService.class);
        assertTrue(AppCenter.isEnabled().get());
        verify(DummyService.getInstance()).onStarted(any(Context.class), eq(DUMMY_APP_SECRET), isNull(String.class), any(Channel.class));
    }

    @Test
    public void keyedAppSecretWithTargetTokenTest() {
        String secret = AppCenter.APP_SECRET_KEY + AppCenter.KEY_VALUE_DELIMITER + DUMMY_APP_SECRET + AppCenter.PAIR_DELIMITER + AppCenter.TRANSMISSION_TARGET_TOKEN_KEY + AppCenter.KEY_VALUE_DELIMITER + DUMMY_TRANSMISSION_TARGET_TOKEN;
        AppCenter.start(mApplication, secret, DummyService.class);
        assertTrue(AppCenter.isEnabled().get());
        verify(DummyService.getInstance()).onStarted(any(Context.class), eq(DUMMY_APP_SECRET), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), any(Channel.class));
    }

    @Test
    public void targetTokenTest() {
        String secret = AppCenter.TRANSMISSION_TARGET_TOKEN_KEY + AppCenter.KEY_VALUE_DELIMITER + DUMMY_TRANSMISSION_TARGET_TOKEN;
        AppCenter.start(mApplication, secret, DummyService.class);
        assertTrue(AppCenter.isEnabled().get());
        verify(DummyService.getInstance()).onStarted(any(Context.class), isNull(String.class), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), any(Channel.class));
    }

    @Test
    public void targetTokenWithAppSecretTest() {
        String secret = AppCenter.TRANSMISSION_TARGET_TOKEN_KEY + AppCenter.KEY_VALUE_DELIMITER + DUMMY_TRANSMISSION_TARGET_TOKEN + AppCenter.PAIR_DELIMITER + DUMMY_APP_SECRET;
        AppCenter.start(mApplication, secret, DummyService.class);
        assertTrue(AppCenter.isEnabled().get());
        verify(DummyService.getInstance()).onStarted(any(Context.class), eq(DUMMY_APP_SECRET), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), any(Channel.class));
    }

    @Test
    public void targetTokenWithUnKeyedAppSecretTest() {
        String secret = AppCenter.TRANSMISSION_TARGET_TOKEN_KEY + AppCenter.KEY_VALUE_DELIMITER + DUMMY_TRANSMISSION_TARGET_TOKEN +  AppCenter.PAIR_DELIMITER + AppCenter.APP_SECRET_KEY + AppCenter.KEY_VALUE_DELIMITER + DUMMY_APP_SECRET;
        AppCenter.start(mApplication, secret, DummyService.class);
        assertTrue(AppCenter.isEnabled().get());
        verify(DummyService.getInstance()).onStarted(any(Context.class), eq(DUMMY_APP_SECRET), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), any(Channel.class));
    }

    public static class DummyService extends AbstractAppCenterService {

        private static DummyService sharedInstance;

        private static UUID mInstallId;

        static AppCenterFuture<UUID> getInstallId() {
            final DefaultAppCenterFuture<UUID> future = new DefaultAppCenterFuture<>();
            getInstance().post(new Runnable() {

                @Override
                public void run() {
                    future.complete(mInstallId);
                }
            });
            return future;
        }

        @SuppressWarnings("WeakerAccess")
        public static DummyService getInstance() {
            if (sharedInstance == null) {
                sharedInstance = spy(new DummyService());
            }
            return sharedInstance;
        }

        @Override
        public String getServiceName() {
            return "Dummy";
        }

        @Override
        protected String getGroupName() {
            return null;
        }

        @Override
        protected String getLoggerTag() {
            return null;
        }

        @Override
        public synchronized void onStarted(@NonNull Context context, String appSecret, String transmissionTargetToken,  @NonNull Channel channel) {
            super.onStarted(context, appSecret, null, channel);

            /* Check no dead lock if we do that. */
            mInstallId = AppCenter.getInstallId().get();
        }
    }
}
