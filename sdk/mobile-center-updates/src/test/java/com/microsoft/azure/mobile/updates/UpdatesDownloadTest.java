package com.microsoft.azure.mobile.updates;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;

import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.http.HttpClient;
import com.microsoft.azure.mobile.http.HttpClientNetworkStateHandler;
import com.microsoft.azure.mobile.http.ServiceCall;
import com.microsoft.azure.mobile.http.ServiceCallback;
import com.microsoft.azure.mobile.test.TestUtils;
import com.microsoft.azure.mobile.utils.AsyncTaskUtils;
import com.microsoft.azure.mobile.utils.storage.StorageHelper.PreferencesStorage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static android.app.DownloadManager.EXTRA_DOWNLOAD_ID;
import static android.content.Context.NOTIFICATION_SERVICE;
import static com.microsoft.azure.mobile.updates.UpdateConstants.DOWNLOAD_STATE_COMPLETED;
import static com.microsoft.azure.mobile.updates.UpdateConstants.DOWNLOAD_STATE_ENQUEUED;
import static com.microsoft.azure.mobile.updates.UpdateConstants.DOWNLOAD_STATE_NOTIFIED;
import static com.microsoft.azure.mobile.updates.UpdateConstants.INVALID_DOWNLOAD_IDENTIFIER;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PREFERENCE_KEY_DOWNLOAD_ID;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PREFERENCE_KEY_DOWNLOAD_STATE;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PREFERENCE_KEY_DOWNLOAD_TIME;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PREFERENCE_KEY_UPDATE_TOKEN;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@PrepareForTest(AsyncTaskUtils.class)
public class UpdatesDownloadTest extends AbstractUpdatesTest {

    private static final long DOWNLOAD_ID = 42;

    @Mock
    private Uri mDownloadUrl;

    @Mock
    private DownloadManager mDownloadManager;

    @Mock
    private NotificationManager mNotificationManager;

    @Mock
    private DownloadManager.Request mDownloadRequest;

    @Mock
    private Activity mFirstActivity;

    private Semaphore mDownloadBeforeSemaphore;

    private Semaphore mDownloadAfterSemaphore;

    private AtomicReference<Updates.DownloadTask> mDownloadTask;

    private Semaphore mCheckDownloadBeforeSemaphore;

    private Semaphore mCheckDownloadAfterSemaphore;

    private AtomicReference<Updates.CheckDownloadTask> mCompletionTask;

