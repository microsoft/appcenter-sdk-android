package com.microsoft.appcenter.distribute.download.manager;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.microsoft.appcenter.AppCenterHandler;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.distribute.Distribute;
import com.microsoft.appcenter.distribute.R;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AppNameHelper;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.powermock.reflect.Whitebox;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.microsoft.appcenter.distribute.DistributeConstants.INVALID_DOWNLOAD_IDENTIFIER;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_ID;
import static com.microsoft.appcenter.utils.PrefStorageConstants.ALLOWED_NETWORK_REQUEST;
import static com.microsoft.appcenter.utils.PrefStorageConstants.KEY_ENABLED;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;


@PrepareForTest({
        SharedPreferencesManager.class,
        AppCenterLog.class,
        AppNameHelper.class,
        Distribute.class,
        HandlerUtils.class
})
public class DownloadManagerDistributeDeadlockTest {

    private static final long DOWNLOAD_ID = 42;

    private static final String DISTRIBUTE_ENABLED_KEY = KEY_ENABLED + "_Distribute";

    private static final int MIN_TIMEOUT = 5000;

    @Rule
    public Timeout mTimeout = new Timeout(MIN_TIMEOUT, TimeUnit.MILLISECONDS) {
        public Statement apply(Statement base, final Description description) {

            /* We use the constructor because we can't use the builder and override evaluate method at the same time. */
            return new FailOnTimeout(base, MIN_TIMEOUT) {
                @Override
                public void evaluate() throws Throwable {
                    try {
                        super.evaluate();
                    } catch (Throwable e) {
                        System.out.println("Caught timeout exception in: " + description.getMethodName());
                        if (description.getAnnotation(TimeoutExpected.class) != null) {
                            System.out.println("Ignore, timeout expected");
                            return;
                        }
                        System.out.println("Throw timeout exception");
                        throw new TimeoutException();
                    }
                }
            };
        }
    };

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    Context mContext;

    @Mock
    Activity mActivity;

    @Mock
    AlertDialog mDialog;

    @Mock
    PackageManager mPackageManager;

    @Mock
    ApplicationInfo mApplicationInfo;

    @Mock
    AppCenterHandler mAppCenterHandler;

    @Mock
    ReleaseDetails mReleaseDetails;

    @Mock
    Channel mChannel;

