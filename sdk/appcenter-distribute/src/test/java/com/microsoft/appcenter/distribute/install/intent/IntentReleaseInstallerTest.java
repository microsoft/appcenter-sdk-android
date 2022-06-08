/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.install.intent;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_FIRST_USER;
import static android.app.Activity.RESULT_OK;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.microsoft.appcenter.distribute.install.ReleaseInstaller;
import com.microsoft.appcenter.distribute.install.ReleaseInstallerActivity;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;
import com.microsoft.appcenter.utils.async.AppCenterFuture;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

@PrepareForTest({
        Intent.class,
        IntentReleaseInstaller.class,
        ReleaseInstallerActivity.class
})
public class IntentReleaseInstallerTest {

    @Rule
    public PowerMockRule mRule = new PowerMockRule();

    @Mock
    Context mContext;

    @Mock
    ReleaseInstaller.Listener mListener;

    @Mock
    Intent mIntent;

    @Mock
    AppCenterFuture<ReleaseInstallerActivity.Result> mResultFuture;

    @Captor
    ArgumentCaptor<AppCenterConsumer<ReleaseInstallerActivity.Result>> mConsumerArgumentCaptor;

    IntentReleaseInstaller mInstaller;

    @Before
    public void setUp() throws Exception {

        /* Mock intent. */
        mockStatic(Intent.class);
        whenNew(Intent.class).withAnyArguments().thenReturn(mIntent);
        when(mIntent.resolveActivity(any())).thenReturn(mock(ComponentName.class));

        /* Mock activity. */
        mockStatic(ReleaseInstallerActivity.class);
        when(ReleaseInstallerActivity.startActivityForResult(eq(mContext), any(Intent.class)))
                .thenReturn(mResultFuture);

        /* Create installer instance. */
        mInstaller = new IntentReleaseInstaller(mContext, null, mListener);
    }

    @Test
    public void cannotResolveActivity() {
        when(mIntent.resolveActivity(any())).thenReturn(null);
        Uri uri = mock(Uri.class);
        mInstaller.install(uri);

        /* Verify error if activity cannot be resolved. */
        verify(mIntent).setData(eq(uri));
        verify(mListener).onError(anyString());
        verifyNoMoreInteractions(mListener);
    }

    @Test
    public void cannotStartActivity() {
        when(ReleaseInstallerActivity.startActivityForResult(eq(mContext), any(Intent.class)))
                .thenReturn(null);
        Uri uri = mock(Uri.class);
        mInstaller.install(uri);

        /* Verify that we tried to start activity. */
        verify(mIntent).setData(eq(uri));
        verifyStatic(ReleaseInstallerActivity.class);
        ReleaseInstallerActivity.startActivityForResult(eq(mContext), eq(mIntent));

        /*
         * It should not be a case, it's expected to have no more than one installer in time.
         * So, don't inform listener to avoid break state of the service.
         */
        verifyNoInteractions(mListener);
    }

    @Test
    public void installSuccess() {
        Uri uri = mock(Uri.class);
        mInstaller.install(uri);

        /* Verify that activity has been started. */
        verify(mIntent).setData(eq(uri));
        verify(mResultFuture).thenAccept(mConsumerArgumentCaptor.capture());

        /* Pass result to captured callback. */
        ReleaseInstallerActivity.Result result = new ReleaseInstallerActivity.Result(RESULT_OK, null);
        mConsumerArgumentCaptor.getValue().accept(result);

        /* In case of success the application will be killed, there is no callback for that case. */
        verifyNoInteractions(mListener);
    }

    @Test
    public void installError() {
        Uri uri = mock(Uri.class);
        mInstaller.install(uri);

        /* Verify that activity has been started. */
        verify(mIntent).setData(eq(uri));
        verify(mResultFuture).thenAccept(mConsumerArgumentCaptor.capture());

        /* Pass result to captured callback. */
        ReleaseInstallerActivity.Result result = new ReleaseInstallerActivity.Result(RESULT_FIRST_USER, null);
        mConsumerArgumentCaptor.getValue().accept(result);

        /* Verify error callback. */
        verify(mListener).onError(anyString());
        verifyNoMoreInteractions(mListener);
    }

    @Test
    public void installCancel() {
        Uri uri = mock(Uri.class);
        mInstaller.install(uri);

        /* Verify that activity has been started. */
        verify(mIntent).setData(eq(uri));
        verify(mResultFuture).thenAccept(mConsumerArgumentCaptor.capture());

        /* Pass result to captured callback. */
        ReleaseInstallerActivity.Result result = new ReleaseInstallerActivity.Result(RESULT_CANCELED, null);
        mConsumerArgumentCaptor.getValue().accept(result);

        /* Verify error cancel. */
        verify(mListener).onCancel();
        verifyNoMoreInteractions(mListener);
    }

    @Test
    public void testToString() {
        assertEquals("ACTION_INSTALL_PACKAGE", mInstaller.toString());
    }
}