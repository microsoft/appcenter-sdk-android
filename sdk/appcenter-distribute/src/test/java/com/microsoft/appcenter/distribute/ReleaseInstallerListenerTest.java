/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.text.NumberFormat;

@PrepareForTest({
        AppCenterLog.class,
        Distribute.class,
        HandlerUtils.class,
        ReleaseInstallerListener.class,
        Toast.class
})
public class ReleaseInstallerListenerTest {

    private static final int SESSION_ID = 42;

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    private Context mContext;

    @Mock
    private Distribute mDistribute;

    @Mock
    private Toast mToast;

    @Mock
    private DownloadManager mDownloadManager;

    @Mock
    private android.app.ProgressDialog mProgressDialog;

    private ReleaseInstallerListener mReleaseInstallerListener;

    @Before
    public void setUp() throws Exception {

        /* Mock static classes. */
        mockStatic(AppCenterLog.class);
        mockStatic(Distribute.class);
        mockStatic(HandlerUtils.class);
        mockStatic(Toast.class);

        /* Mock progress dialog. */
        whenNew(android.app.ProgressDialog.class).withAnyArguments().thenReturn(mProgressDialog);
        when(mProgressDialog.isIndeterminate()).thenReturn(false);

        /* Mock toast. */
        when(mContext.getString(anyInt())).thenReturn("localized_message");
        when(Toast.makeText(any(Context.class), anyString(), anyInt())).thenReturn(mToast);

        /* Mock Distribute. */
        when(Distribute.getInstance()).thenReturn(mDistribute);
        doNothing().when(mDistribute).notifyInstallProgress(anyBoolean());

        /* Mock download manager. */
        when(mDownloadManager.openDownloadedFile(anyLong())).thenReturn(mock(ParcelFileDescriptor.class));
        when(mContext.getSystemService(anyString())).thenReturn(mDownloadManager);

        /* Create installer listener. */
        mReleaseInstallerListener = new ReleaseInstallerListener(mContext);

        /* Init install progress dialog. */
        mReleaseInstallerListener.showInstallProgressDialog(mock(Activity.class));

        /* Verify call methods. */
        verify(mProgressDialog).setProgressPercentFormat(isNull());
        verify(mProgressDialog).setProgressNumberFormat(isNull());
        verify(mProgressDialog).setIndeterminate(anyBoolean());
    }

    @Test
    public void releaseInstallProcessOnFinishFailureWithContext() {

        /* Mock progress dialog. */
        when(mProgressDialog.isIndeterminate()).thenReturn(true);

        /* Emulate session status. */
        mReleaseInstallerListener.onCreated(SESSION_ID);

        /* Verify that installer process was triggered in the Distribute. */
        mReleaseInstallerListener.onActiveChanged(SESSION_ID, true);
        verify(mDistribute).notifyInstallProgress(eq(true));

        /* Verity that progress dialog was updated. */
        mReleaseInstallerListener.onProgressChanged(SESSION_ID, 1);

        /* Verify that the handler was called and catch runnable. */
        verifyStatic(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));

        /* Verify that progress dialog was closed after finish install process. */
        mReleaseInstallerListener.onFinished(SESSION_ID, false);

