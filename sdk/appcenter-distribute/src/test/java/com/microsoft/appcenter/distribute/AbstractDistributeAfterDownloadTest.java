package com.microsoft.appcenter.distribute;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;

import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpClientNetworkStateHandler;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.test.TestUtils;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.storage.StorageHelper.PreferencesStorage;

import org.junit.After;
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
import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_COMPLETED;
import static com.microsoft.appcenter.distribute.DistributeConstants.INVALID_DOWNLOAD_IDENTIFIER;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DISTRIBUTION_GROUP_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_STATE;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_RELEASE_DETAILS;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_UPDATE_TOKEN;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("CanBeFinal")
@PrepareForTest({AsyncTaskUtils.class, DistributeUtils.class, DownloadTask.class, CheckDownloadTask.class, RemoveDownloadTask.class})
public class AbstractDistributeAfterDownloadTest extends AbstractDistributeTest {

    static final long DOWNLOAD_ID = 42;


    static final ArgumentMatcher<CheckDownloadTask> sCheckCompleteTask = new ArgumentMatcher<CheckDownloadTask>() {

        @Override
        public boolean matches(Object argument) {
            return argument instanceof CheckDownloadTask;
        }
    };

    @Mock
    Uri mDownloadUrl;

    @Mock
    DownloadManager mDownloadManager;

    @Mock
    NotificationManager mNotificationManager;

    @Mock
    DownloadManager.Request mDownloadRequest;

    AtomicReference<DownloadTask> mDownloadTask;

    Semaphore mCheckDownloadBeforeSemaphore;

    Semaphore mCheckDownloadAfterSemaphore;

    AtomicReference<CheckDownloadTask> mCompletionTask;

    private Semaphore mDownloadBeforeSemaphore;

    private Semaphore mDownloadAfterSemaphore;

