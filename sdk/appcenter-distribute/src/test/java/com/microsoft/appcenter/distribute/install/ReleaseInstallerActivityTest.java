/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.install;

import static android.app.Activity.RESULT_FIRST_USER;
import static android.app.Activity.RESULT_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class ReleaseInstallerActivityTest {

    @Mock
    private Intent mIntent;

    @Spy
    private ReleaseInstallerActivity mActivity = new ReleaseInstallerActivity();

    @Before
    public void setUp() {
        when(mActivity.getIntent()).thenReturn(mIntent);
    }

    @After
    public void tearDown() {
        ReleaseInstallerActivity.sResultFuture = null;
    }

    @Test
    public void startActivityForResult() {
        Context context = mock(Context.class);
        AppCenterFuture<ReleaseInstallerActivity.Result> confirmFuture =
                ReleaseInstallerActivity.startActivityForResult(context, mIntent);
        assertNotNull(confirmFuture);
        assertFalse(confirmFuture.isDone());
        assertEquals(confirmFuture, ReleaseInstallerActivity.sResultFuture);

        /* Verify that we start our activity. */
        verify(context).startActivity(any());

        /* Second call does nothing. */
        assertNull(ReleaseInstallerActivity.startActivityForResult(context, mIntent));
        assertEquals(confirmFuture, ReleaseInstallerActivity.sResultFuture);
        verifyNoMoreInteractions(context);
    }

    @Test
    public void onCreate() {
        Intent extraIntent = mock(Intent.class);
        when(mIntent.getParcelableExtra(Intent.EXTRA_INTENT)).thenReturn(extraIntent);

        /* Simulate onCreate lifecycle event. */
        mActivity.onCreate(mock(Bundle.class));

        /* Verify that extra intent has been started. */
        verify(mActivity).startActivityForResult(eq(extraIntent), anyInt());
        verify(mActivity, never()).finish();
    }

    @Test
    public void onCreateWithoutIntent() {

        /* Simulate onCreate lifecycle event. */
        mActivity.onCreate(mock(Bundle.class));

        /* Immediately finish incorrectly initialized activity. */
        verify(mActivity).finish();
    }

    @Test
    public void onCreateThrowsSecurityException() {
        Intent extraIntent = mock(Intent.class);
        when(mIntent.getParcelableExtra(Intent.EXTRA_INTENT)).thenReturn(extraIntent);
        doThrow(new SecurityException()).when(mActivity).startActivityForResult(eq(extraIntent), anyInt());

        /* Initialize result future. */
        AppCenterFuture<ReleaseInstallerActivity.Result> confirmFuture =
                ReleaseInstallerActivity.startActivityForResult(mock(Context.class), mIntent);
        assertNotNull(confirmFuture);

        /* Simulate onCreate lifecycle event. */
        mActivity.onCreate(mock(Bundle.class));

        /* Verify that future is completed and activity finished. */
        assertTrue(confirmFuture.isDone());
        assertEquals(RESULT_FIRST_USER, confirmFuture.get().code);
        verify(mActivity).finish();
    }

    @Test
    public void onActivityResult() {

        /* Simulate onActivityResult event without initialized future. */
        mActivity.onActivityResult(0, RESULT_OK, null);
        verify(mActivity).finish();

        /* Initialize result future. */
        AppCenterFuture<ReleaseInstallerActivity.Result> confirmFuture =
                ReleaseInstallerActivity.startActivityForResult(mock(Context.class), mIntent);
        assertNotNull(confirmFuture);

        /* Simulate onActivityResult event. */
        mActivity.onActivityResult(0, RESULT_OK, null);

        /* Verify that future is completed and activity finished. */
        assertTrue(confirmFuture.isDone());
        assertEquals(RESULT_OK, confirmFuture.get().code);
        verify(mActivity, times(2)).finish();
    }
}