        /* Verify that the handler was called again. */
        ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
        verifyStatic(HandlerUtils.class, times(2));
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verify that installer process was triggered in the Distribute again. */
        verify(mToast).show();
    }

    @Test
    public void releaseInstallerProcessWhenProgressDialogNull() {

        /* Emulate session status. */
        mReleaseInstallerListener.onCreated(SESSION_ID);
        mReleaseInstallerListener.onBadgingChanged(SESSION_ID);

        /* Verify that installer process was triggered in the Distribute. */
        mReleaseInstallerListener.onActiveChanged(SESSION_ID, true);
        verify(mDistribute).notifyInstallProgress(eq(true));

        /* Hide dialog. */
        mReleaseInstallerListener.hideInstallProgressDialog();

        /* Verify that runnable was called. */
        ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
        verifyStatic(HandlerUtils.class);
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verity that progress dialog was updated. */
        mReleaseInstallerListener.onProgressChanged(SESSION_ID, 1);

        /* Verify that the handler was called and catch runnable. */
        runnable = ArgumentCaptor.forClass(Runnable.class);
        verifyStatic(HandlerUtils.class, times(2));
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verify that the progress dialog was updated. */
        verify(mProgressDialog, never()).setProgress(anyInt());

        /* Verify that progress dialog was closed after finish install process. */
        mReleaseInstallerListener.onFinished(SESSION_ID, true);

        /* Verify that the handler was called again. */
        verifyStatic(HandlerUtils.class, times(3));
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verify that installer process was triggered in the Distribute again. */
        verify(mDistribute).notifyInstallProgress(eq(false));
    }

    @Test
    public void releaseInstallerProcessWhenDialogIsIndeterminate() {

        /* Mock progress dialog. */
        when(mProgressDialog.isIndeterminate()).thenReturn(true);

        /* Emulate session status. */
        mReleaseInstallerListener.onCreated(SESSION_ID);
        mReleaseInstallerListener.onBadgingChanged(SESSION_ID);

        /* Verify that installer process was triggered in the Distribute. */
        mReleaseInstallerListener.onActiveChanged(SESSION_ID, true);
        verify(mDistribute).notifyInstallProgress(eq(true));

        /* Verity that progress dialog was updated. */
        mReleaseInstallerListener.onProgressChanged(SESSION_ID, 1);

        /* Verify that the handler was called and catch runnable. */
        ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
        verifyStatic(HandlerUtils.class);
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verify that the progress dialog was updated. */
        verify(mProgressDialog).setProgress(anyInt());
        verify(mProgressDialog).setMax(anyInt());
        verify(mProgressDialog).setProgressPercentFormat(any(NumberFormat.class));
        verify(mProgressDialog).setProgressNumberFormat(anyString());
        verify(mProgressDialog, times(2)).setIndeterminate(anyBoolean());

        /* Verify that progress dialog was closed after finish install process. */
        mReleaseInstallerListener.onFinished(SESSION_ID, true);

        /* Verify that the handler was called again. */
        verifyStatic(HandlerUtils.class, times(2));
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verify that installer process was triggered in the Distribute again. */
        verify(mDistribute).notifyInstallProgress(eq(false));
    }

    @Test
    public void releaseInstallerProcessWhenWithContext() {

        /* Emulate session status. */
        mReleaseInstallerListener.onCreated(SESSION_ID);
        mReleaseInstallerListener.onBadgingChanged(SESSION_ID);

        /* Verify that installer process was triggered in the Distribute. */
        mReleaseInstallerListener.onActiveChanged(SESSION_ID, true);
        verify(mDistribute).notifyInstallProgress(eq(true));

        /* Verity that progress dialog was updated. */
        mReleaseInstallerListener.onProgressChanged(SESSION_ID, 1);

        /* Verify that the handler was called and catch runnable. */
        ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
        verifyStatic(HandlerUtils.class);
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verify that the progress dialog was updated. */
        verify(mProgressDialog).setProgress(anyInt());

        /* Verify that progress dialog was closed after finish install process. */
        mReleaseInstallerListener.onFinished(SESSION_ID, true);

        /* Verify that the handler was called again. */
        verifyStatic(HandlerUtils.class, times(2));
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verify that installer process was triggered in the Distribute again. */
        verify(mDistribute).notifyInstallProgress(eq(false));
    }

    @Test
    public void releaseInstallerHideDialogTwice() {

        /* Hide dialog. */
        mReleaseInstallerListener.hideInstallProgressDialog();

        /* Try to hide dialog again. */
        mReleaseInstallerListener.hideInstallProgressDialog();

        /* Verify that runnable was called once only. */
        verifyStatic(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));
    }
}
