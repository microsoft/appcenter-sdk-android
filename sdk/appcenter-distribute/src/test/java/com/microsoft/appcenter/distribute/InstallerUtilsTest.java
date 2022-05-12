/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import static android.app.PendingIntent.FLAG_MUTABLE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;

import org.junit.Assert;
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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@PrepareForTest({
        InstallerUtils.class,
        PendingIntent.class,
        Settings.class
})
public class InstallerUtilsTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    private Context mContext;

    @Mock
    private PackageInstaller mMockPackageInstaller;

    @Mock
    private FileInputStream mInputStream;

    @Mock
    private OutputStream mOutputStream;

    @Mock
    private PackageInstaller.Session mSession;

    @Before
    public void setUp() throws Exception {

        /* Mock package installer. */
        PackageManager mockPackageManager = mock(PackageManager.class);
        when(mockPackageManager.getPackageInstaller()).thenReturn(mMockPackageInstaller);
        when(mContext.getPackageManager()).thenReturn(mockPackageManager);

        /* Mock input file. */
        ContentResolver contentResolver = mock(ContentResolver.class);
        when(mContext.getContentResolver()).thenReturn(contentResolver);
        ParcelFileDescriptor fileDescriptor = mock(ParcelFileDescriptor.class);
        when(contentResolver.openFileDescriptor(any(Uri.class), eq("r"))).thenReturn(fileDescriptor);
        whenNew(FileInputStream.class).withAnyArguments().thenReturn(mInputStream);

        /* Mock session. */
        when(mMockPackageInstaller.openSession(anyInt())).thenReturn(mSession);
        when(mSession.openWrite(anyString(), anyLong(), anyLong())).thenReturn(mOutputStream);
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
        when(mInputStream.read(any())).thenReturn(10).thenReturn(-1);

        /* Call install. */
        InstallerUtils.installPackage(mock(Uri.class), mContext, mockSessionCallback);

        /* Verify. */
        verify(mMockPackageInstaller).registerSessionCallback(eq(mockSessionCallback));
        verify(mInputStream).close();
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
        InstallerUtils.installPackage(mock(Uri.class), mContext, null);

        /* Verify. */
        verify(mInputStream, never()).close();
        verify(mSession).abandon();
    }

    @Test
    public void throwIOExceptionWhenTryToCreateSession() throws IOException {

        /* Throw error when try to create session. */
        when(mMockPackageInstaller.createSession(any(PackageInstaller.SessionParams.class))).thenThrow(new IOException());

        /* Call install method. */
        InstallerUtils.installPackage(mock(Uri.class), mContext, null);

        /* Verify that the session wasn't created. */
        verify(mMockPackageInstaller, never()).openSession(anyInt());
    }

    @Test
    public void isSystemAlertWindowsEnabledReturnsTrueIfBuildVersionLowerQ() throws Exception {

        /* Mock SDK_INT to LOLLIPOP. */
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), Build.VERSION_CODES.LOLLIPOP);

        /* Set canDrawOverlays to false. */
        mockStatic(Settings.class);
        when(Settings.canDrawOverlays(any(Context.class))).thenReturn(false);

        /* Check if system alert windows is enabled. */
        assertTrue(InstallerUtils.isSystemAlertWindowsEnabled(mContext));

        /* Set canDrawOverlays to true. */
        when(Settings.canDrawOverlays(any(Context.class))).thenReturn(true);

        /* Verify canDrawOverlays == true doesn't affect the return value of isSystemAlertWindowsEnabled and it still returns true. */
        assertTrue(InstallerUtils.isSystemAlertWindowsEnabled(mContext));

        /* Also check on P. */
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), Build.VERSION_CODES.P);

        /* Verify that system alert windows is enabled. */
        assertTrue(InstallerUtils.isSystemAlertWindowsEnabled(mContext));
    }

    @Test
    public void isSystemAlertWindowsEnabledReturnsFalseIfBuildVersionQorHigherAndCanDrawOverlay() throws Exception {

        /* Mock SDK_INT to Q. */
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), Build.VERSION_CODES.Q);

        /* Set canDrawOverlays to true. */
        mockStatic(Settings.class);
        when(Settings.canDrawOverlays(any(Context.class))).thenReturn(true);

        /* Verify that system alert windows is enabled. */
        assertTrue(InstallerUtils.isSystemAlertWindowsEnabled(mContext));
    }

    @Test
    public void isSystemAlertWindowsEnabledReturnsFalseIfBuildVersionQorHigherAndCannotDrawOverlay() throws Exception {

        /* Mock SDK_INT to Q. */
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), Build.VERSION_CODES.Q);

        /* Set canDrawOverlays to false. */
        mockStatic(Settings.class);
        when(Settings.canDrawOverlays(any(Context.class))).thenReturn(false);

        /* Verify that system alert windows is not enabled. */
        assertFalse(InstallerUtils.isSystemAlertWindowsEnabled(mContext));
    }

    @Test
    public void createIntentSenderOnAndroidS() throws Exception {

        /* Mock SDK_INT to S. */
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), Build.VERSION_CODES.S);
        createIntentSender(FLAG_MUTABLE);
    }

    @Test
    public void createIntentSenderOnAndroidLowS() throws Exception {

        /* Mock SDK_INT to M. */
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), Build.VERSION_CODES.M);
        createIntentSender(0);
    }

    private void createIntentSender(final int expectedFlag) {
        mockStatic(PendingIntent.class);
        final PendingIntent mockIntent = mock(PendingIntent.class);
        when(mockIntent.getIntentSender()).thenReturn(mock(IntentSender.class));
        when(PendingIntent.getBroadcast(any(Context.class), anyInt(), any(Intent.class), anyInt())).then(new Answer<PendingIntent>() {
            @Override
            public PendingIntent answer(InvocationOnMock invocation) {
                int flag = (int)invocation.getArguments()[3];
                Assert.assertEquals(flag, expectedFlag);
                return mockIntent;
            }
        });
        InstallerUtils.createIntentSender(mContext, 1);
    }

    /**
     * This is used ot set a specific Build.VERSION.SDK_INT for tests.
     * @param field static field
     * @param newValue new value for the given field
     */
    static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, newValue);
    }
}
