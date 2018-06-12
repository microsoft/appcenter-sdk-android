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
import com.microsoft.appcenter.utils.async.AppCenterConsumer;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;
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

@SuppressWarnings("unused")
public class AppCenterAndroidTest {

    private Application mApplication;

    @Before
    public void setUp() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        AppCenter.unsetInstance();
        Constants.APPLICATION_DEBUGGABLE = false;
        mApplication = Instrumentation.newApplication(Application.class, InstrumentationRegistry.getTargetContext());
    }

    @After
    public void tearDown() {
        AppCenter.setEnabled(true).get();
    }

    @Test
    public void getInstallId() {
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
    public void setDefaultLogLevelDebug() {
        AppCenterLog.setLogLevel(Log.ASSERT);
        AppCenter.start(mApplication, UUIDUtils.randomUUID().toString());
        assertEquals(Log.WARN, AppCenter.getLogLevel());
    }

    private static class DummyService extends AbstractAppCenterService {

        private static final DummyService sInstance = new DummyService();

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

        public static DummyService getInstance() {
            return sInstance;
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
        public synchronized void onStarted(@NonNull Context context, String appSecret, String transmissionTargetToken, @NonNull Channel channel) {
            super.onStarted(context, appSecret, transmissionTargetToken, channel);

            /* Check no dead lock if we do that. */
            mInstallId = AppCenter.getInstallId().get();
        }
    }
}
