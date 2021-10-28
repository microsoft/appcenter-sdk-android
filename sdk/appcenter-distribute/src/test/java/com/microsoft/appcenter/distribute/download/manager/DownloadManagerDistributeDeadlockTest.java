package com.microsoft.appcenter.distribute.download.manager;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.widget.Toast;

import com.microsoft.appcenter.AppCenterHandler;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.distribute.Distribute;
import com.microsoft.appcenter.distribute.InstallerUtils;
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
import org.junit.runners.model.TestTimedOutException;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.powermock.reflect.Whitebox;

import java.lang.reflect.Constructor;
import java.util.Random;
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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;


@PrepareForTest({
        SharedPreferencesManager.class,
        AppCenterLog.class,
        AppNameHelper.class,
        Distribute.class,
        HandlerUtils.class,
        InstallerUtils.class,
        Toast.class,
        Uri.class
})
public class DownloadManagerDistributeDeadlockTest {

    private static final long DOWNLOAD_ID = 42;

    private static final String LOCAL_FILENAME_PATH_MOCK = "ANSWER_IS_42";

    private static final String DISTRIBUTE_ENABLED_KEY = KEY_ENABLED + "_Distribute";

    private static final int MIN_TIMEOUT = 5000;

    private static final int RANDOM_MS_BOUND = 5;

    /**
     * Custom timeout rule here to be able to test expected timeouts.
     */
    @Rule
    public Timeout mTimeout = new Timeout(MIN_TIMEOUT, TimeUnit.MILLISECONDS) {
        public Statement apply(Statement base, final Description description) {

            /* We use the constructor because we can't use the builder and override evaluate method at the same time. */
            return new FailOnTimeout(base, MIN_TIMEOUT) {
                @Override
                public void evaluate() throws Throwable {
                    try {
                        super.evaluate();
                    } catch (TestTimedOutException e) {
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
    Cursor mCursor;

    @Mock
    Activity mActivity;

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

    @Mock
    Toast mToast;

    @Mock
    Uri mUri;

    @Mock
    Intent mInstallIntent;

    DownloadManagerReleaseDownloader mReleaseDownloader;

    @Before
    public void setUp() throws Exception {

        /* 'unsetInstance' has package-private visibility. We don't need to break it for the test. */
        Whitebox.invokeMethod(Distribute.class, "unsetInstance");

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

        /* Toast. */
        mockStatic(Toast.class);
        when(Toast.makeText(any(Context.class), anyInt(), anyInt())).thenReturn(mToast);

        /* Mock Uri. */
        when(mUri.toString()).thenReturn(LOCAL_FILENAME_PATH_MOCK);
        when(mUri.getPath()).thenReturn(LOCAL_FILENAME_PATH_MOCK);
        when(mUri.getEncodedPath()).thenReturn(LOCAL_FILENAME_PATH_MOCK);
        mockStatic(Uri.class);
        when(Uri.parse(anyString())).thenReturn(mUri);

        /* Mock Install Intent. */
        when(mInstallIntent.getData()).thenReturn(mUri);
        when(mInstallIntent.resolveActivity(eq(mPackageManager))).thenReturn(mock(ComponentName.class));
        mockStatic(InstallerUtils.class);
        PowerMockito.when(InstallerUtils.class, "getInstallIntent", Matchers.<Object[]>any()).thenReturn(mInstallIntent);

        /* Mock app name and other string resources. */
        mockStatic(AppNameHelper.class);
        when(AppNameHelper.getAppName(mContext)).thenReturn("unit-test-app");
        when(mContext.getString(R.string.appcenter_distribute_update_dialog_message_optional)).thenReturn("%s%s%d");
        when(mContext.getString(R.string.appcenter_distribute_update_dialog_message_mandatory)).thenReturn("%s%s%d");
        when(mContext.getString(R.string.appcenter_distribute_install_ready_message)).thenReturn("%s%s%d");

        /* Mock release details. */
        when(mReleaseDetails.getVersion()).thenReturn(1);
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(false);

        /* Init release downloader listener. The implementation is package private, therefore we use reflection here. */
        Constructor<?> listenerConstructor = Whitebox.getConstructor(Class.forName("com.microsoft.appcenter.distribute.ReleaseDownloadListener"), Context.class, ReleaseDetails.class);
        listenerConstructor.setAccessible(true);
        ReleaseDownloader.Listener releaseDownloadListener = (ReleaseDownloader.Listener) listenerConstructor.newInstance(mContext, mReleaseDetails);

        /* Init release downloader. */
//        mReleaseDownloader = new DownloadManagerReleaseDownloader(mContext, mReleaseDetails, releaseDownloadListener);
        Whitebox.setInternalState(mReleaseDownloader, "mDownloadId", DOWNLOAD_ID);

        /* Simulate we already have release details initialized, otherwise we would have to implement it naturally and the test would become unnecessarily large. */
        Whitebox.setInternalState(Distribute.getInstance(), "mReleaseDetails", mReleaseDetails);
        Whitebox.setInternalState(Distribute.getInstance(), "mReleaseDownloader", mReleaseDownloader);
    }

    @Test
    public void onDownloadStartedTest() throws Exception {
        testDeadlock(true, new Runnable() {

            @Override
            public void run() {
                mReleaseDownloader.onDownloadStarted(DOWNLOAD_ID, 0);
            }
        });
    }

    @Test
    @TimeoutExpected
    public void onDownloadStartedDeadlocked() throws Exception {

        /* Deadlock simulation. The same can be used for other callbacks of ReleaseDownloadListener. */
        testDeadlock(false, new Runnable() {

            @Override
            public void run() {
                mReleaseDownloader.onDownloadStarted(DOWNLOAD_ID, 0);
            }
        });
    }

    @Test
    public void onDownloadErrorTest() throws Exception {
        testDeadlock(true, new Runnable() {

            @Override
            public void run() {
                mReleaseDownloader.onDownloadError(new RuntimeException("test"));
            }
        });
    }

    public void testDeadlock(final boolean handlerOn, final Runnable backgroundCall) throws Exception {

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
                    randomPause();
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
                    backgroundCall.run();
                    randomPause();
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

    /**
     * Random pause to simulate deadlock timing.
     */
    private void randomPause() {
        try {
            int millis = new Random().nextInt(RANDOM_MS_BOUND);
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Wait for another thread to reach this point, to simulate deadlock timing.
     */
    private void waitAnotherThreadAndStartExecution(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (BrokenBarrierException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
