/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.install.session;

import static android.app.Activity.RESULT_OK;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;

import com.microsoft.appcenter.distribute.install.ReleaseInstaller;
import com.microsoft.appcenter.distribute.install.ReleaseInstallerActivity;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;
import com.microsoft.appcenter.utils.async.AppCenterFuture;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.powermock.reflect.Whitebox;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@PrepareForTest({
        InstallStatusReceiver.class,
        PackageInstallerListener.class,
        ReleaseInstallerActivity.class,
        SessionReleaseInstaller.class,
})
public class SessionReleaseInstallerTest {

    private static final int SESSION_ID = 42;

    @Rule
    public PowerMockRule mRule = new PowerMockRule();

    @Mock
    private Context mContext;

    @Mock
    private Handler mHandler;

    @Mock
    private ReleaseInstaller.Listener mListener;

    @Mock
    private PackageInstaller mPackageInstaller;

    @Mock
    private InstallStatusReceiver mInstallStatusReceiver;

    @Mock
    private PackageInstallerListener mPackageInstallerListener;

    @Mock
    private FileInputStream mInputStream;

    @Mock
    private OutputStream mOutputStream;

    @Mock
    private PackageInstaller.Session mSession;

    @Mock
    private AppCenterFuture<ReleaseInstallerActivity.Result> mResultFuture;

    @Captor
    private ArgumentCaptor<AppCenterConsumer<ReleaseInstallerActivity.Result>> mConsumerArgumentCaptor;

    private SessionReleaseInstaller mInstaller;

