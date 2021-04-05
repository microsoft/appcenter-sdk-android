/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import android.util.Log;

import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.PrefStorageConstants;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.After;
import org.junit.Assert;
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
        mApplication = Instrumentation.newApplication(Application.class, InstrumentationRegistry.getInstrumentation().getContext());
    }

    @After
    public void tearDown() {
        AppCenter.setEnabled(true).get();
    }

    @Test
    public void getInstallId() {
        assertNull(AppCenter.getInstallId().get());
        SharedPreferencesManager.initialize(mApplication);
        SharedPreferencesManager.remove(PrefStorageConstants.KEY_INSTALL_ID);
        AppCenter.start(mApplication, UUID.randomUUID().toString(), DummyService.class);
        UUID installId = AppCenter.getInstallId().get();
        assertNotNull(installId);
        assertEquals(installId, AppCenter.getInstallId().get());
        assertEquals(installId, DummyService.getInstallId().get());
        SharedPreferencesManager.remove(PrefStorageConstants.KEY_INSTALL_ID);
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
        AppCenter.start(mApplication, UUID.randomUUID().toString());
        assertEquals(Log.WARN, AppCenter.getLogLevel());
    }

    @Test
    public void enableDisable() {
        String appSecret = UUID.randomUUID().toString();
        AppCenter.start(mApplication, appSecret, DummyService.class);

        /* Disable SDK. */
        AppCenter.setEnabled(false);

        /* Verify disabled. */
        final Semaphore lock = new Semaphore(0);
        final AtomicReference<Boolean> isEnabled = new AtomicReference<>();
        AppCenter.isEnabled().thenAccept(new AppCenterConsumer<Boolean>() {

            @Override
            public void accept(Boolean aBoolean) {
                isEnabled.set(aBoolean);
                lock.release();
            }
        });
        lock.acquireUninterruptibly();
        Assert.assertFalse(isEnabled.get());

        /* Restart SDK. */
        AppCenter.unsetInstance();
        AppCenter.start(mApplication, appSecret, DummyService.class);
        final Semaphore lock1 = new Semaphore(0);
        final AtomicReference<Boolean> isEnabled1 = new AtomicReference<>();
        AppCenter.isEnabled().thenAccept(new AppCenterConsumer<Boolean>() {

            @Override
            public void accept(Boolean aBoolean) {
                isEnabled1.set(aBoolean);
                lock1.release();
            }
        });
        lock1.acquireUninterruptibly();

        /* Verify SDK is still disabled. */
        Assert.assertFalse(isEnabled1.get());
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
        public synchronized void onStarted(@NonNull Context context, @NonNull Channel channel, String appSecret, String transmissionTargetToken, boolean startedFromApp) {
            super.onStarted(context, channel, appSecret, transmissionTargetToken, startedFromApp);

            /* Check no dead lock if we do that. */
            mInstallId = AppCenter.getInstallId().get();
        }
    }
}
