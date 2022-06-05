/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.install.session;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.ParcelFileDescriptor;

import com.microsoft.appcenter.distribute.install.ReleaseInstaller;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@PrepareForTest({
        InstallStatusReceiver.class,
        PackageInstallerListener.class,
        SessionReleaseInstaller.class
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

        /* Mock session. */
        when(mPackageInstaller.openSession(anyInt())).thenReturn(mSession);
        when(mSession.openWrite(anyString(), anyLong(), anyLong())).thenReturn(mOutputStream);

        mockStatic(InstallStatusReceiver.class);
        whenNew(InstallStatusReceiver.class).withAnyArguments().thenReturn(mInstallStatusReceiver);
        when(InstallStatusReceiver.getInstallStatusIntentSender(any(Context.class), anyInt()))
                .thenReturn(mock(IntentSender.class));

        mockStatic(PackageInstallerListener.class);
        whenNew(PackageInstallerListener.class).withAnyArguments().thenReturn(mPackageInstallerListener);

        when(mHandler.post(any(Runnable.class))).then(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock invocation) {
                invocation.<Runnable>getArgument(0).run();
                return true;
            }
        });

        mInstaller = new SessionReleaseInstaller(mContext, mHandler, mListener);
    }

    @Test
    public void install() throws IOException {
        /* Mock data. */
        when(mInputStream.read(any())).thenReturn(10).thenReturn(-1);

        Uri uri = mock(Uri.class);
        mInstaller.install(uri);

        verify(mHandler).post(any(Runnable.class));
        verify(mContext).registerReceiver(eq(mInstallStatusReceiver), any());
        verify(mPackageInstaller).registerSessionCallback(eq(mPackageInstallerListener));
        verify(mInputStream).close();
        verify(mOutputStream).close();
        verify(mSession).commit(any(IntentSender.class));
        verify(mSession, never()).abandon();
        verify(mSession).close();
        verifyNoInteractions(mListener);
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
    public void clear() {
        mInstaller.clear();
    }

    @Test
    public void testToString() {
        assertEquals("PackageInstaller", mInstaller.toString());
    }

    @Test
    public void onInstallProgress() {
        mInstaller.onInstallProgress(SESSION_ID);
    }

    @Test
    public void onInstallConfirmation() {
        mInstaller.onInstallConfirmation(SESSION_ID, mock(Intent.class));
    }

    @Test
    public void onInstallError() {
        mInstaller.onInstallError(SESSION_ID, "Some error");
    }

    @Test
    public void onInstallCancel() {
        mInstaller.onInstallCancel(SESSION_ID);
    }
}