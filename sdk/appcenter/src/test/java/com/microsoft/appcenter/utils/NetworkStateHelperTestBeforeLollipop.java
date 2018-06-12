package com.microsoft.appcenter.utils;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkRequest;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class NetworkStateHelperTestBeforeLollipop extends AbstractNetworkStateHelperTest {

    @Test
    public void nullNetworkInfo() {
        NetworkStateHelper helper = new NetworkStateHelper(mContext);
        assertFalse(helper.isNetworkConnected());
    }

    @Test
    public void networkStates() {
        for (NetworkInfo.State state : NetworkInfo.State.values()) {
            NetworkInfo networkInfo = mock(NetworkInfo.class);
            when(mConnectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
            when(networkInfo.isConnected()).thenReturn(state == NetworkInfo.State.CONNECTED);
            NetworkStateHelper helper = new NetworkStateHelper(mContext);
            assertEquals(state == NetworkInfo.State.CONNECTED, helper.isNetworkConnected());
        }
    }

    @Test
    public void permissionDenied() {
        when(mConnectivityManager.getActiveNetworkInfo()).thenThrow(new SecurityException());
        NetworkStateHelper helper = new NetworkStateHelper(mContext);
        assertFalse(helper.isNetworkConnected());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void listenNetwork() {
        final AtomicReference<BroadcastReceiver> receiverRef = new AtomicReference<>();
        when(mContext.registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class))).then(new Answer<Intent>() {

            @Override
            public Intent answer(InvocationOnMock invocation) {
                BroadcastReceiver receiver = (BroadcastReceiver) invocation.getArguments()[0];
                IntentFilter filter = (IntentFilter) invocation.getArguments()[1];
                assertNotNull(receiver);
                assertNotNull(filter);
                receiverRef.set(receiver);
                return mock(Intent.class);
            }
        });
        NetworkStateHelper helper = new NetworkStateHelper(mContext);
        verify(mContext).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class));
        NetworkStateHelper.Listener listener = mock(NetworkStateHelper.Listener.class);
        helper.addListener(listener);

        /* Initial state is down, if state does not change, no callback. */
        final Intent intent = mock(Intent.class);
        BroadcastReceiver receiver = receiverRef.get();
        receiver.onReceive(mContext, intent);
        verify(listener, never()).onNetworkStateUpdated(anyBoolean());

        /* Change state to up. */
        NetworkInfo networkInfo = mock(NetworkInfo.class);
        when(networkInfo.isConnected()).thenReturn(true);
        when(networkInfo.getTypeName()).thenReturn("MOBILE");
        when(networkInfo.getSubtypeName()).thenReturn("EDGE");
        when(mConnectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        when(intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO)).thenReturn(networkInfo);
        receiver.onReceive(mContext, intent);
        verify(listener).onNetworkStateUpdated(true);
        verify(listener, never()).onNetworkStateUpdated(false);
        assertTrue(helper.isNetworkConnected());

        /* Change to WIFI. */
        helper.removeListener(listener);
        NetworkStateHelper.Listener listener2 = mock(NetworkStateHelper.Listener.class);
        helper.addListener(listener2);
        when(networkInfo.getTypeName()).thenReturn("WIFI");
        when(networkInfo.getSubtypeName()).thenReturn(null);
        receiver.onReceive(mContext, intent);
        verify(listener2).onNetworkStateUpdated(false);
        verify(listener2).onNetworkStateUpdated(true);
        assertTrue(helper.isNetworkConnected());

        /* Duplicate WIFI callback. */
        receiver.onReceive(mContext, intent);
        verifyNoMoreInteractions(listener2);

        /* But then WIFI is disconnected. */
        helper.removeListener(listener2);
        NetworkStateHelper.Listener listener3 = mock(NetworkStateHelper.Listener.class);
        helper.addListener(listener3);
        when(networkInfo.isConnected()).thenReturn(false);
        receiver.onReceive(mContext, intent);
        verify(listener3).onNetworkStateUpdated(false);
        assertFalse(helper.isNetworkConnected());

        /* Make it connected again before closing with no listener. */
        helper.removeListener(listener3);
        when(networkInfo.isConnected()).thenReturn(true);
        receiver.onReceive(mContext, intent);
        verify(listener3, never()).onNetworkStateUpdated(true);
        assertTrue(helper.isNetworkConnected());

        /* Close and verify interactions. This will also reset to disconnected. */
        helper.close();
        assertFalse(helper.isNetworkConnected());
        verify(mContext).unregisterReceiver(receiver);

        /* Reopening will not restore removed listeners by close. */
        helper.reopen();
        assertTrue(helper.isNetworkConnected());
        verify(mContext, times(2)).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class));

        /* Check no extra listener calls. */
        verifyNoMoreInteractions(listener);
        verifyNoMoreInteractions(listener2);
        verifyNoMoreInteractions(listener3);

        /* Verify we didn't try to use newer APIs before Lollipop on old devices. */
        verify(mConnectivityManager, never()).unregisterNetworkCallback(any(ConnectivityManager.NetworkCallback.class));
        verify(mConnectivityManager, never()).registerNetworkCallback(any(NetworkRequest.class), any(ConnectivityManager.NetworkCallback.class));
    }
}
