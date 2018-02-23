package com.microsoft.appcenter.distribute;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

import com.microsoft.appcenter.test.TestUtils;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.storage.StorageHelper.PreferencesStorage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Semaphore;

import static android.app.DownloadManager.EXTRA_DOWNLOAD_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_ENQUEUED;
import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_NOTIFIED;
import static com.microsoft.appcenter.distribute.DistributeConstants.INVALID_DOWNLOAD_IDENTIFIER;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_STATE;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_TIME;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

public class DistributeDownloadTest extends AbstractDistributeAfterDownloadTest {

    @Before
    public void setUpDownload() throws Exception {
        setUpDownload(false);
    }

    @Test
    public void startDownloadThenDisable() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Verify. */
        verify(mDownloadManager).enqueue(mDownloadRequest);
        verifyNew(DownloadManager.Request.class).withArguments(mDownloadUrl);
        verifyStatic();
        PreferencesStorage.putLong(PREFERENCE_KEY_DOWNLOAD_ID, DOWNLOAD_ID);
        verifyStatic();
        PreferencesStorage.putInt(PREFERENCE_KEY_DOWNLOAD_STATE, DOWNLOAD_STATE_ENQUEUED);

        /* Pause/resume should do nothing excepting mentioning progress. */
        verify(mDialog).show();
        Distribute.getInstance().onActivityPaused(mActivity);
        Distribute.getInstance().onActivityResumed(mActivity);
        verify(mDialog).show();

