package com.microsoft.appcenter.utils;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

import com.microsoft.appcenter.test.TestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PrepareForTest;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@PrepareForTest(NetworkStateHelper.class)
public class NetworkStateHelperTestFromLollipop extends AbstractNetworkStateHelperTest {

    @Before
    public void setUp() throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.LOLLIPOP);
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", 0);
    }

    @Test
    public void initialState() {
        assertFalse(new NetworkStateHelper(mContext).isNetworkConnected());
    }

    @Test
    public void permissionDenied() {
        doThrow(new SecurityException())
                .when(mConnectivityManager)
                .registerNetworkCallback(any(NetworkRequest.class), any(ConnectivityManager.NetworkCallback.class));
        assertFalse(new NetworkStateHelper(mContext).isNetworkConnected());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void listenNetwork() {
        NetworkStateHelper helper = new NetworkStateHelper(mContext);
        ArgumentCaptor<ConnectivityManager.NetworkCallback> callback = ArgumentCaptor.forClass(ConnectivityManager.NetworkCallback.class);
        verify(mConnectivityManager).registerNetworkCallback(any(NetworkRequest.class), callback.capture());
        NetworkStateHelper.Listener listener = mock(NetworkStateHelper.Listener.class);
        helper.addListener(listener);

        /* Initial state is down, if state does not change, no callback. */
        verify(listener, never()).onNetworkStateUpdated(anyBoolean());

        /* Change state to up, say WIFI. */
        Network network = mock(Network.class);
        callback.getValue().onAvailable(network);
        verify(listener).onNetworkStateUpdated(true);
        verify(listener, never()).onNetworkStateUpdated(false);

        /* Change to say, Mobile. */
        helper.removeListener(listener);
        NetworkStateHelper.Listener listener2 = mock(NetworkStateHelper.Listener.class);
        helper.addListener(listener2);
        callback.getValue().onLost(network);
        network = mock(Network.class);
        callback.getValue().onAvailable(network);
        verify(listener2).onNetworkStateUpdated(false);
        verify(listener2).onNetworkStateUpdated(true);

        /* Make new network available before we lost the previous one. */
        helper.removeListener(listener2);
        NetworkStateHelper.Listener listener3 = mock(NetworkStateHelper.Listener.class);
        helper.addListener(listener3);
        Network network2 = mock(Network.class);
        callback.getValue().onAvailable(network2);

        /* The callbacks are triggered only when losing previous network. */
        verify(listener3, never()).onNetworkStateUpdated(anyBoolean());
        callback.getValue().onLost(network);
        verify(listener3).onNetworkStateUpdated(false);
        verify(listener3).onNetworkStateUpdated(true);

        /* Close and verify interactions. */
        helper.close();
        verify(mConnectivityManager).unregisterNetworkCallback(callback.getValue());
        verifyNoMoreInteractions(listener);
        verifyNoMoreInteractions(listener2);
        verifyNoMoreInteractions(listener3);

        /* Verify we didn't try to use older APIs after Lollipop on newer devices. */
        verify(mContext, never()).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class));
        verify(mContext, never()).unregisterReceiver(any(BroadcastReceiver.class));
        verify(mConnectivityManager, never()).getActiveNetworkInfo();
    }

    @Test
    public void verifyRequestedCapabilitiesBeforeAndroidM() throws Exception {
        NetworkRequest.Builder builder = mock(NetworkRequest.Builder.class);
        whenNew(NetworkRequest.Builder.class).withAnyArguments().thenReturn(builder);
        new NetworkStateHelper(mContext);
        verify(builder).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        verify(builder, never()).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    @Test
    public void verifyRequestedCapabilitiesFromAndroidM() throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.M);
        NetworkRequest.Builder builder = mock(NetworkRequest.Builder.class);
        whenNew(NetworkRequest.Builder.class).withAnyArguments().thenReturn(builder);
        new NetworkStateHelper(mContext);
        verify(builder).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        verify(builder).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
