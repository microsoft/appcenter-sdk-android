/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import static com.microsoft.appcenter.distribute.UpdateInstaller.RECOVERABLE_ERROR;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

import com.microsoft.appcenter.distribute.install.ReleaseInstaller;
import com.microsoft.appcenter.distribute.install.intent.IntentReleaseInstaller;
import com.microsoft.appcenter.distribute.install.session.SessionReleaseInstaller;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.util.LinkedList;

@PrepareForTest({
        Distribute.class,
        Handler.class,
        HandlerThread.class,
        IntentReleaseInstaller.class,
        SessionReleaseInstaller.class,
        UpdateInstaller.class
})
public class UpdateInstallerTest {

    @Rule
    public PowerMockRule mRule = new PowerMockRule();

    @Mock
    private Distribute mDistribute;

    @Mock
    private ReleaseDetails mReleaseDetails;

    @Mock
    private ReleaseInstaller mReleaseInstaller;

    private UpdateInstaller mInstaller;

    @Before
    public void setUp() throws Exception {
        mockStatic(Distribute.class);
        when(Distribute.getInstance()).thenReturn(mDistribute);
        mInstaller = new UpdateInstaller(mReleaseDetails, new LinkedList<ReleaseInstaller>() {{
            add(mReleaseInstaller);
        }});
    }

    @Test
    public void installerPassThrough() {

        /* Start installation. */
        Uri uri = mock(Uri.class);
        mInstaller.install(uri);
        verify(mReleaseInstaller).install(eq(uri));

        /* Clear installer. */
        mInstaller.clear();
        verify(mReleaseInstaller).clear();
    }

    @Test
    public void resume() {

        /* No-op if not cancelled. */
        mInstaller.resume();
        verifyNoInteractions(mDistribute);

        /* Start installation. */
        Uri uri = mock(Uri.class);
        mInstaller.install(uri);
        verify(mReleaseInstaller).install(eq(uri));

        /* Cancel installation. */
        mInstaller.onCancel();
        verify(mDistribute).completeWorkflow(eq(mReleaseDetails));

        /* No-op if not mandatory. */
        mInstaller.resume();
        verifyNoMoreInteractions(mDistribute);
    }

    @Test
    public void resumeMandatoryUpdate() {
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(true);

        /* No-op if not cancelled. */
        mInstaller.resume();
        verifyNoInteractions(mDistribute);

        /* Start installation. */
        Uri uri = mock(Uri.class);
        mInstaller.install(uri);
        verify(mReleaseInstaller).install(eq(uri));

        /* Cancel installation. */
        mInstaller.onCancel();
        verify(mDistribute).showMandatoryDownloadReadyDialog(eq(mReleaseDetails));

        /* Show dialog if mandatory update was cancelled in background. */
        mInstaller.resume();
        verify(mDistribute, times(2)).showMandatoryDownloadReadyDialog(eq(mReleaseDetails));

        /* No-op after restarting installation. */
        mInstaller.install(uri);
        mInstaller.resume();
        verifyNoMoreInteractions(mDistribute);
    }

    @Test
    public void nonRecoverableError() {
        Uri uri = mock(Uri.class);
        mInstaller.install(uri);
        verify(mReleaseInstaller).install(eq(uri));

        /* Complete workflow on non-recoverable error. */
        mInstaller.onError("Some error");
        verify(mDistribute).completeWorkflow(eq(mReleaseDetails));
        verifyNoMoreInteractions(mReleaseInstaller);
    }

    @Test
    public void fallbackOnRecoverableError() throws Exception {

        /* Mock Handler. */
        mockStatic(Handler.class);
        Handler handler = mock(Handler.class);
        whenNew(Handler.class).withAnyArguments().thenReturn(handler);

        /* Mock HandlerThread. */
        mockStatic(HandlerThread.class);
        HandlerThread handlerThread = mock(HandlerThread.class);
        whenNew(HandlerThread.class).withAnyArguments().thenReturn(handlerThread);

        /* Mock SessionReleaseInstaller. */
        mockStatic(SessionReleaseInstaller.class);
        SessionReleaseInstaller sessionInstaller = mock(SessionReleaseInstaller.class);
        whenNew(SessionReleaseInstaller.class).withAnyArguments().thenReturn(sessionInstaller);

        /* Mock IntentReleaseInstaller. */
        mockStatic(IntentReleaseInstaller.class);
        IntentReleaseInstaller intentInstaller = mock(IntentReleaseInstaller.class);
        whenNew(IntentReleaseInstaller.class).withAnyArguments().thenReturn(intentInstaller);

        /* Create installer instance. */
        mInstaller = new UpdateInstaller(mock(Context.class), mReleaseDetails);

        /* Start installation. */
        Uri uri = mock(Uri.class);
        mInstaller.install(uri);
        verify(sessionInstaller).install(eq(uri));

        /* Fallback on recoverable error. */
        mInstaller.onError(RECOVERABLE_ERROR);
        verify(mDistribute, never()).completeWorkflow(eq(mReleaseDetails));
        verify(sessionInstaller).clear();
        verify(intentInstaller).install(eq(uri));

        /* Complete workflow without fallback. */
        mInstaller.onError(RECOVERABLE_ERROR);
        verify(mDistribute).completeWorkflow(eq(mReleaseDetails));
        verify(intentInstaller).clear();

        /* No-op on future calls without fallback installer. */
        mInstaller.install(uri);
        mInstaller.clear();
        verifyNoMoreInteractions(sessionInstaller);
        verifyNoMoreInteractions(intentInstaller);
    }
}