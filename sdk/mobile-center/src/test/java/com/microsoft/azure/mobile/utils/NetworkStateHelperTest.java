package com.microsoft.azure.mobile.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
public class NetworkStateHelperTest {

    @Test
    public void nullNetworkInfo() {
        Context context = mock(Context.class);
        ConnectivityManager connectivityManager = mock(ConnectivityManager.class);
        when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);
        NetworkStateHelper helper = new NetworkStateHelper(context);
        assertFalse(helper.isNetworkConnected());
    }

    @Test
    public void networkStates() {
        for (NetworkInfo.State state : NetworkInfo.State.values()) {
            Context context = mock(Context.class);
            ConnectivityManager connectivityManager = mock(ConnectivityManager.class);
            NetworkInfo networkInfo = mock(NetworkInfo.class);
            when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);
            when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
            when(networkInfo.getState()).thenReturn(state);
            NetworkStateHelper helper = new NetworkStateHelper(context);
            assertEquals(state == NetworkInfo.State.CONNECTED, helper.isNetworkConnected());
        }
    }

    @Test
    public void permissionDenied() {
        Context context = mock(Context.class);
        ConnectivityManager connectivityManager = mock(ConnectivityManager.class);
        when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);
        when(connectivityManager.getActiveNetworkInfo()).thenThrow(new SecurityException());
        NetworkStateHelper helper = new NetworkStateHelper(context);
        assertFalse(helper.isNetworkConnected());
    }

    @Test
    public void listenNetwork() {
        Context context = mock(Context.class);
        ConnectivityManager connectivityManager = mock(ConnectivityManager.class);
        final AtomicReference<BroadcastReceiver> receiverRef = new AtomicReference<>();
        when(context.getApplicationContext()).thenReturn(context);
        when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);
        when(context.registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class))).then(new Answer<Intent>() {

            @Override
            public Intent answer(InvocationOnMock invocation) throws Throwable {
                BroadcastReceiver receiver = (BroadcastReceiver) invocation.getArguments()[0];
                IntentFilter filter = (IntentFilter) invocation.getArguments()[1];
                assertNotNull(receiver);
                assertNotNull(filter);
                receiverRef.set(receiver);
                return mock(Intent.class);
            }
        });
        NetworkStateHelper helper = new NetworkStateHelper(context);
        NetworkStateHelper.Listener listener = mock(NetworkStateHelper.Listener.class);
        helper.addListener(listener);

        /* Initial state is down, if state does not change, no callback. */
        final Intent intent = mock(Intent.class);
        BroadcastReceiver receiver = receiverRef.get();
        receiver.onReceive(context, intent);
        verify(listener, never()).onNetworkStateUpdated(anyBoolean());

        /* Change state to up. */
        NetworkInfo networkInfo = mock(NetworkInfo.class);
        when(networkInfo.getState()).thenReturn(NetworkInfo.State.CONNECTED);
        when(networkInfo.getTypeName()).thenReturn("MOBILE");
        when(networkInfo.getSubtypeName()).thenReturn("EDGE");
        when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);

        //noinspection deprecation
        when(intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO)).thenReturn(networkInfo);
        receiver.onReceive(context, intent);
        verify(listener).onNetworkStateUpdated(true);
        verify(listener, never()).onNetworkStateUpdated(false);

        /* Change to WIFI. */
        helper.removeListener(listener);
        NetworkStateHelper.Listener listener2 = mock(NetworkStateHelper.Listener.class);
        helper.addListener(listener2);
        when(networkInfo.getTypeName()).thenReturn("WIFI");
        when(networkInfo.getSubtypeName()).thenReturn(null);
        receiver.onReceive(context, intent);
        verify(listener2).onNetworkStateUpdated(false);
        verify(listener2).onNetworkStateUpdated(true);

        /* Duplicate WIFI callback. */
        receiver.onReceive(context, intent);
        verifyNoMoreInteractions(listener2);

        /* But then WIFI is disconnected. */
        helper.removeListener(listener2);
        NetworkStateHelper.Listener listener3 = mock(NetworkStateHelper.Listener.class);
        helper.addListener(listener3);
        when(networkInfo.getState()).thenReturn(NetworkInfo.State.DISCONNECTED);
        receiver.onReceive(context, intent);
        verify(listener3).onNetworkStateUpdated(false);

        /* Close and verify interactions. */
        helper.close();
        verify(context).unregisterReceiver(receiver);
        verifyNoMoreInteractions(listener);
        verifyNoMoreInteractions(listener2);
        verifyNoMoreInteractions(listener3);
    }
}