    void setUpDownload(boolean mandatoryUpdate) throws Exception {

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
            public Void answer(InvocationOnMock invocation) {
                when(PreferencesStorage.getLong(invocation.getArguments()[0].toString(), INVALID_DOWNLOAD_IDENTIFIER)).thenReturn((Long) invocation.getArguments()[1]);
                return null;
            }
        }).when(PreferencesStorage.class);
        PreferencesStorage.putLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), anyLong());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                when(PreferencesStorage.getLong(invocation.getArguments()[0].toString(), INVALID_DOWNLOAD_IDENTIFIER)).thenReturn(INVALID_DOWNLOAD_IDENTIFIER);
                return null;
            }
        }).when(PreferencesStorage.class);
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                when(PreferencesStorage.getInt(invocation.getArguments()[0].toString(), DOWNLOAD_STATE_COMPLETED)).thenReturn((Integer) invocation.getArguments()[1]);
                return null;
            }
        }).when(PreferencesStorage.class);
        PreferencesStorage.putInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), anyInt());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                when(PreferencesStorage.getInt(invocation.getArguments()[0].toString(), DOWNLOAD_STATE_COMPLETED)).thenReturn(DOWNLOAD_STATE_COMPLETED);
                return null;
            }
        }).when(PreferencesStorage.class);
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                when(PreferencesStorage.getString(invocation.getArguments()[0].toString())).thenReturn(invocation.getArguments()[1].toString());
                return null;
            }
        }).when(PreferencesStorage.class);
        PreferencesStorage.putString(eq(PREFERENCE_KEY_RELEASE_DETAILS), anyString());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                when(PreferencesStorage.getString(invocation.getArguments()[0].toString())).thenReturn(null);
                return null;
            }
        }).when(PreferencesStorage.class);
        PreferencesStorage.remove(PREFERENCE_KEY_RELEASE_DETAILS);

        /* Mock everything that triggers a download. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("some group");
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("mock");
                return mock(ServiceCall.class);
            }
        });
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn(4);
        when(releaseDetails.getVersion()).thenReturn(7);
        when(releaseDetails.getDownloadUrl()).thenReturn(mDownloadUrl);
        when(releaseDetails.isMandatoryUpdate()).thenReturn(mandatoryUpdate);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);
        mockStatic(AsyncTaskUtils.class);
        start();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Mock download asyncTask. */
        mDownloadBeforeSemaphore = new Semaphore(0);
        mDownloadAfterSemaphore = new Semaphore(0);
        mDownloadTask = new AtomicReference<>();
        when(AsyncTaskUtils.execute(anyString(), argThat(new ArgumentMatcher<DownloadTask>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof DownloadTask;
            }
        }), Mockito.<Void>anyVararg())).then(new Answer<DownloadTask>() {

            @Override
            public DownloadTask answer(InvocationOnMock invocation) {
                final DownloadTask task = spy((DownloadTask) invocation.getArguments()[1]);
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
        when(AsyncTaskUtils.execute(anyString(), argThat(new ArgumentMatcher<RemoveDownloadTask>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof RemoveDownloadTask;
            }
        }), Mockito.<Void>anyVararg())).then(new Answer<RemoveDownloadTask>() {

            @Override
            public RemoveDownloadTask answer(InvocationOnMock invocation) {
                final RemoveDownloadTask task = (RemoveDownloadTask) invocation.getArguments()[1];
                task.doInBackground();
                return task;
            }
        });

        /* Mock download completion async task. */
        mCheckDownloadBeforeSemaphore = new Semaphore(0);
        mCheckDownloadAfterSemaphore = new Semaphore(0);
        mCompletionTask = new AtomicReference<>();
        when(AsyncTaskUtils.execute(anyString(), argThat(sCheckCompleteTask), Mockito.<Void>anyVararg())).then(new Answer<CheckDownloadTask>() {

            @Override
            public CheckDownloadTask answer(InvocationOnMock invocation) {
                final CheckDownloadTask task = spy((CheckDownloadTask) invocation.getArguments()[1]);
                mCompletionTask.set(task);
                new Thread() {

                    @Override
                    public void run() {
                        mCheckDownloadBeforeSemaphore.acquireUninterruptibly();
                        task.onPostExecute(task.doInBackground());
                        mCheckDownloadAfterSemaphore.release();
                    }
                }.start();
                return task;
            }
        });

        /* Click on dialog. */
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder).setPositiveButton(eq(R.string.appcenter_distribute_update_dialog_download), clickListener.capture());
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_POSITIVE);
    }

    void waitDownloadTask() {
        mDownloadBeforeSemaphore.release();
        mDownloadAfterSemaphore.acquireUninterruptibly();
    }

    void waitCheckDownloadTask() {
        mCheckDownloadBeforeSemaphore.release();
        mCheckDownloadAfterSemaphore.acquireUninterruptibly();
    }

    void completeDownload() {
        Intent completionIntent = mock(Intent.class);
        when(completionIntent.getAction()).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        when(completionIntent.getLongExtra(eq(EXTRA_DOWNLOAD_ID), anyLong())).thenReturn(DOWNLOAD_ID);
        new DownloadManagerReceiver().onReceive(mContext, completionIntent);
    }

    @NonNull
    Cursor mockSuccessCursor() {
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
    @SuppressLint("NewApi")
    Notification.Builder mockNotificationBuilderChain() throws Exception {
        Notification.Builder notificationBuilder = mock(Notification.Builder.class);
        whenNew(Notification.Builder.class).withAnyArguments().thenReturn(notificationBuilder);
        when(notificationBuilder.setTicker(anyString())).thenReturn(notificationBuilder);
        when(notificationBuilder.setContentTitle(anyString())).thenReturn(notificationBuilder);
        when(notificationBuilder.setContentText(anyString())).thenReturn(notificationBuilder);
        when(notificationBuilder.setSmallIcon(anyInt())).thenReturn(notificationBuilder);
        when(notificationBuilder.setContentIntent(any(PendingIntent.class))).thenReturn(notificationBuilder);
        when(notificationBuilder.setChannelId(anyString())).thenReturn(notificationBuilder);
        return notificationBuilder;
    }

    @NonNull
    Intent mockInstallIntent() throws Exception {
        Intent installIntent = mock(Intent.class);
        whenNew(Intent.class).withArguments(Intent.ACTION_INSTALL_PACKAGE).thenReturn(installIntent);
        when(installIntent.resolveActivity(any(PackageManager.class))).thenReturn(mock(ComponentName.class));
        return installIntent;
    }

    void restartActivity() {
        Distribute.getInstance().onActivityStopped(mActivity);
        Distribute.getInstance().onActivityDestroyed(mActivity);
        Distribute.getInstance().onActivityCreated(mActivity, null);
        Distribute.getInstance().onActivityResumed(mActivity);
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", 0);
        checkSemaphoreSanity(mCheckDownloadBeforeSemaphore);
        checkSemaphoreSanity(mCheckDownloadAfterSemaphore);
        checkSemaphoreSanity(mDownloadBeforeSemaphore);
        checkSemaphoreSanity(mDownloadAfterSemaphore);
    }

    void checkSemaphoreSanity(Semaphore semaphore) {
        assertEquals(0, semaphore.availablePermits());
        assertEquals(0, semaphore.getQueueLength());
    }
}