    @Before
    public void setUp() throws Exception {

        /* Mock package installer. */
        PackageManager mockPackageManager = mock(PackageManager.class);
        when(mockPackageManager.getPackageInstaller()).thenReturn(mPackageInstaller);
        when(mContext.getPackageManager()).thenReturn(mockPackageManager);

        /* Mock input file. */
        ContentResolver contentResolver = mock(ContentResolver.class);
        when(mContext.getContentResolver()).thenReturn(contentResolver);
        ParcelFileDescriptor fileDescriptor = mock(ParcelFileDescriptor.class);
        when(contentResolver.openFileDescriptor(any(Uri.class), eq("r"))).thenReturn(fileDescriptor);
        whenNew(FileInputStream.class).withAnyArguments().thenReturn(mInputStream);
        when(mInputStream.read(any())).thenReturn(10).thenReturn(-1);

        /* Mock session. */
        when(mPackageInstaller.openSession(anyInt())).thenReturn(mSession);
        when(mPackageInstaller.createSession(any(PackageInstaller.SessionParams.class))).thenReturn(SESSION_ID);
        when(mSession.openWrite(anyString(), anyLong(), anyLong())).thenReturn(mOutputStream);

        /* Mock install status receiver. */
        mockStatic(InstallStatusReceiver.class);
        whenNew(InstallStatusReceiver.class).withAnyArguments().thenReturn(mInstallStatusReceiver);
        when(InstallStatusReceiver.getInstallStatusIntentSender(any(Context.class), anyInt()))
                .thenReturn(mock(IntentSender.class));

        /* Mock package installer listener. */
        mockStatic(PackageInstallerListener.class);
        whenNew(PackageInstallerListener.class).withAnyArguments().thenReturn(mPackageInstallerListener);

        /* Mock activity. */
        mockStatic(ReleaseInstallerActivity.class);
        when(ReleaseInstallerActivity.startActivityForResult(eq(mContext), any(Intent.class)))
                .thenReturn(mResultFuture);

        /* Run posted runnable immediately. */
        when(mHandler.post(any(Runnable.class))).then(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock invocation) {
                invocation.<Runnable>getArgument(0).run();
                return true;
            }
        });

        /* Create SessionReleaseInstaller instance. */
        mInstaller = new SessionReleaseInstaller(mContext, mHandler, mListener);
    }

    @Test
    public void installSuccessForSV2() throws IOException {
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.S_V2);

        Uri uri = mock(Uri.class);
        mInstaller.install(uri);

        /* Verify that all required things called. */
        verify(mHandler).post(any(Runnable.class));
        verify(mContext).registerReceiver(eq(mInstallStatusReceiver), any());
        verify(mPackageInstaller).registerSessionCallback(eq(mPackageInstallerListener));
        verify(mInputStream).close();
        verify(mOutputStream).close();
        verify(mSession).commit(any(IntentSender.class));
        verify(mSession, never()).abandon();
        verify(mSession).close();
        verifyNoInteractions(mListener);

        /* Try to star install second time. It's valid case if something goes wrong with previous try. */
        mInstaller.install(uri);

        /* Cancel previous session and re-use callbacks. */
        verify(mContext).registerReceiver(eq(mInstallStatusReceiver), any());
        verify(mPackageInstaller).registerSessionCallback(eq(mPackageInstallerListener));
        verify(mPackageInstaller).abandonSession(eq(SESSION_ID));
    }

    @Test
    public void installSuccessForTiramisu() throws IOException {
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.TIRAMISU);

        Uri uri = mock(Uri.class);
        mInstaller.install(uri);

        /* Verify that all required things called. */
        verify(mHandler).post(any(Runnable.class));
        verify(mContext).registerReceiver(eq(mInstallStatusReceiver), any(), anyInt());
        verify(mPackageInstaller).registerSessionCallback(eq(mPackageInstallerListener));
        verify(mInputStream).close();
        verify(mOutputStream).close();
        verify(mSession).commit(any(IntentSender.class));
        verify(mSession, never()).abandon();
        verify(mSession).close();
        verifyNoInteractions(mListener);

        /* Try to star install second time. It's valid case if something goes wrong with previous try. */
        mInstaller.install(uri);

        /* Cancel previous session and re-use callbacks. */
        verify(mContext).registerReceiver(eq(mInstallStatusReceiver), any(), anyInt());
        verify(mPackageInstaller).registerSessionCallback(eq(mPackageInstallerListener));
        verify(mPackageInstaller).abandonSession(eq(SESSION_ID));
    }

    @Test
    public void throwIOExceptionWhenTryToOpenWriteSession() throws IOException {

        /* Throw error when try to open write to session. */
        when(mSession.openWrite(anyString(), anyLong(), anyLong()))
                .thenThrow(new IOException());

        /* Call install method. */
        Uri uri = mock(Uri.class);
        mInstaller.install(uri);

        /* Verify. */
        verify(mInputStream, never()).close();
        verify(mSession).abandon();
    }

    @Test
    public void throwIOExceptionWhenTryToCreateSession() throws IOException {

        /* Throw error when try to create session. */
        when(mPackageInstaller.createSession(any(PackageInstaller.SessionParams.class)))
                .thenThrow(new IOException());

        /* Call install method. */
        Uri uri = mock(Uri.class);
        mInstaller.install(uri);

        /* Verify that the session wasn't created. */
        verify(mPackageInstaller, never()).openSession(anyInt());
    }

    @Test
    public void tooManyActiveSessions() throws Exception {
        when(mPackageInstaller.openSession(anyInt()))
                .thenThrow(new IllegalStateException())
                .thenReturn(mSession);

        /* Mock some active sessions. */
        List<PackageInstaller.SessionInfo> mySessions = new ArrayList<>();
        PackageInstaller.SessionInfo session1 = mock(PackageInstaller.SessionInfo.class);
        when(session1.getSessionId()).thenReturn(1);
        mySessions.add(session1);
        PackageInstaller.SessionInfo session2 = mock(PackageInstaller.SessionInfo.class);
        when(session2.getSessionId()).thenReturn(2);
        mySessions.add(session2);
        when(mPackageInstaller.getMySessions()).thenReturn(mySessions);

        /* Call install method. */
        Uri uri = mock(Uri.class);
        mInstaller.install(uri);

        /* Abandon previous sessions. */
        verify(mPackageInstaller).abandonSession(eq(1));
        verify(mPackageInstaller).abandonSession(eq(2));
    }

    @Test
    public void clear() {

        /* Clear before install does nothing. */
        mInstaller.clear();
        verifyNoInteractions(mContext);
        verifyNoInteractions(mPackageInstaller);

        /* Call install method. */
        Uri uri = mock(Uri.class);
        mInstaller.install(uri);

        /* Registering callbacks. */
        verify(mContext).registerReceiver(eq(mInstallStatusReceiver), any(), anyInt());
        verify(mPackageInstaller).registerSessionCallback(eq(mPackageInstallerListener));

        /* Clear after start should clear registered callbacks and abandon the session. */
        mInstaller.clear();

        /* Verify. */
        verify(mContext).unregisterReceiver(eq(mInstallStatusReceiver));
        verify(mPackageInstaller).unregisterSessionCallback(eq(mPackageInstallerListener));
        verify(mPackageInstaller).abandonSession(eq(SESSION_ID));
    }

    @Test
    public void testToString() {
        assertEquals("PackageInstaller", mInstaller.toString());
    }

    @Test
    public void ignoreAnotherSessionEvents() {
        final int ANOTHER_SESSION_ID = 7;
        mInstaller.onInstallProgress(ANOTHER_SESSION_ID);
        mInstaller.onInstallConfirmation(ANOTHER_SESSION_ID, mock(Intent.class));
        mInstaller.onInstallError(ANOTHER_SESSION_ID, "Some error");
        mInstaller.onInstallCancel(ANOTHER_SESSION_ID);

        /* No-op before starting install or from another session. */
        verifyNoInteractions(mListener);
    }

    @Test
    public void installConfirmation() {
        Uri uri = mock(Uri.class);
        mInstaller.install(uri);

        /* Ask for user confirmation. */
        Intent confirmIntent = mock(Intent.class);
        mInstaller.onInstallConfirmation(SESSION_ID, confirmIntent);
        verify(mResultFuture).thenAccept(mConsumerArgumentCaptor.capture());

        /* Pass result to captured callback. */
        ReleaseInstallerActivity.Result result = new ReleaseInstallerActivity.Result(RESULT_OK, null);
        mConsumerArgumentCaptor.getValue().accept(result);

        /* Capture delayed action. */
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mHandler).postDelayed(runnableCaptor.capture(), anyLong());

        /* Run without progress cancels the process. */
        runnableCaptor.getValue().run();
        verify(mListener).onCancel();

        /* Run with progress event does nothing. */
        mInstaller.onInstallProgress(SESSION_ID);
        runnableCaptor.getValue().run();
        verifyNoMoreInteractions(mListener);
    }

    @Test
    public void cannotStartConfirmationActivity() {
        when(ReleaseInstallerActivity.startActivityForResult(eq(mContext), any(Intent.class)))
                .thenReturn(null);
        Uri uri = mock(Uri.class);
        mInstaller.install(uri);

        /* Ask for user confirmation. */
        Intent confirmIntent = mock(Intent.class);
        mInstaller.onInstallConfirmation(SESSION_ID, confirmIntent);

        /* Verify. */
        verifyStatic(ReleaseInstallerActivity.class);
        ReleaseInstallerActivity.startActivityForResult(eq(mContext), eq(confirmIntent));

        /*
         * It should not be a case, it's expected to have no more than one installer in time.
         * So, don't inform listener to avoid break state of the service.
         */
        verifyNoInteractions(mListener);
    }

    @Test
    public void installError() {
        Uri uri = mock(Uri.class);
        mInstaller.install(uri);
        mInstaller.onInstallError(SESSION_ID, "Some error");

        /* Verify error callback. */
        verify(mListener).onError(anyString());
        verifyNoMoreInteractions(mListener);
    }

    @Test
    public void installCancel() {
        Uri uri = mock(Uri.class);
        mInstaller.install(uri);
        mInstaller.onInstallCancel(SESSION_ID);

        /* Verify cancel callback. */
        verify(mListener).onCancel();
        verifyNoMoreInteractions(mListener);
    }
}