    @Before
    public void setUpDownload() throws Exception {

        /* Allow unknown sources. */
        when(InstallerUtils.isUnknownSourcesEnabled(any(Context.class))).thenReturn(true);

        /* Mock download manager. */
        when(mContext.getSystemService(Context.DOWNLOAD_SERVICE)).thenReturn(mDownloadManager);
        whenNew(DownloadManager.Request.class).withAnyArguments().thenReturn(mDownloadRequest);
        when(mDownloadManager.enqueue(mDownloadRequest)).thenReturn(DOWNLOAD_ID);

        /* Mock notification manager. */
        when(mContext.getSystemService(NOTIFICATION_SERVICE)).thenReturn(mNotificationManager);

        /* Mock updates to storage. */
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                when(PreferencesStorage.getLong(invocation.getArguments()[0].toString(), INVALID_DOWNLOAD_IDENTIFIER)).thenReturn((Long) invocation.getArguments()[1]);
                return null;
            }
        }).when(PreferencesStorage.class);
        PreferencesStorage.putLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), anyLong());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                when(PreferencesStorage.getLong(invocation.getArguments()[0].toString(), INVALID_DOWNLOAD_IDENTIFIER)).thenReturn(INVALID_DOWNLOAD_IDENTIFIER);
                return null;
            }
        }).when(PreferencesStorage.class);
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                when(PreferencesStorage.getInt(invocation.getArguments()[0].toString(), DOWNLOAD_STATE_COMPLETED)).thenReturn((Integer) invocation.getArguments()[1]);
                return null;
            }
        }).when(PreferencesStorage.class);
        PreferencesStorage.putInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), anyInt());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                when(PreferencesStorage.getInt(invocation.getArguments()[0].toString(), DOWNLOAD_STATE_COMPLETED)).thenReturn(DOWNLOAD_STATE_COMPLETED);
                return null;
            }
        }).when(PreferencesStorage.class);
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Mock everything that triggers a download. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) throws Throwable {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("mock");
                return mock(ServiceCall.class);
            }
        });
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn("someId");
        when(releaseDetails.getVersion()).thenReturn(7);
        when(releaseDetails.getDownloadUrl()).thenReturn(mDownloadUrl);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);
        mockStatic(AsyncTaskUtils.class);
        Updates.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Updates.getInstance().onActivityResumed(mFirstActivity);

        /* Mock download asyncTask. */
        mDownloadBeforeSemaphore = new Semaphore(0);
        mDownloadAfterSemaphore = new Semaphore(0);
        mDownloadTask = new AtomicReference<>();
        when(AsyncTaskUtils.execute(anyString(), argThat(new ArgumentMatcher<Updates.DownloadTask>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof Updates.DownloadTask;
            }
        }), Mockito.<Void>anyVararg())).then(new Answer<Updates.DownloadTask>() {

            @Override
            public Updates.DownloadTask answer(InvocationOnMock invocation) throws Throwable {
                final Updates.DownloadTask task = spy((Updates.DownloadTask) invocation.getArguments()[1]);
                mDownloadTask.set(task);
                new Thread() {

                    @Override
                    public void run() {
                        mDownloadBeforeSemaphore.acquireUninterruptibly();
                        task.doInBackground(null);
                        mDownloadAfterSemaphore.release();
                    }
                }.start();
                return task;
            }
        });

        /* Mock remove download async task. */
        when(AsyncTaskUtils.execute(anyString(), argThat(new ArgumentMatcher<Updates.RemoveDownloadTask>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof Updates.RemoveDownloadTask;
            }
        }), Mockito.<Long>anyVararg())).then(new Answer<Updates.RemoveDownloadTask>() {

            @Override
            public Updates.RemoveDownloadTask answer(InvocationOnMock invocation) throws Throwable {
                final Updates.RemoveDownloadTask task = (Updates.RemoveDownloadTask) invocation.getArguments()[1];
                task.doInBackground((Long) invocation.getArguments()[2]);
                return task;
            }
        });

        /* Mock download completion async task. */
        mCheckDownloadBeforeSemaphore = new Semaphore(0);
        mCheckDownloadAfterSemaphore = new Semaphore(0);
        mCompletionTask = new AtomicReference<>();
        when(AsyncTaskUtils.execute(anyString(), argThat(new ArgumentMatcher<Updates.CheckDownloadTask>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof Updates.CheckDownloadTask;
            }
        }), Mockito.<Void>anyVararg())).then(new Answer<Updates.CheckDownloadTask>() {

            @Override
            public Updates.CheckDownloadTask answer(InvocationOnMock invocation) throws Throwable {
                final Updates.CheckDownloadTask task = spy((Updates.CheckDownloadTask) invocation.getArguments()[1]);
                mCompletionTask.set(task);
                new Thread() {

                    @Override
                    public void run() {
                        mCheckDownloadBeforeSemaphore.acquireUninterruptibly();
                        task.doInBackground();
                        mCheckDownloadAfterSemaphore.release();
                    }
                }.start();
                return task;
            }
        });

        /* Click on dialog. */
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder).setPositiveButton(eq(R.string.mobile_center_updates_update_dialog_download), clickListener.capture());
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_POSITIVE);
    }

    private void waitDownloadTask() {
        mDownloadBeforeSemaphore.release();
        mDownloadAfterSemaphore.acquireUninterruptibly();
    }

    private void waitCheckDownloadTask() {
        mCheckDownloadBeforeSemaphore.release();
        mCheckDownloadAfterSemaphore.acquireUninterruptibly();
    }

    private void completeDownload() {
        Intent completionIntent = mock(Intent.class);
        when(completionIntent.getAction()).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        when(completionIntent.getLongExtra(eq(EXTRA_DOWNLOAD_ID), anyLong())).thenReturn(DOWNLOAD_ID);
        new DownloadManagerReceiver().onReceive(mContext, completionIntent);
    }

    @NonNull
    private Cursor mockSuccessCursor() {
        Cursor cursor = mock(Cursor.class);
        when(mDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(cursor);
        when(cursor.moveToFirst()).thenReturn(true);
        when(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)).thenReturn(0);
        when(cursor.getInt(0)).thenReturn(DownloadManager.STATUS_SUCCESSFUL);
        when(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)).thenReturn(1);
        when(cursor.getString(1)).thenReturn("content://downloads/all_downloads/" + DOWNLOAD_ID);
        return cursor;
    }

    @NonNull
    private Notification.Builder mockNotificationBuilderChain() throws Exception {
        Notification.Builder notificationBuilder = mock(Notification.Builder.class);
        whenNew(Notification.Builder.class).withAnyArguments().thenReturn(notificationBuilder);
        when(notificationBuilder.setTicker(anyString())).thenReturn(notificationBuilder);
        when(notificationBuilder.setContentTitle(anyString())).thenReturn(notificationBuilder);
        when(notificationBuilder.setContentText(anyString())).thenReturn(notificationBuilder);
        when(notificationBuilder.setSmallIcon(anyInt())).thenReturn(notificationBuilder);
        when(notificationBuilder.setContentIntent(any(PendingIntent.class))).thenReturn(notificationBuilder);
        return notificationBuilder;
    }

    @NonNull
    private Intent mockInstallIntent() throws Exception {
        Intent installIntent = mock(Intent.class);
        whenNew(Intent.class).withArguments(Intent.ACTION_INSTALL_PACKAGE).thenReturn(installIntent);
        when(installIntent.resolveActivity(any(PackageManager.class))).thenReturn(mock(ComponentName.class));
        return installIntent;
    }

    private void restartActivity() {
        Updates.getInstance().onActivityStopped(mFirstActivity);
        Updates.getInstance().onActivityDestroyed(mFirstActivity);
        Updates.getInstance().onActivityCreated(mFirstActivity, null);
        Updates.getInstance().onActivityResumed(mFirstActivity);
    }

    private void restartProcessAndSdk() {
        Updates.unsetInstance();
        Updates.getInstance().onStarted(mContext, "a", mock(Channel.class));
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
        Updates.getInstance().onActivityPaused(mFirstActivity);
        Updates.getInstance().onActivityResumed(mFirstActivity);
        verify(mDialog).show();

        /* Cancel download by disabling. */
        Updates.setEnabled(false);
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
        Updates.setEnabled(false);
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
        Updates.setEnabled(false);
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
        Updates.getInstance().onActivityPaused(activity);
        Updates.getInstance().onActivityResumed(activity);

        /* Verify download happened only once. */
        verify(mDownloadManager).enqueue(mDownloadRequest);

        /* Exit app. */
        Updates.getInstance().onActivityPaused(activity);
        Updates.getInstance().onActivityStopped(activity);
        Updates.getInstance().onActivityDestroyed(activity);

        /* Recreate activity, we'll cache that there is no launcher since no mock intent. */
        when(activity.getPackageManager()).thenReturn(mock(PackageManager.class));
        Updates.getInstance().onActivityCreated(activity, null);

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
        Updates.setEnabled(false);

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

        /* Verify enabling triggers update dialog again. */
        verify(mDialog).show();
        Updates.setEnabled(true);
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
        verifyNoMoreInteractions(mNotificationManager);
        verify(cursor).close();
    }

    @Test
    public void longFailingDownload() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Mock running cursor. */
        Cursor cursor = mock(Cursor.class);
        when(mDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(cursor);
        when(cursor.moveToFirst()).thenReturn(true);
        when(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)).thenReturn(0);
        when(cursor.getInt(0)).thenReturn(DownloadManager.STATUS_RUNNING);

        /* Restart launcher, nothing happens. */
        when(mFirstActivity.getPackageManager()).thenReturn(mPackageManager);
        Intent launcherIntent = mock(Intent.class);
        when(mPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(launcherIntent);
        ComponentName launcher = mock(ComponentName.class);
        when(launcherIntent.resolveActivity(mPackageManager)).thenReturn(launcher);
        when(launcher.getClassName()).thenReturn(mFirstActivity.getClass().getName());
        restartActivity();

        /* Restart app process. Still nothing as background. */
        restartProcessAndSdk();

        /* No download check yet. */
        verifyStatic(never());
        AsyncTaskUtils.execute(anyString(), argThat(new ArgumentMatcher<Updates.CheckDownloadTask>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof Updates.CheckDownloadTask;
            }
        }), Mockito.<Void>anyVararg());

        /* Foreground: check still in progress. */
        Updates.getInstance().onActivityResumed(mFirstActivity);
        waitCheckDownloadTask();
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), argThat(new ArgumentMatcher<Updates.CheckDownloadTask>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof Updates.CheckDownloadTask;
            }
        }), Mockito.<Void>anyVararg());
        verify(cursor).close();

        /* Restart launcher. */
        Updates.getInstance().onActivityPaused(mFirstActivity);
        restartActivity();

        /* Verify we don't run the check again. (Only once). */
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), argThat(new ArgumentMatcher<Updates.CheckDownloadTask>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof Updates.CheckDownloadTask;
            }
        }), Mockito.<Void>anyVararg());

        /* Download eventually fails. */
        when(cursor.getInt(0)).thenReturn(DownloadManager.STATUS_FAILED);
        completeDownload();

        /* Simulate task. */
        waitCheckDownloadTask();

        /* Verify we complete workflow on failure. */
        verify(mContext, never()).startActivity(any(Intent.class));
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
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

        /* Restart app process. Still nothing as background. */
        restartProcessAndSdk();
        Updates.getInstance().onActivityResumed(mFirstActivity);

        /* Change behavior of get download it to block to simulate the concurrency issue. */
        final Semaphore waitDisabledSemaphore = new Semaphore(0);

        /* Call get to execute last when so that we can override the answer for next calls. */
        final long downloadId = PreferencesStorage.getLong(PREFERENCE_KEY_DOWNLOAD_ID, INVALID_DOWNLOAD_IDENTIFIER);

        /* Overwrite next answer. */
        final Thread testThread = Thread.currentThread();
        when(PreferencesStorage.getLong(PREFERENCE_KEY_DOWNLOAD_ID, INVALID_DOWNLOAD_IDENTIFIER)).then(new Answer<Long>() {

            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {

                /* This is called by setEnabled too and we want to block only the async task. */
                if (testThread != Thread.currentThread()) {
                    waitDisabledSemaphore.acquireUninterruptibly();
                }
                return downloadId;
            }
        });

        /* Make sure async task is getting storage. */
        mCheckDownloadBeforeSemaphore.release();

        /* Disable now. */
        Updates.setEnabled(false);

        /* Release task. */
        waitDisabledSemaphore.release();

        /* And wait for it to complete. */
        mCheckDownloadAfterSemaphore.acquireUninterruptibly();

        /* Verify we don't mark download checked as in progress. */
        assertEquals(false, Whitebox.getInternalState(Updates.getInstance(), "mCheckedDownload"));
    }

    @Test
    public void disabledBeforeNotifying() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Change behavior of get download it to block to simulate the concurrency issue. */
        final Semaphore waitDisabledSemaphore = new Semaphore(0);

        /* Call get to execute last when so that we can override the answer for next calls. */
        final long downloadId = PreferencesStorage.getLong(PREFERENCE_KEY_DOWNLOAD_ID, INVALID_DOWNLOAD_IDENTIFIER);

        /* Overwrite next answer. */
        final Thread testThread = Thread.currentThread();
        when(PreferencesStorage.getLong(PREFERENCE_KEY_DOWNLOAD_ID, INVALID_DOWNLOAD_IDENTIFIER)).then(new Answer<Long>() {

            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {

                /* This is called by setEnabled too and we want to block only the async task. */
                if (testThread != Thread.currentThread()) {
                    waitDisabledSemaphore.acquireUninterruptibly();
                }
                return downloadId;
            }
        });

        /* Mock success in background. */
        Updates.getInstance().onActivityPaused(mFirstActivity);
        mockSuccessCursor();
        mockInstallIntent();
        completeDownload();

        /* Make sure async task is getting storage. */
        mCheckDownloadBeforeSemaphore.release();

        /* Disable now. */
        Updates.setEnabled(false);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Release task. */
        waitDisabledSemaphore.release();

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
        Updates.setEnabled(false);
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
    public void failsToGetNotificationIcon() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Process download completion. */
        completeDownload();
        Cursor cursor = mockSuccessCursor();
        Intent installIntent = mockInstallIntent();

        /* In background. */
        Updates.getInstance().onActivityPaused(mFirstActivity);

        /* And the icon will fail. */
        when(mPackageManager.getApplicationInfo(mContext.getPackageName(), 0)).thenThrow(new PackageManager.NameNotFoundException());

        /* Simulate task. */
        waitCheckDownloadTask();

        /* Verify complete workflow with no notification. */
        verify(mContext, never()).startActivity(installIntent);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verifyNoMoreInteractions(mNotificationManager);
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
        Updates.getInstance().onActivityPaused(mFirstActivity);

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
        verify(mNotificationManager).notify(eq(Updates.getNotificationId()), any(Notification.class));
        verifyNoMoreInteractions(mNotificationManager);
        verify(cursor).close();

        /* Launch app should pop install U.I. and cancel notification. */
        when(mFirstActivity.getPackageManager()).thenReturn(mPackageManager);
        Intent launcherIntent = mock(Intent.class);
        when(mPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(launcherIntent);
        ComponentName launcher = mock(ComponentName.class);
        when(launcherIntent.resolveActivity(mPackageManager)).thenReturn(launcher);
        when(launcher.getClassName()).thenReturn(mFirstActivity.getClass().getName());
        restartActivity();

        /* Wait again. */
        waitCheckDownloadTask();

        /* Verify U.I shown after restart and workflow completed. */
        verify(mContext).startActivity(installIntent);
        verify(mNotificationManager).cancel(Updates.getNotificationId());
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Verify however downloaded file was kept. */
        verifyStatic(never());
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        verify(mDownloadManager, never()).remove(DOWNLOAD_ID);

        /* Verify second download (restart app again) cleans first one. */
        when(mDownloadManager.enqueue(mDownloadRequest)).thenReturn(DOWNLOAD_ID + 1);
        restartActivity();
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder, times(2)).setPositiveButton(eq(R.string.mobile_center_updates_update_dialog_download), clickListener.capture());
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_POSITIVE);
        waitDownloadTask();

        /* Verify new download id in storage. */
        verifyStatic();
        PreferencesStorage.putLong(PREFERENCE_KEY_DOWNLOAD_ID, DOWNLOAD_ID + 1);

        /* Verify previous download removed. */
        verify(mDownloadManager).remove(DOWNLOAD_ID);

        /* Notification already canceled so no more call, i.e. only once. */
        verify(mNotificationManager).cancel(Updates.getNotificationId());
    }

    @Test
    @PrepareForTest(Uri.class)
    @SuppressWarnings("deprecation")
    public void notifyThenRestartThenInstallerFails() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Kill app, this has nothing to do with failure, but we need to test that too. */
        Updates.unsetInstance();

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
        verify(mNotificationManager).notify(eq(Updates.getNotificationId()), any(Notification.class));
        verifyNoMoreInteractions(mNotificationManager);
        verify(cursor).getString(2);
        verify(cursor).close();

        /* Restart app should pop install U.I. and cancel notification and pop a new dialog then a new download. */
        doThrow(new ActivityNotFoundException()).when(mContext).startActivity(installIntent);
        Updates.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Updates.getInstance().onActivityResumed(mFirstActivity);

        /* Wait download manager query. */
        waitCheckDownloadTask();

        /* Verify workflow completed even on failure to show install U.I. */
        verify(mContext).startActivity(installIntent);
        verify(mNotificationManager).cancel(Updates.getNotificationId());
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
        Updates.getInstance().onActivityPaused(mFirstActivity);

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
        Updates.getInstance().onActivityResumed(mFirstActivity);
        Updates.getInstance().onActivityPaused(mFirstActivity);
        waitCheckDownloadTask();
        verify(mNotificationManager).notify(anyInt(), any(Notification.class));
        verify(mContext).startActivity(installIntent);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void dontShowInstallUiIfUpgradedAfterNotification() throws Exception {

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
        Notification.Builder notificationBuilder = mockNotificationBuilderChain();
        when(notificationBuilder.getNotification()).thenReturn(mock(Notification.class));

        /* Make notification happen. */
        Updates.getInstance().onActivityPaused(mFirstActivity);
        completeDownload();
        waitCheckDownloadTask();

        /* Verify. */
        verify(mNotificationManager).notify(anyInt(), any(Notification.class));
        verify(mContext, never()).startActivity(installIntent);

        /* Restart app after upgrade, discard download and check update again. */
        PackageInfo packageInfo = mock(PackageInfo.class);
        packageInfo.lastUpdateTime = Long.MAX_VALUE;
        when(mPackageManager.getPackageInfo(mContext.getPackageName(), 0)).thenReturn(packageInfo);
        restartProcessAndSdk();
        Updates.getInstance().onActivityResumed(mFirstActivity);
        verify(mDownloadManager).remove(DOWNLOAD_ID);

        /* Verify new release checked (for example what we installed was something else than the upgrade. */
        verify(mDialog, times(2)).show();
    }

    @Test
    @SuppressWarnings("deprecation")
    public void failToCheckLastUpdateTimeOnRestart() throws PackageManager.NameNotFoundException {

        /* Make the package manager fails on restart after download started. */
        waitDownloadTask();
        when(mPackageManager.getPackageInfo(mContext.getPackageName(), 0)).thenThrow(new PackageManager.NameNotFoundException());
        restartProcessAndSdk();
        Updates.getInstance().onActivityResumed(mFirstActivity);

        /* Verify workflow completed on failure. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", 0);
    }
}
