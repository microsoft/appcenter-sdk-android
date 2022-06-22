/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.install.session;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PackageInstallerListenerTest {

    private static final int SESSION_ID = 42;

    @Mock
    private SessionReleaseInstaller mInstaller;

    private PackageInstallerListener mListener;

    @Before
    public void setUp() {
        mListener = new PackageInstallerListener(mInstaller);
    }

    @Test
    public void onCreated() {
        mListener.onCreated(SESSION_ID);
        verifyNoInteractions(mInstaller);
    }

    @Test
    public void onBadgingChanged() {
        mListener.onBadgingChanged(SESSION_ID);
        verifyNoInteractions(mInstaller);
    }

    @Test
    public void onActiveChanged() {
        mListener.onActiveChanged(SESSION_ID, true);
        verifyNoInteractions(mInstaller);
    }

    @Test
    public void onProgressChanged() {
        mListener.onProgressChanged(SESSION_ID, 0.5f);
        verify(mInstaller).onInstallProgress(eq(SESSION_ID));
    }

    @Test
    public void onFinished() {
        mListener.onFinished(SESSION_ID, true);
        verifyNoInteractions(mInstaller);
    }
}