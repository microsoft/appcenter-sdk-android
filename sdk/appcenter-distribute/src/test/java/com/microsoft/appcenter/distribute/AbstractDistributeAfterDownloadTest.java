/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

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

import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.distribute.download.ReleaseDownloaderFactory;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.test.TestUtils;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.After;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.concurrent.Semaphore;

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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("CanBeFinal")
@PrepareForTest({AsyncTaskUtils.class, DistributeUtils.class, ReleaseDownloadListener.class})
public class AbstractDistributeAfterDownloadTest extends AbstractDistributeTest {

    @Mock
    Uri mDownloadUrl;

    @Mock
    NotificationManager mNotificationManager;


    void setUpDownload(boolean mandatoryUpdate) throws Exception {

        /* Allow unknown sources. */
        when(InstallerUtils.isUnknownSourcesEnabled(any(Context.class))).thenReturn(true);

        /* Mock notification manager. */
        when(mContext.getSystemService(NOTIFICATION_SERVICE)).thenReturn(mNotificationManager);

        /* Mock updates to storage. */
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                when(SharedPreferencesManager.getLong(invocation.getArguments()[0].toString(), INVALID_DOWNLOAD_IDENTIFIER)).thenReturn((Long) invocation.getArguments()[1]);
                return null;
            }
        }).when(SharedPreferencesManager.class);
        SharedPreferencesManager.putLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), anyLong());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                when(SharedPreferencesManager.getLong(invocation.getArguments()[0].toString(), INVALID_DOWNLOAD_IDENTIFIER)).thenReturn(INVALID_DOWNLOAD_IDENTIFIER);
                return null;
            }
        }).when(SharedPreferencesManager.class);
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                when(SharedPreferencesManager.getInt(invocation.getArguments()[0].toString(), DOWNLOAD_STATE_COMPLETED)).thenReturn((Integer) invocation.getArguments()[1]);
                return null;
            }
        }).when(SharedPreferencesManager.class);
        SharedPreferencesManager.putInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), anyInt());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                when(SharedPreferencesManager.getInt(invocation.getArguments()[0].toString(), DOWNLOAD_STATE_COMPLETED)).thenReturn(DOWNLOAD_STATE_COMPLETED);
                return null;
            }
        }).when(SharedPreferencesManager.class);
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                when(SharedPreferencesManager.getString(invocation.getArguments()[0].toString())).thenReturn(invocation.getArguments()[1].toString());
                return null;
            }
        }).when(SharedPreferencesManager.class);
        SharedPreferencesManager.putString(eq(PREFERENCE_KEY_RELEASE_DETAILS), anyString());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                when(SharedPreferencesManager.getString(invocation.getArguments()[0].toString())).thenReturn(null);
                return null;
            }
        }).when(SharedPreferencesManager.class);
        SharedPreferencesManager.remove(PREFERENCE_KEY_RELEASE_DETAILS);

        /* Mock everything that triggers a download. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("some group");
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("mock", null);
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

        /* Click on dialog. */
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder).setPositiveButton(eq(R.string.appcenter_distribute_update_dialog_download), clickListener.capture());
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_POSITIVE);
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
    }
}
