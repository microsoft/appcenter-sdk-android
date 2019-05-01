/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.push;

import android.content.Context;
import android.content.Intent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest(Push.class)
public class PushReceiverTest {

    @Mock
    private Push mPush;

    @Before
    public void setUp() {
        mockStatic(Push.class);
        when(Push.getInstance()).thenReturn(mPush);
    }

    @Test
    public void onTokenRefresh() {
        Intent intent = mock(Intent.class);
        when(intent.getAction()).thenReturn(PushReceiver.INTENT_ACTION_REGISTRATION);
        when(intent.getStringExtra(PushReceiver.INTENT_EXTRA_REGISTRATION)).thenReturn("mockToken");
        new PushReceiver().onReceive(mock(Context.class), intent);
        verify(mPush).onTokenRefresh("mockToken");
        verifyNoMoreInteractions(mPush);
    }

    @Test
    public void onMessageReceived() {
        Context context = mock(Context.class);
        Intent intent = mock(Intent.class);
        when(intent.getAction()).thenReturn(PushReceiver.INTENT_ACTION_RECEIVE);
        new PushReceiver().onReceive(context, intent);
        verify(mPush).onMessageReceived(context, intent);
        verifyNoMoreInteractions(mPush);
    }

    @Test
    public void onInvalidIntent() {
        new PushReceiver().onReceive(mock(Context.class), mock(Intent.class));
        verifyZeroInteractions(mPush);
    }
}