/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.content.Context;
import android.net.ConnectivityManager;

import org.junit.Before;
import org.junit.Rule;
import org.mockito.Mock;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static org.mockito.Mockito.when;

public class AbstractNetworkStateHelperTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    Context mContext;

    @Mock
    ConnectivityManager mConnectivityManager;

    @Before
    public void setUpAbstract() {
        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(mConnectivityManager);
    }
}
