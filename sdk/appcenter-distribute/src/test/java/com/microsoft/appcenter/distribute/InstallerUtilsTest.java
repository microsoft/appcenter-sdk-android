/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@PrepareForTest(PendingIntent.class)
public class InstallerUtilsTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    private Context mContext;

    @Mock
    private PackageInstaller mMockPackageInstaller;

    @Mock
    private OutputStream mOutputStream;

    @Mock
    private PackageInstaller.Session mSession;

    @Mock
    private InputStream mData;

    @Before
    public void setUp() throws IOException {

        /* Mock package installer. */
        PackageManager mockPackageManager = mock(PackageManager.class);
        when(mockPackageManager.getPackageInstaller()).thenReturn(mMockPackageInstaller);
        when(mContext.getPackageManager()).thenReturn(mockPackageManager);

        /* Mock session. */
        when(mMockPackageInstaller.openSession(anyInt())).thenReturn(mSession);
        when(mSession.openWrite(anyString(), anyInt(), anyInt())).thenReturn(mOutputStream);
    }

    @Test
    public void installPackage() throws IOException {

        /* Mock intent. */
        mockStatic(PendingIntent.class);
        PendingIntent mockIntent = mock(PendingIntent.class);
        when(mockIntent.getIntentSender()).thenReturn(mock(IntentSender.class));
        when(PendingIntent.getBroadcast(any(Context.class), anyInt(), any(Intent.class), anyInt())).thenReturn(mockIntent);

        /* Mock session callback. */
        PackageInstaller.SessionCallback mockSessionCallback = mock(PackageInstaller.SessionCallback.class);

        /* Mock data. */
        when(mData.read(Matchers.<byte[]>anyObject())).thenReturn(10).thenReturn(-1);

        /* Call install. */
        InstallerUtils.installPackage(mData, mContext, mockSessionCallback);

        /* Verify. */
        verify(mMockPackageInstaller).registerSessionCallback(eq(mockSessionCallback));
        verify(mData).close();
        verify(mOutputStream).close();
        verify(mSession).commit(any(IntentSender.class));
        verify(mSession, never()).abandon();
        verify(mSession).close();
    }

    @Test
    public void throwIOExceptionWhenTryToOpenWriteSession() throws IOException {

        /* Throw error when try to open write to session. */
        when(mSession.openWrite(anyString(), anyLong(), anyLong())).thenThrow(new IOException());

        /* Call install method. */
        InstallerUtils.installPackage(mData, mContext, null);

        /* Verify. */
        verify(mData, never()).close();
        verify(mSession).abandon();
    }

    @Test
    public void throwIOExceptionWhenTryToCreateSession() throws IOException {

        /* Throw error when try to create session. */
        when(mMockPackageInstaller.createSession(any(PackageInstaller.SessionParams.class))).thenThrow(new IOException());

        /* Call install method. */
        InstallerUtils.installPackage(mData, mContext, null);

        /* Verify that the session wasn't created. */
        verify(mMockPackageInstaller, never()).openSession(anyInt());
    }
}
