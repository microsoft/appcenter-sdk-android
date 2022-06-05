/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.install;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

    @Test
    public void startActivityForResult() {
        Context context = mock(Context.class);
        ReleaseInstallerActivity.startActivityForResult(context, mIntent);
    }

    @Test
    public void onCreate() {
        mActivity.onCreate(mock(Bundle.class));
    }

    @Test
    public void onActivityResult() {
        mActivity.onActivityResult(0, 0, null);
    }
}