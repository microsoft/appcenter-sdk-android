/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

import android.content.Context;
import android.net.Uri;

import com.microsoft.appcenter.distribute.install.ReleaseInstaller;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.util.LinkedList;

@RunWith(MockitoJUnitRunner.class)
public class UpdateInstallerTest {

    @Mock
    private ReleaseDetails mReleaseDetails;

    @Mock
    private ReleaseInstaller mReleaseInstaller;

    private UpdateInstaller mInstaller;

    @Before
    public void setUp() throws Exception {
        mInstaller = new UpdateInstaller(mReleaseDetails, new LinkedList<ReleaseInstaller>() {{
            add(mReleaseInstaller);
        }});
    }

    @Test
    public void install() {
        Uri uri = mock(Uri.class);
        mInstaller.install(uri);

        verify(mReleaseInstaller).install(eq(uri));
    }

    @Test
    public void resume() {
        mInstaller.resume();
    }

    @Test
    public void clear() {
        mInstaller.clear();
    }

    @Test
    public void onError() {
        mInstaller.onError("Some error");
    }

    @Test
    public void onCancel() {
        mInstaller.onCancel();
    }
}