    @Before
    public void initDownloader() throws Exception {

        /* 'unsetInstance' has package-private visibility. We don't need to break it for the test. */
        Method unsetInstanceMethod = Distribute.class.getDeclaredMethod("unsetInstance");
        unsetInstanceMethod.setAccessible(true);
        unsetInstanceMethod.invoke(null);

        /* First call to com.microsoft.appcenter.AppCenter.isEnabled shall return true, initial state. */
        mockStatic(SharedPreferencesManager.class);
        when(SharedPreferencesManager.getBoolean(DISTRIBUTE_ENABLED_KEY, true)).thenReturn(true);
        when(SharedPreferencesManager.getBoolean(ALLOWED_NETWORK_REQUEST, true)).thenReturn(true);

        /* Default download id when not found. */
        when(SharedPreferencesManager.getLong(PREFERENCE_KEY_DOWNLOAD_ID, INVALID_DOWNLOAD_IDENTIFIER)).thenReturn(INVALID_DOWNLOAD_IDENTIFIER);

        /* Mock package manager. */
        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getPackageName()).thenReturn("com.contoso");
        when(mActivity.getPackageName()).thenReturn("com.contoso");
        when(mContext.getApplicationInfo()).thenReturn(mApplicationInfo);
        when(mActivity.getApplicationInfo()).thenReturn(mApplicationInfo);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mActivity.getPackageManager()).thenReturn(mPackageManager);
        PackageInfo packageInfo = mock(PackageInfo.class);
        when(mPackageManager.getPackageInfo("com.contoso", 0)).thenReturn(packageInfo);
        Whitebox.setInternalState(packageInfo, "packageName", "com.contoso");
        Whitebox.setInternalState(packageInfo, "versionName", "1.2.3");
        Whitebox.setInternalState(packageInfo, "versionCode", 6);
        Whitebox.setInternalState(packageInfo, "lastUpdateTime", 2);

        /* Mock app name and other string resources. */
        mockStatic(AppNameHelper.class);
        when(AppNameHelper.getAppName(mContext)).thenReturn("unit-test-app");
        when(mContext.getString(R.string.appcenter_distribute_update_dialog_message_optional)).thenReturn("%s%s%d");
        when(mContext.getString(R.string.appcenter_distribute_update_dialog_message_mandatory)).thenReturn("%s%s%d");
        when(mContext.getString(R.string.appcenter_distribute_install_ready_message)).thenReturn("%s%s%d");
    }

    @Test
    @TimeoutExpected
    public void deadlockExpected() throws Exception {
        testDeadlock(false);
    }

    @Test
    public void deadlockFixed() throws Exception {
        testDeadlock(true);
    }

    public void testDeadlock(final boolean handlerOn) throws Exception {

        /* Init release downloader listener. The implementation is package private, therefore we use reflection here. */
        Class<?> downloadListenerClass = Class.forName("com.microsoft.appcenter.distribute.ReleaseDownloadListener");
        Constructor<?> downloadListenerDeclaredConstructor = downloadListenerClass.getDeclaredConstructor(Context.class, ReleaseDetails.class);
        downloadListenerDeclaredConstructor.setAccessible(true);
        ReleaseDownloader.Listener releaseDownloadListener = (ReleaseDownloader.Listener) downloadListenerDeclaredConstructor.newInstance(mContext, mReleaseDetails);

        /* Init release downloader. */
        final DownloadManagerReleaseDownloader mReleaseDownloader = new DownloadManagerReleaseDownloader(mContext, mReleaseDetails, releaseDownloadListener);

        /* Mock release details. */
        when(mReleaseDetails.getVersion()).thenReturn(1);
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(false);

        /* If we don't stub this method, the onActivityResumed thread will try to display the dialog. */
        Distribute distribute = spy(Distribute.getInstance());
        PowerMockito.when(distribute, "shouldRefreshDialog", mDialog).thenReturn(false);

        /* Simulate we already have release details initialized, otherwise we would have to implement it naturally and the test would become unnecessarily large. */
        Field privateStringField = Distribute.getInstance().getClass().getDeclaredField("mReleaseDetails");
        privateStringField.setAccessible(true);
        privateStringField.set(Distribute.getInstance(), mReleaseDetails);
        Field privateStringField2 = Distribute.getInstance().getClass().getDeclaredField("mReleaseDownloader");
        privateStringField2.setAccessible(true);
        privateStringField2.set(Distribute.getInstance(), mReleaseDownloader);

        /* Init the barrier in order to align concurrent threads execution. */
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final int cycles = 50;

        /* We use executor service for the following Android Handler simulation. */
        final ExecutorService uiThreadExecutor = Executors.newSingleThreadExecutor();

        /* Handler Utils. */
        Answer<Void> runNow = new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                if (handlerOn) {

                    /* Submit the runnable to the ui thread executor, mimics HandlerUtils.runOnUiThread behavior. */
                    uiThreadExecutor.submit(runnable);
                } else {

                    /* Run immediately on the same thread. */
                    runnable.run();
                }
                return null;
            }
        };
        mockStatic(HandlerUtils.class);
        doAnswer(runNow).when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));

        /* UI thread scenario. */
        Callable<Object> onActivityResumedCallable = new Callable<Object>() {

            @Override
            public Object call() {

                /* Prepare distribute setup. */
                Distribute.getInstance().onStarting(mAppCenterHandler);
                Distribute.getInstance().onStarted(mContext, mChannel, "a", null, true);
                waitAnotherThreadAndStartExecution(barrier);
                for (int i = 0; i < cycles; i++) {

                    /* The enter point of the deadlock case. */
                    Distribute.getInstance().onActivityResumed(mActivity);
                }
                return null;
            }
        };

        /* Worker thread scenario. */
        Runnable downloadRunnable = new Runnable() {

            @Override
            public void run() {
                waitAnotherThreadAndStartExecution(barrier);
                for (int i = 0; i < cycles; i++) {

                    /* The enter point of the deadlock case. */
                    mReleaseDownloader.onDownloadStarted(DOWNLOAD_ID, 0);
                }
            }
        };
        final Thread backgroundThread = new Thread(downloadRunnable, "background-thread");

        /* Start execution. */
        Future<Object> submit = uiThreadExecutor.submit(onActivityResumedCallable);
        backgroundThread.start();

        /* Wait for the execution to complete. */
        submit.get();
        backgroundThread.join();
    }

    private void waitAnotherThreadAndStartExecution(CyclicBarrier barrier) {
        try {

            /* Wait for another thread to reach this point, to simulate deadlock timing. */
            barrier.await();
        } catch (BrokenBarrierException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}