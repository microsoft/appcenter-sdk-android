package com.microsoft.appcenter.utils;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.VisibleForTesting;

import java.io.Closeable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static com.microsoft.appcenter.AppCenter.LOG_TAG;

/**
 * Network state helper.
 */
public class NetworkStateHelper implements Closeable {

    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static NetworkStateHelper sSharedInstance;

    /**
     * Android context.
     */
    private final Context mContext;

    /**
     * Android connectivity manager.
     */
    private final ConnectivityManager mConnectivityManager;

    /**
     * Network state listeners that will subscribe to us.
     */
    private final Set<Listener> mListeners = new HashSet<>();

    /**
     * Network callback, null on API level < 21.
     */
    private ConnectivityManager.NetworkCallback mNetworkCallback;

    /**
     * Our connectivity event receiver, null on API level >= 21.
     */
    private ConnectivityReceiver mConnectivityReceiver;

    /**
     * Current network state.
     */
    private boolean mConnected;

    /**
     * Init.
     *
     * @param context any Android context.
     */
    @VisibleForTesting
    NetworkStateHelper(Context context) {
        mContext = context.getApplicationContext();
        mConnectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        reopen();
    }

    /**
     * Get shared instance.
     *
     * @param context any context.
     * @return shared instance.
     */
    public static synchronized NetworkStateHelper getSharedInstance(Context context) {
        if (sSharedInstance == null) {
            sSharedInstance = new NetworkStateHelper(context);
        }
        return sSharedInstance;
    }

    /**
     * Make this helper active again after closing.
     */
    public synchronized void reopen() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                /*
                 * Build query to get a working network listener.
                 *
                 * NetworkCapabilities.NET_CAPABILITY_VALIDATED (that indicates that connectivity
                 * on this network was successfully validated) shouldn't be applied here because it
                 * might miss networks with partial internet availability.
                 */
                NetworkRequest.Builder request = new NetworkRequest.Builder();
                request.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                mNetworkCallback = new ConnectivityManager.NetworkCallback() {

                    @Override
                    public void onAvailable(Network network) {
                        onNetworkAvailable(network);
                    }

                    @Override
                    public void onLost(Network network) {
                        onNetworkLost(network);
                    }
                };

                //noinspection ConstantConditions
                mConnectivityManager.registerNetworkCallback(request.build(), mNetworkCallback);
            } else {
                mConnectivityReceiver = new ConnectivityReceiver();
                mContext.registerReceiver(mConnectivityReceiver, getOldIntentFilter());
                handleNetworkStateUpdate();
            }
        } catch (RuntimeException e) {

            /*
             * Can be security exception if permission missing or sometimes another runtime exception
             * on some customized firmwares.
             */
            AppCenterLog.error(LOG_TAG, "Cannot access network state information.", e);

            /* We should try to send the data, even if we can't get the current network state. */
            mConnected = true;
        }
    }

    @NonNull
    @SuppressWarnings("deprecation")
    private IntentFilter getOldIntentFilter() {
        return new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    }

    /**
     * Check whether the network is currently connected.
     *
     * @return true for connected, false for disconnected.
     */
    public synchronized boolean isNetworkConnected() {
        return mConnected || isAnyNetworkConnected();
    }

    /**
     * Check if any network is connected.
     *
     * @return true for connected, false for disconnected.
     */
    private boolean isAnyNetworkConnected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Network[] networks = mConnectivityManager.getAllNetworks();
            if (networks == null) {
                return false;
            }
            for (Network network : networks) {
                NetworkInfo info = mConnectivityManager.getNetworkInfo(network);
                if (info != null && info.isConnected()) {
                    return true;
                }
            }
        } else {
            NetworkInfo[] networks = mConnectivityManager.getAllNetworkInfo();
            if (networks == null) {
                return false;
            }
            for (NetworkInfo info : networks) {
                if (info != null && info.isConnected()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Handle network available update on API level >= 21.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private synchronized void onNetworkAvailable(@SuppressWarnings("unused") Network network) {
        if (!mConnected) {
            notifyNetworkStateUpdated(true);
            mConnected = true;
        }
    }

    /**
     * Handle network available update on API level >= 21.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private synchronized void onNetworkLost(Network network) {
        Network[] networks = mConnectivityManager.getAllNetworks();
        boolean noNetworks = networks == null || networks.length == 0 ||
                Arrays.equals(networks, new Network[] { network });
        if (mConnected && noNetworks) {
            notifyNetworkStateUpdated(false);
            mConnected = false;
        }
    }

    /**
     * Handle network state update on API level < 21.
     */
    private synchronized void handleNetworkStateUpdate() {
        boolean connected = isAnyNetworkConnected();
        if (connected != mConnected) {
            notifyNetworkStateUpdated(connected);
            mConnected = connected;
        }
    }

    /**
     * Notify listeners that the network state changed.
     *
     * @param connected whether the network is connected or not.
     */
    private void notifyNetworkStateUpdated(boolean connected) {
        AppCenterLog.debug(LOG_TAG, "Network has been " + (connected ? "connected." : "disconnected."));
        for (Listener listener : mListeners) {
            listener.onNetworkStateUpdated(connected);
        }
    }

    @Override
    public synchronized void close() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
        } else {
            mContext.unregisterReceiver(mConnectivityReceiver);
        }
    }

    /**
     * Add a network state listener.
     *
     * @param listener listener to add.
     */
    public synchronized void addListener(Listener listener) {
        mListeners.add(listener);
    }

    /**
     * Remove a network state listener.
     *
     * @param listener listener to remove.
     */
    public synchronized void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    /**
     * Network state listener specification.
     */
    public interface Listener {

        /**
         * Called whenever the network state is updated.
         *
         * @param connected true if connected, false otherwise.
         */
        void onNetworkStateUpdated(boolean connected);
    }

    /**
     * Class receiving connectivity changes.
     */
    private class ConnectivityReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            handleNetworkStateUpdate();
        }
    }
}