        /* Cancel download by disabling. */
        Distribute.setEnabled(false);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verify(mDownloadTask.get()).cancel(true);
        verify(mDownloadManager).remove(DOWNLOAD_ID);
        verify(mNotificationManager, never()).notify(anyInt(), any(Notification.class));
    }

    @Test
    public void disableWhileStartingDownload() throws Exception {

        /* Cancel download before async task completes. */
        Distribute.setEnabled(false);
        waitDownloadTask();

        /* Verify. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verify(mDownloadTask.get()).cancel(true);
        verify(mDownloadManager).enqueue(mDownloadRequest);
        verifyNew(DownloadManager.Request.class).withArguments(mDownloadUrl);
        verify(mDownloadManager).remove(DOWNLOAD_ID);

        /* And that we didn't persist the state. */
        verifyStatic(never());
        PreferencesStorage.putLong(PREFERENCE_KEY_DOWNLOAD_ID, DOWNLOAD_ID);
        verifyStatic(never());
        PreferencesStorage.putString(PREFERENCE_KEY_DOWNLOAD_STATE, "");
        verifyZeroInteractions(mNotificationManager);
    }

    @Test
    public void disableWhileProcessingCompletion() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Process download completion. */
        completeDownload();

        /* Disable before completion. */
        Distribute.setEnabled(false);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        waitCheckDownloadTask();

        /* Verify cancellation. */
        verify(mCompletionTask.get()).cancel(true);
        verify(mDownloadManager).remove(DOWNLOAD_ID);
        verifyZeroInteractions(mNotificationManager);

        /* Check cleaned state only once, the completeWorkflow on failed download has to be ignored. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
    }

    @Test
    public void failDownloadRestartNoLauncher() {

        /* Simulate async task. */
        waitDownloadTask();

        /* Process download completion. */
        completeDownload();

        /* Wait. Fails as we dont mock success uri. */
        waitCheckDownloadTask();

        /* Check failure processing. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Nothing should happen if just changing activities. */
        Activity activity = mock(Activity.class);
        Distribute.getInstance().onActivityPaused(activity);
        Distribute.getInstance().onActivityResumed(activity);

        /* Verify download happened only once. */
        verify(mDownloadManager).enqueue(mDownloadRequest);

        /* Exit app. */
        Distribute.getInstance().onActivityPaused(activity);
        Distribute.getInstance().onActivityStopped(activity);
        Distribute.getInstance().onActivityDestroyed(activity);

        /* Recreate activity, we'll cache that there is no launcher since no mock intent. */
        when(activity.getPackageManager()).thenReturn(mock(PackageManager.class));
        Distribute.getInstance().onActivityCreated(activity, null);

        /* So nothing happens since no launcher restart detected. */
        verify(mDownloadManager).enqueue(mDownloadRequest);
    }

    @Test
    public void downloadCursorNull() {

        /* Simulate async task. */
        waitDownloadTask();

        /* Process download completion. */
        completeDownload();

        /* Simulate task. */
        waitCheckDownloadTask();

        /* Check we completed workflow without starting activity because installer not found. */
        verify(mContext, never()).startActivity(any(Intent.class));
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
    }

    @Test
    public void downloadCursorEmpty() {

        /* Simulate async task. */
        waitDownloadTask();

        /* Process download completion. */
        completeDownload();
        Cursor cursor = mock(Cursor.class);
        when(mDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(cursor);

        /* Simulate task. */
        waitCheckDownloadTask();

        /* Check we completed workflow without starting activity because installer not found. */
        verify(cursor).close();
        verify(mContext, never()).startActivity(any(Intent.class));
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
    }

    @Test
    public void successDownloadInstallerNotFoundEvenWithLocalFile() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Process download completion. */
        completeDownload();
        Cursor cursor = mockSuccessCursor();
        whenNew(Intent.class).withArguments(Intent.ACTION_INSTALL_PACKAGE).thenReturn(mock(Intent.class));

        /* Simulate task. */
        waitCheckDownloadTask();

        /* Check we completed workflow without starting activity because installer not found. */
        verify(cursor).close();
        verify(mContext, never()).startActivity(any(Intent.class));
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
    }

    @Test
    public void successDownloadInstallerNotFoundAfterNougat() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Process download completion. */
        completeDownload();
        Cursor cursor = mockSuccessCursor();
        whenNew(Intent.class).withArguments(Intent.ACTION_INSTALL_PACKAGE).thenReturn(mock(Intent.class));
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.N);

        /* Simulate task. */
        waitCheckDownloadTask();

        /* Check we completed workflow without starting activity because installer not found. */
        verify(cursor).close();
        verify(mContext, never()).startActivity(any(Intent.class));
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
    }

    @Test
    public void disableDuringDownload() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Disable. */
        Distribute.setEnabled(false);

        /* We receive intent from download manager when we remove download. */
        verify(mDownloadManager).remove(DOWNLOAD_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        completeDownload();

        /* Simulate task. */
        waitCheckDownloadTask();

        /* Check we completed workflow without starting activity because disabled. */
        verify(mContext, never()).startActivity(any(Intent.class));
        verifyZeroInteractions(mNotificationManager);

        /* Verify state deleted only at disable time. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Verify no release hash+id were saved. */
        verifyStatic(never());
        PreferencesStorage.putString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH), anyString());
        verifyStatic(never());
        PreferencesStorage.putInt(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_ID), anyInt());
        verifyStatic(never());
        PreferencesStorage.putString(eq(PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID), anyString());

        /* Verify enabling triggers update dialog again. */
        verify(mDialog).show();
        Distribute.setEnabled(true);
        verify(mDialog, times(2)).show();
    }

    @Test
    public void successInForeground() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Process download completion. */
        completeDownload();
        Cursor cursor = mockSuccessCursor();
        Intent installIntent = mockInstallIntent();

        /* Simulate task. */
        waitCheckDownloadTask();

        /* Verify start activity and complete workflow. */
        verify(mContext).startActivity(installIntent);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Verify release hash+id were saved. */
        verifyStatic();
        PreferencesStorage.putString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH), anyString());
        verifyStatic();
        PreferencesStorage.putInt(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_ID), anyInt());
        verifyStatic();
        PreferencesStorage.putString(eq(PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID), anyString());
        verifyNoMoreInteractions(mNotificationManager);
        verify(cursor).close();
    }

    @Test
    public void longFailingDownloadForOptionalDownload() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Mock running cursor. */
        Cursor cursor = mock(Cursor.class);
        when(mDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(cursor);
        when(cursor.moveToFirst()).thenReturn(true);
        when(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)).thenReturn(0);
        when(cursor.getInt(0)).thenReturn(DownloadManager.STATUS_RUNNING);

        /* Restart launcher, nothing happens. */
        Intent launcherIntent = mock(Intent.class);
        when(mPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(launcherIntent);
        ComponentName launcher = mock(ComponentName.class);
        when(launcherIntent.resolveActivity(mPackageManager)).thenReturn(launcher);
        when(launcher.getClassName()).thenReturn(mActivity.getClass().getName());
        restartActivity();

        /* Restart app process. Still nothing as background. */
        restartProcessAndSdk();

        /* No download check yet. */
        verifyStatic(never());
        AsyncTaskUtils.execute(anyString(), argThat(sCheckCompleteTask), Mockito.<Void>anyVararg());

        /* Foreground: check still in progress. */
        Distribute.getInstance().onActivityResumed(mActivity);
        waitCheckDownloadTask();
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), argThat(sCheckCompleteTask), Mockito.<Void>anyVararg());
        verify(cursor).close();

        /* Restart launcher. */
        Distribute.getInstance().onActivityPaused(mActivity);
        restartActivity();

        /* Verify we don't run the check again. (Only once). */
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), argThat(sCheckCompleteTask), Mockito.<Void>anyVararg());

        /* Download eventually fails. */
        when(cursor.getInt(0)).thenReturn(DownloadManager.STATUS_FAILED);
        completeDownload();

        /* Simulate task. */
        waitCheckDownloadTask();

        /* Verify we complete workflow on failure. */
        verify(mContext, never()).startActivity(any(Intent.class));
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Verify no release hash+id were saved. */
        verifyStatic(never());
        PreferencesStorage.putString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH), anyString());
        verifyStatic(never());
        PreferencesStorage.putInt(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_ID), anyInt());
        verifyStatic(never());
        PreferencesStorage.putString(eq(PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID), anyString());
        verifyZeroInteractions(mNotificationManager);
    }

    @Test
    public void disabledWhileCheckingDownloadOnRestart() throws BrokenBarrierException, InterruptedException {

        /* Simulate async task. */
        waitDownloadTask();

        /* Mock running cursor. */
        Cursor cursor = mock(Cursor.class);
        when(mDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(cursor);
        when(cursor.moveToFirst()).thenReturn(true);
        when(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)).thenReturn(0);
        when(cursor.getInt(0)).thenReturn(DownloadManager.STATUS_RUNNING);

        /* Restart app process and resume. */
        restartProcessAndSdk();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Change behavior of get download it to block to simulate the concurrency issue. */
        final Semaphore beforeDisabledSemaphore = new Semaphore(0);
        final Semaphore afterDisabledSemaphore = new Semaphore(0);

        /* Call get to execute last when so that we can override the answer for next calls. */
        final long downloadId = PreferencesStorage.getLong(PREFERENCE_KEY_DOWNLOAD_ID, INVALID_DOWNLOAD_IDENTIFIER);

        /* Overwrite next answer. */
        final Thread testThread = Thread.currentThread();
        when(PreferencesStorage.getLong(PREFERENCE_KEY_DOWNLOAD_ID, INVALID_DOWNLOAD_IDENTIFIER)).then(new Answer<Long>() {

            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {

                /* This is called by setEnabled too and we want to block only the async task. */
                if (testThread != Thread.currentThread()) {
                    beforeDisabledSemaphore.release();
                    afterDisabledSemaphore.acquireUninterruptibly();
                }
                return downloadId;
            }
        });

        /* Make sure async task is getting storage. */
        mCheckDownloadBeforeSemaphore.release();
        beforeDisabledSemaphore.acquireUninterruptibly();

        /* Disable now. */
        Distribute.setEnabled(false);

        /* Release task. */
        afterDisabledSemaphore.release();

        /* And wait for it to complete. */
        mCheckDownloadAfterSemaphore.acquireUninterruptibly();

        /* Verify we don't mark download checked as in progress. */
        assertEquals(false, Whitebox.getInternalState(Distribute.getInstance(), "mCheckedDownload"));
    }

    @Test
    public void disabledBeforeNotifying() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Change behavior of get download it to block to simulate the concurrency issue. */
        final Semaphore beforeDisabledSemaphore = new Semaphore(0);
        final Semaphore afterDisabledSemaphore = new Semaphore(0);

        /* Call get to execute last when so that we can override the answer for next calls. */
        final long downloadId = PreferencesStorage.getLong(PREFERENCE_KEY_DOWNLOAD_ID, INVALID_DOWNLOAD_IDENTIFIER);

        /* Overwrite next answer. */
        final Thread testThread = Thread.currentThread();
        when(PreferencesStorage.getLong(PREFERENCE_KEY_DOWNLOAD_ID, INVALID_DOWNLOAD_IDENTIFIER)).then(new Answer<Long>() {

            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {

                /* This is called by setEnabled too and we want to block only the async task. */
                if (testThread != Thread.currentThread()) {
                    beforeDisabledSemaphore.release();
                    afterDisabledSemaphore.acquireUninterruptibly();
                }
                return downloadId;
            }
        });

        /* Mock success in background. */
        Distribute.getInstance().onActivityPaused(mActivity);
        mockSuccessCursor();
        mockInstallIntent();
        completeDownload();

        /* Make sure async task is getting storage. */
        mCheckDownloadBeforeSemaphore.release();
        beforeDisabledSemaphore.acquireUninterruptibly();

        /* Disable now. */
        Distribute.setEnabled(false);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Release task. */
        afterDisabledSemaphore.release();

        /* And wait for it to complete. */
        mCheckDownloadAfterSemaphore.acquireUninterruptibly();

        /* Verify we skip notification and clean happens only in disable (only once). */
        verify(mContext, never()).startActivity(any(Intent.class));
        verifyZeroInteractions(mNotificationManager);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
    }

    @Test
    public void startActivityButDisabledAfterCheckpoint() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Process download completion. */
        Cursor cursor = mockSuccessCursor();
        final Intent installIntent = mock(Intent.class);
        whenNew(Intent.class).withArguments(Intent.ACTION_INSTALL_PACKAGE).thenReturn(installIntent);
        when(installIntent.resolveActivity(any(PackageManager.class))).thenReturn(mock(ComponentName.class));
        final Semaphore beforeStartingActivityLock = new Semaphore(0);
        final Semaphore disabledLock = new Semaphore(0);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                beforeStartingActivityLock.release();
                disabledLock.acquireUninterruptibly();
                return null;
            }
        }).when(mContext).startActivity(installIntent);

        /* Disable while calling startActivity... */
        completeDownload();
        mCheckDownloadBeforeSemaphore.release();
        beforeStartingActivityLock.acquireUninterruptibly();
        Distribute.setEnabled(false);
        disabledLock.release();
        mCheckDownloadAfterSemaphore.acquireUninterruptibly();

        /* Verify start activity and complete workflow skipped, e.g. clean behavior happened only once. */
        verify(mContext).startActivity(installIntent);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verifyZeroInteractions(mNotificationManager);
        verify(cursor).close();
    }

    @Test
    @PrepareForTest(Uri.class)
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void notifyThenRestartAppTwice() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Process fake download completion, should not interfere and will be ignored. */
        {
            Intent completionIntent = mock(Intent.class);
            when(completionIntent.getAction()).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            when(completionIntent.getLongExtra(eq(EXTRA_DOWNLOAD_ID), anyLong())).thenReturn(404L);
            new DownloadManagerReceiver().onReceive(mContext, completionIntent);
            waitCheckDownloadTask();
            verify(mDownloadManager, never()).query(any(DownloadManager.Query.class));
        }

        /* Process download completion with the real download identifier. */
        completeDownload();
        Cursor cursor = mockSuccessCursor();
        Intent installIntent = mockInstallIntent();

        /* In background. */
        Distribute.getInstance().onActivityPaused(mActivity);

        /* Mock notification. */
        when(mPackageManager.getApplicationInfo(mContext.getPackageName(), 0)).thenReturn(mock(ApplicationInfo.class));
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.JELLY_BEAN);
        Notification.Builder notificationBuilder = mockNotificationBuilderChain();
        when(notificationBuilder.build()).thenReturn(mock(Notification.class));

        /* Simulate task. */
        waitCheckDownloadTask();

        /* Verify notification. */
        verify(mContext, never()).startActivity(installIntent);
        verifyStatic();
        PreferencesStorage.putInt(PREFERENCE_KEY_DOWNLOAD_STATE, DOWNLOAD_STATE_NOTIFIED);
        verify(notificationBuilder).build();
        verify(notificationBuilder, never()).getNotification();
        verify(mNotificationManager).notify(eq(DistributeUtils.getNotificationId()), any(Notification.class));
        verifyNoMoreInteractions(mNotificationManager);
        verify(cursor).close();

        /* Launch app should pop install U.I. and cancel notification. */
        when(mActivity.getPackageManager()).thenReturn(mPackageManager);
        Intent launcherIntent = mock(Intent.class);
        when(mPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(launcherIntent);
        ComponentName launcher = mock(ComponentName.class);
        when(launcherIntent.resolveActivity(mPackageManager)).thenReturn(launcher);
        when(launcher.getClassName()).thenReturn(mActivity.getClass().getName());
        restartActivity();

        /* Wait again. */
        waitCheckDownloadTask();

        /* Verify U.I shown after restart and workflow completed. */
        verify(mContext).startActivity(installIntent);
        verify(mNotificationManager).cancel(DistributeUtils.getNotificationId());
        verifyStatic();

        /* Verify workflow completed. */
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Verify however downloaded file was kept. */
        verifyStatic(never());
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        verify(mDownloadManager, never()).remove(DOWNLOAD_ID);

        /* Verify second download (restart app again) cleans first one. */
        when(mDownloadManager.enqueue(mDownloadRequest)).thenReturn(DOWNLOAD_ID + 1);
        restartActivity();
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder, times(2)).setPositiveButton(eq(R.string.appcenter_distribute_update_dialog_download), clickListener.capture());
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_POSITIVE);
        waitDownloadTask();

        /* Verify new download id in storage. */
        verifyStatic();
        PreferencesStorage.putLong(PREFERENCE_KEY_DOWNLOAD_ID, DOWNLOAD_ID + 1);

        /* Verify previous download removed. */
        verify(mDownloadManager).remove(DOWNLOAD_ID);

        /* Notification already canceled so no more call, i.e. only once. */
        verify(mNotificationManager).cancel(DistributeUtils.getNotificationId());
    }

    @Test
    @PrepareForTest(Uri.class)
    @SuppressWarnings("deprecation")
    public void notifyThenRestartThenInstallerFails() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Kill app, this has nothing to do with failure, but we need to test that too. */
        Distribute.unsetInstance();

        /* Process download completion. */
        completeDownload();

        /* Mock old device URI. */
        Cursor cursor = mockSuccessCursor();
        when(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_FILENAME)).thenReturn(2);
        Intent installIntent = mock(Intent.class);
        whenNew(Intent.class).withArguments(Intent.ACTION_INSTALL_PACKAGE).thenReturn(installIntent);
        when(installIntent.resolveActivity(any(PackageManager.class))).thenReturn(null).thenReturn(mock(ComponentName.class));

        /* Mock notification. */
        when(mPackageManager.getApplicationInfo(mContext.getPackageName(), 0)).thenReturn(mock(ApplicationInfo.class));
        Notification.Builder notificationBuilder = mockNotificationBuilderChain();
        when(notificationBuilder.getNotification()).thenReturn(mock(Notification.class));

        /* Simulate task. */
        waitCheckDownloadTask();

        /* Verify notification. */
        verify(mContext, never()).startActivity(installIntent);
        verifyStatic();
        PreferencesStorage.putInt(PREFERENCE_KEY_DOWNLOAD_STATE, DOWNLOAD_STATE_NOTIFIED);
        verify(mNotificationManager).notify(eq(DistributeUtils.getNotificationId()), any(Notification.class));
        verifyNoMoreInteractions(mNotificationManager);
        verify(cursor).getString(2);
        verify(cursor).close();

        /* Restart app should pop install U.I. and cancel notification and pop a new dialog then a new download. */
        doThrow(new ActivityNotFoundException()).when(mContext).startActivity(installIntent);
        start();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Wait download manager query. */
        waitCheckDownloadTask();

        /* Verify workflow completed even on failure to show install U.I. */
        verify(mContext).startActivity(installIntent);
        verify(mNotificationManager).cancel(DistributeUtils.getNotificationId());
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verifyStatic(never());
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void restartDownloadCheckIsLongEnoughToAppCanGoBackgroundAgain() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();
        Distribute.getInstance().onActivityPaused(mActivity);

        /* Process download completion to notify. */
        completeDownload();
        mockSuccessCursor();
        Intent installIntent = mockInstallIntent();
        when(mPackageManager.getApplicationInfo(mContext.getPackageName(), 0)).thenReturn(mock(ApplicationInfo.class));
        Notification.Builder notificationBuilder = mockNotificationBuilderChain();
        when(notificationBuilder.getNotification()).thenReturn(mock(Notification.class));

        /* Verify. */
        waitCheckDownloadTask();
        verify(mNotificationManager).notify(anyInt(), any(Notification.class));
        verify(mContext, never()).startActivity(installIntent);

        /*
         * Restart app, even if app goes background while checking state, we must show U.I. as we
         * already notified.
         */
        restartProcessAndSdk();
        Distribute.getInstance().onActivityResumed(mActivity);
        Distribute.getInstance().onActivityPaused(mActivity);
        waitCheckDownloadTask();
        verify(mNotificationManager).notify(anyInt(), any(Notification.class));
        verify(mContext).startActivity(installIntent);
    }

    @Test
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public void doNotShowInstallUiIfUpgradedAfterNotification() throws Exception {

        /* Mock download time storage. */
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                when(PreferencesStorage.getLong(PREFERENCE_KEY_DOWNLOAD_TIME)).thenReturn((Long) invocation.getArguments()[1]);
                return null;
            }
        }).when(PreferencesStorage.class);
        PreferencesStorage.putLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME), anyLong());

        /* Simulate async task. */
        waitDownloadTask();
        verifyStatic();
        PreferencesStorage.putLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME), anyLong());

        /* Mock download completion to notify. */
        mockSuccessCursor();
        Intent installIntent = mockInstallIntent();
        when(mPackageManager.getApplicationInfo(mContext.getPackageName(), 0)).thenReturn(mock(ApplicationInfo.class));
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.O);
        Notification.Builder notificationBuilder = mockNotificationBuilderChain();
        when(notificationBuilder.build()).thenReturn(mock(Notification.class));

        /* Make notification happen. */
        Distribute.getInstance().onActivityPaused(mActivity);
        completeDownload();
        waitCheckDownloadTask();

        /* Verify. */
        verify(mNotificationManager).notify(anyInt(), any(Notification.class));
        verify(mNotificationManager).createNotificationChannel(any(NotificationChannel.class));
        verify(mContext, never()).startActivity(installIntent);

        /* Restart app after upgrade, discard download and check update again. */
        PackageInfo packageInfo = mock(PackageInfo.class);
        packageInfo.lastUpdateTime = Long.MAX_VALUE;
        when(mPackageManager.getPackageInfo(mContext.getPackageName(), 0)).thenReturn(packageInfo);
        restartProcessAndSdk();
        Distribute.getInstance().onActivityResumed(mActivity);
        verify(mDownloadManager).remove(DOWNLOAD_ID);

        /* Verify new release checked (for example what we installed was something else than the upgrade. */
        verify(mDialog, times(2)).show();
    }
}
