package com.microsoft.azure.mobile;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.PrefStorageConstants;
import com.microsoft.azure.mobile.utils.UUIDUtils;
import com.microsoft.azure.mobile.utils.async.DefaultSimpleFuture;
import com.microsoft.azure.mobile.utils.async.SimpleConsumer;
import com.microsoft.azure.mobile.utils.async.SimpleFuture;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

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
public class MobileCenterAndroidTest {

    private Application mApplication;

    @Before
    public void setUp() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        MobileCenter.unsetInstance();
        Constants.APPLICATION_DEBUGGABLE = false;
        mApplication = Instrumentation.newApplication(Application.class, InstrumentationRegistry.getTargetContext());
    }

    @After
    public void tearDown() {
        MobileCenter.setEnabled(true);
    }

    @Test
    public void getInstallId() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        assertNull(MobileCenter.getInstallId().get());
        StorageHelper.initialize(mApplication);
        StorageHelper.PreferencesStorage.remove(PrefStorageConstants.KEY_INSTALL_ID);
        MobileCenter.start(mApplication, UUIDUtils.randomUUID().toString(), DummyService.class);
        UUID installId = MobileCenter.getInstallId().get();
        assertNotNull(installId);
        assertEquals(installId, MobileCenter.getInstallId().get());
        assertEquals(installId, DummyService.getInstallId().get());
        StorageHelper.PreferencesStorage.remove(PrefStorageConstants.KEY_INSTALL_ID);
        final UUID installId2 = MobileCenter.getInstallId().get();
        assertNotNull(installId2);
        assertNotEquals(installId2, installId);
        final Semaphore lock = new Semaphore(0);
        final AtomicReference<UUID> asyncUUID = new AtomicReference<>();
        MobileCenter.getInstallId().thenAccept(new SimpleConsumer<UUID>() {

            @Override
            public void apply(UUID uuid) {
                asyncUUID.set(uuid);
                lock.release();
            }
        });
        lock.acquireUninterruptibly();
        assertEquals(installId2, asyncUUID.get());
        MobileCenter.setEnabled(false);
        assertNull(MobileCenter.getInstallId().get());
    }

    @Test
    public void setDefaultLogLevelDebug() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        MobileCenterLog.setLogLevel(Log.ASSERT);
        MobileCenter.start(mApplication, UUIDUtils.randomUUID().toString());
        assertEquals(Log.WARN, MobileCenter.getLogLevel());
    }

    private static class DummyService extends AbstractMobileCenterService {

        private static DummyService sInstance = new DummyService();

        private static UUID mInstallId;

        static SimpleFuture<UUID> getInstallId() {
            final DefaultSimpleFuture<UUID> future = new DefaultSimpleFuture<>();
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
        public synchronized void onStarted(@NonNull Context context, @NonNull String appSecret, @NonNull Channel channel) {
            super.onStarted(context, appSecret, channel);

            /* Check no dead lock if we do that. */
            mInstallId = MobileCenter.getInstallId().get();
        }
    }
}
