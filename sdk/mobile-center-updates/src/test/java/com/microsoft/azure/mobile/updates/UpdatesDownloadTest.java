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
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.http.HttpClient;
import com.microsoft.azure.mobile.http.HttpClientNetworkStateHandler;
import com.microsoft.azure.mobile.http.ServiceCall;
import com.microsoft.azure.mobile.http.ServiceCallback;
import com.microsoft.azure.mobile.test.TestUtils;
import com.microsoft.azure.mobile.utils.AsyncTaskUtils;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;
import com.microsoft.azure.mobile.utils.storage.StorageHelper.PreferencesStorage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static android.app.DownloadManager.EXTRA_DOWNLOAD_ID;
import static android.content.Context.NOTIFICATION_SERVICE;
import static com.microsoft.azure.mobile.updates.UpdateConstants.INVALID_DOWNLOAD_IDENTIFIER;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PREFERENCE_KEY_DOWNLOAD_ID;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PREFERENCE_KEY_DOWNLOAD_URI;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PREFERENCE_KEY_UPDATE_TOKEN;
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
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
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

    private Semaphore mCompletionBeforeSemaphore;

    private Semaphore mCompletionAfterSemaphore;

    private AtomicReference<Updates.ProcessDownloadCompletionTask> mCompletionTask;

    @Before
    public void setUpDownload() throws Exception {

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
                when(StorageHelper.PreferencesStorage.getLong(invocation.getArguments()[0].toString(), INVALID_DOWNLOAD_IDENTIFIER)).thenReturn((Long) invocation.getArguments()[1]);
                return null;
            }
        }).when(StorageHelper.PreferencesStorage.class);
        StorageHelper.PreferencesStorage.putLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), anyLong());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                when(StorageHelper.PreferencesStorage.getLong(invocation.getArguments()[0].toString(), INVALID_DOWNLOAD_IDENTIFIER)).thenReturn(INVALID_DOWNLOAD_IDENTIFIER);
                return null;
            }
        }).when(StorageHelper.PreferencesStorage.class);
        StorageHelper.PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                when(StorageHelper.PreferencesStorage.getString(invocation.getArguments()[0].toString())).thenReturn((String) invocation.getArguments()[1]);
                return null;
            }
        }).when(StorageHelper.PreferencesStorage.class);
        StorageHelper.PreferencesStorage.putString(eq(PREFERENCE_KEY_DOWNLOAD_URI), anyString());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                when(StorageHelper.PreferencesStorage.getString(invocation.getArguments()[0].toString())).thenReturn(null);
                return null;
            }
        }).when(StorageHelper.PreferencesStorage.class);
        StorageHelper.PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);

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
        mCompletionBeforeSemaphore = new Semaphore(0);
        mCompletionAfterSemaphore = new Semaphore(0);
        mCompletionTask = new AtomicReference<>();
        when(AsyncTaskUtils.execute(anyString(), argThat(new ArgumentMatcher<Updates.ProcessDownloadCompletionTask>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof Updates.ProcessDownloadCompletionTask;
            }
        }), Mockito.<Void>anyVararg())).then(new Answer<Updates.ProcessDownloadCompletionTask>() {

            @Override
            public Updates.ProcessDownloadCompletionTask answer(InvocationOnMock invocation) throws Throwable {
                final Updates.ProcessDownloadCompletionTask task = spy((Updates.ProcessDownloadCompletionTask) invocation.getArguments()[1]);
                mCompletionTask.set(task);
                new Thread() {

                    @Override
                    public void run() {
                        mCompletionBeforeSemaphore.acquireUninterruptibly();
                        task.doInBackground();
                        mCompletionAfterSemaphore.release();
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

    private void waitCompletionTask() {
        mCompletionBeforeSemaphore.release();
        mCompletionAfterSemaphore.acquireUninterruptibly();
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
        PreferencesStorage.putString(PREFERENCE_KEY_DOWNLOAD_URI, "");

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
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
        verify(mDownloadTask.get()).cancel(true);
        verify(mDownloadManager).remove(DOWNLOAD_ID);
        verify(mNotificationManager).cancel(Updates.getNotificationId());
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
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
        verify(mDownloadTask.get()).cancel(true);
        verify(mDownloadManager).enqueue(mDownloadRequest);
        verifyNew(DownloadManager.Request.class).withArguments(mDownloadUrl);
        verify(mDownloadManager).remove(DOWNLOAD_ID);

        /* And that we didn't persist the state. */
        verifyStatic(never());
        PreferencesStorage.putLong(PREFERENCE_KEY_DOWNLOAD_ID, DOWNLOAD_ID);
        verifyStatic(never());
        PreferencesStorage.putString(PREFERENCE_KEY_DOWNLOAD_URI, "");
        verifyZeroInteractions(mNotificationManager);
    }

    @Test
    public void disableWhileProcessingCompletion() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Process download completion. */
        Intent intent = mock(Intent.class);
        when(intent.getAction()).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        when(intent.getLongExtra(eq(EXTRA_DOWNLOAD_ID), anyLong())).thenReturn(DOWNLOAD_ID);
        new DownloadCompletionReceiver().onReceive(mContext, intent);

        /* Disable before completion. */
        Updates.setEnabled(false);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
        waitCompletionTask();

        /* Verify cancellation. */
        verify(mCompletionTask.get()).cancel(true);
        verify(mDownloadManager).remove(DOWNLOAD_ID);
        verify(mNotificationManager).cancel(Updates.getNotificationId());

        /* Check cleaned state only once, the completeWorkflow on failed download has to be ignored. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
    }

    @Test
    public void failDownloadRestartNoLauncher() {

        /* Simulate async task. */
        waitDownloadTask();

        /* Process download completion. */
        Intent intent = mock(Intent.class);
        when(intent.getAction()).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        when(intent.getLongExtra(eq(EXTRA_DOWNLOAD_ID), anyLong())).thenReturn(DOWNLOAD_ID);
        new DownloadCompletionReceiver().onReceive(mContext, intent);

        /* Wait. Fails as we dont mock success uri. */
        waitCompletionTask();

        /* Check failure processing. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);

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
    public void successDownloadInstallerNotFoundCursorIsNull() {

        /* Simulate async task. */
        waitDownloadTask();

        /* Process download completion. */
        Intent intent = mock(Intent.class);
        when(intent.getAction()).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        when(intent.getLongExtra(eq(EXTRA_DOWNLOAD_ID), anyLong())).thenReturn(DOWNLOAD_ID);
        new DownloadCompletionReceiver().onReceive(mContext, intent);
        when(mDownloadManager.getUriForDownloadedFile(DOWNLOAD_ID)).thenReturn(mock(Uri.class));
        when(mDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(null);

        /* Simulate task. */
        waitCompletionTask();

        /* Check we completed workflow without starting activity because installer not found. */
        verify(mFirstActivity, never()).startActivity(any(Intent.class));
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
    }

    @Test
    public void successDownloadInstallerNotFoundCursorEmpty() {

        /* Simulate async task. */
        waitDownloadTask();

        /* Process download completion. */
        Intent intent = mock(Intent.class);
        when(intent.getAction()).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        when(intent.getLongExtra(eq(EXTRA_DOWNLOAD_ID), anyLong())).thenReturn(DOWNLOAD_ID);
        new DownloadCompletionReceiver().onReceive(mContext, intent);
        when(mDownloadManager.getUriForDownloadedFile(DOWNLOAD_ID)).thenReturn(mock(Uri.class));
        Cursor cursor = mock(Cursor.class);
        when(mDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(cursor);

        /* Simulate task. */
        waitCompletionTask();

        /* Check we completed workflow without starting activity because installer not found. */
        verify(cursor).close();
        verify(mFirstActivity, never()).startActivity(any(Intent.class));
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
    }

    @Test
    public void successDownloadInstallerNotFoundEvenWithLocalFile() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Process download completion. */
        Intent completionIntent = mock(Intent.class);
        when(completionIntent.getAction()).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        when(completionIntent.getLongExtra(eq(EXTRA_DOWNLOAD_ID), anyLong())).thenReturn(DOWNLOAD_ID);
        new DownloadCompletionReceiver().onReceive(mContext, completionIntent);
        when(mDownloadManager.getUriForDownloadedFile(DOWNLOAD_ID)).thenReturn(mock(Uri.class));
        Cursor cursor = mock(Cursor.class);
        when(mDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(cursor);
        when(cursor.moveToNext()).thenReturn(true).thenReturn(false);

        /* Simulate task. */
        waitCompletionTask();

        /* Check we completed workflow without starting activity because installer not found. */
        verify(cursor).close();
        verify(mFirstActivity, never()).startActivity(any(Intent.class));
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
    }

    @Test
    public void successDownloadInstallerNotFoundAfterNougat() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Process download completion. */
        Intent completionIntent = mock(Intent.class);
        when(completionIntent.getAction()).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        when(completionIntent.getLongExtra(eq(EXTRA_DOWNLOAD_ID), anyLong())).thenReturn(DOWNLOAD_ID);
        new DownloadCompletionReceiver().onReceive(mContext, completionIntent);
        when(mDownloadManager.getUriForDownloadedFile(DOWNLOAD_ID)).thenReturn(mock(Uri.class));
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.N);

        /* Simulate task. */
        waitCompletionTask();

        /* Check we completed workflow without starting activity because installer not found. */
        verify(mFirstActivity, never()).startActivity(any(Intent.class));
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
    }

    @Test
    public void disableWhileCompletingBeforeNougat() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Process download completion. */
        Intent completionIntent = mock(Intent.class);
        when(completionIntent.getAction()).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        when(completionIntent.getLongExtra(eq(EXTRA_DOWNLOAD_ID), anyLong())).thenReturn(DOWNLOAD_ID);
        new DownloadCompletionReceiver().onReceive(mContext, completionIntent);
        when(mDownloadManager.getUriForDownloadedFile(DOWNLOAD_ID)).thenReturn(mock(Uri.class));
        Cursor cursor = mock(Cursor.class);
        when(mDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(cursor);
        when(cursor.moveToNext()).thenReturn(true).thenReturn(false);
        Intent installIntent = mock(Intent.class);
        whenNew(Intent.class).withArguments(Intent.ACTION_INSTALL_PACKAGE).thenReturn(installIntent);
        when(installIntent.resolveActivity(any(PackageManager.class))).thenReturn(null).thenReturn(mock(ComponentName.class));

        /* Disable before task run. */
        Updates.setEnabled(false);

        /* Simulate task. */
        waitCompletionTask();

        /* Check we completed workflow without starting activity because disabled. */
        verify(cursor).close();
        verify(mFirstActivity, never()).startActivity(any(Intent.class));
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
        verify(mNotificationManager).cancel(Updates.getNotificationId());
        verifyNoMoreInteractions(mNotificationManager);
    }

    @Test
    public void successInForeground() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Process download completion. */
        Intent completionIntent = mock(Intent.class);
        when(completionIntent.getAction()).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        when(completionIntent.getLongExtra(eq(EXTRA_DOWNLOAD_ID), anyLong())).thenReturn(DOWNLOAD_ID);
        new DownloadCompletionReceiver().onReceive(mContext, completionIntent);
        Uri uri = mock(Uri.class);
        when(uri.toString()).thenReturn("original");
        when(mDownloadManager.getUriForDownloadedFile(DOWNLOAD_ID)).thenReturn(uri);
        Intent installIntent = mock(Intent.class);
        whenNew(Intent.class).withArguments(Intent.ACTION_INSTALL_PACKAGE).thenReturn(installIntent);
        when(installIntent.resolveActivity(any(PackageManager.class))).thenReturn(mock(ComponentName.class));

        /* Simulate task. */
        waitCompletionTask();

        /* Verify start activity and complete workflow. */
        verify(mFirstActivity).startActivity(installIntent);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
        verifyNoMoreInteractions(mNotificationManager);
    }

    @Test
    public void startActivityButDisabledAfterCheckpoint() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Process download completion. */
        Intent completionIntent = mock(Intent.class);
        when(completionIntent.getAction()).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        when(completionIntent.getLongExtra(eq(EXTRA_DOWNLOAD_ID), anyLong())).thenReturn(DOWNLOAD_ID);
        new DownloadCompletionReceiver().onReceive(mContext, completionIntent);
        Uri uri = mock(Uri.class);
        when(uri.toString()).thenReturn("original");
        when(mDownloadManager.getUriForDownloadedFile(DOWNLOAD_ID)).thenReturn(uri);
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
        }).when(mFirstActivity).startActivity(installIntent);

        /* Disable while calling startActivity... */
        mCompletionBeforeSemaphore.release();
        beforeStartingActivityLock.acquireUninterruptibly();
        Updates.setEnabled(false);
        disabledLock.release();
        mCompletionAfterSemaphore.acquireUninterruptibly();

        /* Verify start activity and complete workflow skipped, e.g. clean behavior happened only once. */
        verify(mFirstActivity).startActivity(installIntent);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
        verify(mNotificationManager).cancel(Updates.getNotificationId());
        verifyNoMoreInteractions(mNotificationManager);
    }

    @Test
    public void failsToGetNotificationIcon() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Process download completion. */
        Intent completionIntent = mock(Intent.class);
        when(completionIntent.getAction()).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        when(completionIntent.getLongExtra(eq(EXTRA_DOWNLOAD_ID), anyLong())).thenReturn(DOWNLOAD_ID);
        new DownloadCompletionReceiver().onReceive(mContext, completionIntent);
        Uri uri = mock(Uri.class);
        when(uri.toString()).thenReturn("original");
        when(mDownloadManager.getUriForDownloadedFile(DOWNLOAD_ID)).thenReturn(uri);
        Intent installIntent = mock(Intent.class);
        whenNew(Intent.class).withArguments(Intent.ACTION_INSTALL_PACKAGE).thenReturn(installIntent);
        when(installIntent.resolveActivity(any(PackageManager.class))).thenReturn(mock(ComponentName.class));

        /* In background. */
        Updates.getInstance().onActivityPaused(mFirstActivity);

        /* And the icon will fail. */
        when(mPackageManager.getApplicationInfo(mContext.getPackageName(), 0)).thenThrow(new PackageManager.NameNotFoundException());

        /* Simulate task. */
        waitCompletionTask();

        /* Verify complete workflow with no notification. */
        verify(mFirstActivity, never()).startActivity(installIntent);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
        verifyNoMoreInteractions(mNotificationManager);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void disableRightBeforeNotifying() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Process download completion. */
        Intent completionIntent = mock(Intent.class);
        when(completionIntent.getAction()).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        when(completionIntent.getLongExtra(eq(EXTRA_DOWNLOAD_ID), anyLong())).thenReturn(DOWNLOAD_ID);
        new DownloadCompletionReceiver().onReceive(mContext, completionIntent);
        Uri uri = mock(Uri.class);
        when(uri.toString()).thenReturn("original");
        when(mDownloadManager.getUriForDownloadedFile(DOWNLOAD_ID)).thenReturn(uri);
        Intent installIntent = mock(Intent.class);
        whenNew(Intent.class).withArguments(Intent.ACTION_INSTALL_PACKAGE).thenReturn(installIntent);
        when(installIntent.resolveActivity(any(PackageManager.class))).thenReturn(mock(ComponentName.class));

        /* In background. */
        Updates.getInstance().onActivityPaused(mFirstActivity);

        /* Mock notification. */
        when(mPackageManager.getApplicationInfo(mContext.getPackageName(), 0)).thenReturn(mock(ApplicationInfo.class));
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.JELLY_BEAN);
        Notification.Builder notificationBuilder = mock(Notification.Builder.class);
        whenNew(Notification.Builder.class).withAnyArguments().thenReturn(notificationBuilder);
        when(notificationBuilder.setTicker(anyString())).thenReturn(notificationBuilder);
        when(notificationBuilder.setContentTitle(anyString())).thenReturn(notificationBuilder);
        when(notificationBuilder.setContentText(anyString())).thenReturn(notificationBuilder);
        when(notificationBuilder.setSmallIcon(anyInt())).thenReturn(notificationBuilder);
        when(notificationBuilder.setContentIntent(any(PendingIntent.class))).thenReturn(notificationBuilder);
        final Semaphore beforeNotifying = new Semaphore(0);
        final Semaphore disabledLock = new Semaphore(0);
        when(notificationBuilder.build()).thenAnswer(new Answer<Notification>() {

            @Override
            public Notification answer(InvocationOnMock invocation) throws Throwable {
                beforeNotifying.release();
                disabledLock.acquireUninterruptibly();
                return mock(Notification.class);
            }
        });

        /* Disable while preparing notification... */
        mCompletionBeforeSemaphore.release();
        beforeNotifying.acquireUninterruptibly();
        Updates.setEnabled(false);
        disabledLock.release();
        mCompletionAfterSemaphore.acquireUninterruptibly();

        /* Verify no notification and complete workflow skipped, e.g. clean behavior happened only once. */
        verify(mFirstActivity, never()).startActivity(installIntent);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
        verify(mNotificationManager, never()).notify(anyInt(), any(Notification.class));
    }

    @Test
    @PrepareForTest(Uri.class)
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void notifyThenRestartApp() throws Exception {

        /* Simulate async task. */
        waitDownloadTask();

        /* Process fake download completion, should not interfere and will be ignored. */
        {
            Intent completionIntent = mock(Intent.class);
            when(completionIntent.getAction()).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            when(completionIntent.getLongExtra(eq(EXTRA_DOWNLOAD_ID), anyLong())).thenReturn(404L);
            new DownloadCompletionReceiver().onReceive(mContext, completionIntent);
            waitCompletionTask();
            verify(mDownloadManager, never()).getUriForDownloadedFile(anyLong());
        }

        /* Process download completion with the real download identifier. */
        Intent completionIntent = mock(Intent.class);
        when(completionIntent.getAction()).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        when(completionIntent.getLongExtra(eq(EXTRA_DOWNLOAD_ID), anyLong())).thenReturn(DOWNLOAD_ID);
        new DownloadCompletionReceiver().onReceive(mContext, completionIntent);
        Uri uri = mock(Uri.class);
        when(uri.toString()).thenReturn("original");
        when(mDownloadManager.getUriForDownloadedFile(DOWNLOAD_ID)).thenReturn(uri);
        Intent installIntent = mock(Intent.class);
        whenNew(Intent.class).withArguments(Intent.ACTION_INSTALL_PACKAGE).thenReturn(installIntent);
        when(installIntent.resolveActivity(any(PackageManager.class))).thenReturn(mock(ComponentName.class));

        /* In background. */
        Updates.getInstance().onActivityPaused(mFirstActivity);

        /* Mock notification. */
        when(mPackageManager.getApplicationInfo(mContext.getPackageName(), 0)).thenReturn(mock(ApplicationInfo.class));
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.JELLY_BEAN);
        Notification.Builder notificationBuilder = mock(Notification.Builder.class);
        whenNew(Notification.Builder.class).withAnyArguments().thenReturn(notificationBuilder);
        when(notificationBuilder.build()).thenReturn(mock(Notification.class));
        when(notificationBuilder.setTicker(anyString())).thenReturn(notificationBuilder);
        when(notificationBuilder.setContentTitle(anyString())).thenReturn(notificationBuilder);
        when(notificationBuilder.setContentText(anyString())).thenReturn(notificationBuilder);
        when(notificationBuilder.setSmallIcon(anyInt())).thenReturn(notificationBuilder);
        when(notificationBuilder.setContentIntent(any(PendingIntent.class))).thenReturn(notificationBuilder);

        /* Simulate task. */
        waitCompletionTask();

        /* Verify notification. */
        verify(mFirstActivity, never()).startActivity(installIntent);
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_DOWNLOAD_URI, "original");
        verify(notificationBuilder).build();
        verify(notificationBuilder, never()).getNotification();
        verify(mNotificationManager).notify(eq(Updates.getNotificationId()), any(Notification.class));
        verifyNoMoreInteractions(mNotificationManager);

        /* Launch app should pop install U.I. and cancel notification. */
        when(mFirstActivity.getPackageManager()).thenReturn(mPackageManager);
        Intent launcherIntent = mock(Intent.class);
        when(mPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(launcherIntent);
        ComponentName launcher = mock(ComponentName.class);
        when(launcherIntent.resolveActivity(mPackageManager)).thenReturn(launcher);
        when(launcher.getClassName()).thenReturn(mFirstActivity.getClass().getName());
        mockStatic(Uri.class);
        when(Uri.parse("original")).thenReturn(uri);
        Updates.getInstance().onActivityStopped(mFirstActivity);
        Updates.getInstance().onActivityDestroyed(mFirstActivity);
        Updates.getInstance().onActivityCreated(mFirstActivity, null);
        Updates.getInstance().onActivityResumed(mFirstActivity);

        /* Verify. */
        verify(mFirstActivity).startActivity(installIntent);
        verify(mNotificationManager).cancel(Updates.getNotificationId());
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);

        /* Keep download. */
        verifyStatic(never());
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        verify(mDownloadManager, never()).remove(DOWNLOAD_ID);

        /* Verify second download (restart app) cleans first one. */
        when(mDownloadManager.enqueue(mDownloadRequest)).thenReturn(DOWNLOAD_ID + 1);
        Updates.getInstance().onActivityStopped(mFirstActivity);
        Updates.getInstance().onActivityDestroyed(mFirstActivity);
        Updates.getInstance().onActivityCreated(mFirstActivity, null);
        Updates.getInstance().onActivityResumed(mFirstActivity);
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
        Intent completionIntent = mock(Intent.class);
        when(completionIntent.getAction()).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        when(completionIntent.getLongExtra(eq(EXTRA_DOWNLOAD_ID), anyLong())).thenReturn(DOWNLOAD_ID);
        new DownloadCompletionReceiver().onReceive(mContext, completionIntent);
        Uri originalUri = mock(Uri.class);
        when(originalUri.toString()).thenReturn("original");
        when(mDownloadManager.getUriForDownloadedFile(DOWNLOAD_ID)).thenReturn(originalUri);
        Cursor cursor = mock(Cursor.class);
        when(mDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(cursor);
        when(cursor.moveToNext()).thenReturn(true).thenReturn(false);
        when(cursor.getString(anyInt())).thenReturn("localFile");
        mockStatic(Uri.class);
        Uri localFileUri = mock(Uri.class);
        when(Uri.parse("file://localFile")).thenReturn(localFileUri);
        when(localFileUri.toString()).thenReturn("file://localFile");
        Intent installIntent = mock(Intent.class);
        whenNew(Intent.class).withArguments(Intent.ACTION_INSTALL_PACKAGE).thenReturn(installIntent);
        when(installIntent.resolveActivity(any(PackageManager.class))).thenReturn(null).thenReturn(mock(ComponentName.class));

        /* Mock notification. */
        when(mPackageManager.getApplicationInfo(mContext.getPackageName(), 0)).thenReturn(mock(ApplicationInfo.class));
        Notification.Builder notificationBuilder = mock(Notification.Builder.class);
        whenNew(Notification.Builder.class).withAnyArguments().thenReturn(notificationBuilder);
        when(notificationBuilder.getNotification()).thenReturn(mock(Notification.class));
        when(notificationBuilder.setTicker(anyString())).thenReturn(notificationBuilder);
        when(notificationBuilder.setContentTitle(anyString())).thenReturn(notificationBuilder);
        when(notificationBuilder.setContentText(anyString())).thenReturn(notificationBuilder);
        when(notificationBuilder.setSmallIcon(anyInt())).thenReturn(notificationBuilder);
        when(notificationBuilder.setContentIntent(any(PendingIntent.class))).thenReturn(notificationBuilder);

        /* Simulate task. */
        waitCompletionTask();

        /* Verify notification. */
        verify(mFirstActivity, never()).startActivity(installIntent);
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_DOWNLOAD_URI, "file://localFile");
        verify(mNotificationManager).notify(eq(Updates.getNotificationId()), any(Notification.class));
        verifyNoMoreInteractions(mNotificationManager);

        /* Restart app should pop install U.I. and cancel notification and pop a new dialog then a new download. */
        doThrow(new ActivityNotFoundException()).when(mFirstActivity).startActivity(installIntent);
        when(mDownloadManager.enqueue(mDownloadRequest)).thenReturn(DOWNLOAD_ID + 1);
        Updates.getInstance().onStarted(mContext, "", mock(Channel.class));
        Updates.getInstance().onActivityResumed(mFirstActivity);

        /* Verify. */
        verify(mFirstActivity).startActivity(installIntent);
        verify(mNotificationManager).cancel(Updates.getNotificationId());
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        verify(mDownloadManager).remove(DOWNLOAD_ID);

        /* Verify workflow restarted right after failure. */
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder, times(2)).setPositiveButton(eq(R.string.mobile_center_updates_update_dialog_download), clickListener.capture());
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_POSITIVE);
        waitDownloadTask();
        verifyStatic();
        PreferencesStorage.putLong(PREFERENCE_KEY_DOWNLOAD_ID, DOWNLOAD_ID + 1);

        /* Check no duplicate cleaning tasks, i.e. happened only once. */
        verify(mNotificationManager).cancel(Updates.getNotificationId());
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        verify(mDownloadManager).remove(DOWNLOAD_ID);
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", 0);
    }
}
