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
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.AppCenter;

import java.io.Closeable;
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
     * Currently available networks, always empty on API level < 21.
     */
    private final Set<Network> mAvailableNetworks = new HashSet<>();

    /**
     * Network callback, null on API level < 21.
     */
    private ConnectivityManager.NetworkCallback mNetworkCallback;

    /**
     * Current network type, null for disconnected or API level >= 21.
     */
    private String mNetworkType;

    /**
     * Our connectivity event receiver, null on API level >= 21.
     */
    private ConnectivityReceiver mConnectivityReceiver;

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

                /* Build query to get a working network listener. */
                NetworkRequest.Builder request = new NetworkRequest.Builder();
                request.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    request.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                }
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
                updateNetworkType();
                mConnectivityReceiver = new ConnectivityReceiver();
                mContext.registerReceiver(mConnectivityReceiver, getOldIntentFilter());
            }
        } catch (RuntimeException e) {

            /*
             * Can be security exception if permission missing or sometimes another runtime exception
             * on some customized firmwares.
             */
            AppCenterLog.error(LOG_TAG, "Cannot access network state information", e);
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
        return mNetworkType != null || !mAvailableNetworks.isEmpty();
    }

    /**
     * Handle network available update on API level >= 21.
     */
    private synchronized void onNetworkAvailable(Network network) {
        AppCenterLog.debug(LOG_TAG, "Network available netId: " + network);
        mAvailableNetworks.add(network);
        AppCenterLog.debug(LOG_TAG, "Available networks netIds: " + mAvailableNetworks);

        /*
         * Trigger event only once if we gain a new network while one was already
         * available. Special logic is handled in network lost events.
         */
        if (mAvailableNetworks.size() == 1) {
            notifyNetworkStateUpdated(true);
        }
    }

    /**
     * Handle network available update on API level >= 21.
     */
    private synchronized void onNetworkLost(Network network) {

        /*
         * We will have WIFI network available event before we lose mobile network.
         * This notification scheme is similar to the old connectivity receiver implementation.
         */
        AppCenterLog.debug(LOG_TAG, "Network lost netId: " + network);
        mAvailableNetworks.remove(network);
        AppCenterLog.debug(LOG_TAG, "Available networks netIds: " + mAvailableNetworks);
        notifyNetworkStateUpdated(false);
        if (!mAvailableNetworks.isEmpty()) {
            notifyNetworkStateUpdated(true);
        }
    }

    /**
     * Update network type by polling on API level < 21.
     */
    @SuppressWarnings("deprecation")
    private void updateNetworkType() {

        /* Get active network info */
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        AppCenterLog.debug(AppCenter.LOG_TAG, "Active network info=" + networkInfo);

        /* Update network type. null for not connected. */
        if (networkInfo != null && networkInfo.isConnected()) {
            mNetworkType = networkInfo.getTypeName() + networkInfo.getSubtypeName();
        } else {
            mNetworkType = null;
        }
    }

    /**
     * Handle network state update on API level < 21.
     */
    private synchronized void handleNetworkStateUpdate() {

        /*
         * This code is used to notify listeners only when the network state goes from
         * connected to disconnected and vice versa
         * (without duplicate calls, the sequence will be consistent).
         */
        String previousNetworkType = mNetworkType;
        updateNetworkType();
        boolean networkTypeChanged = previousNetworkType == null ? mNetworkType != null : !previousNetworkType.equals(mNetworkType);
        if (networkTypeChanged) {
            boolean connected = isNetworkConnected();
            if (connected && previousNetworkType != null) {
                notifyNetworkStateUpdated(false);
            }
            notifyNetworkStateUpdated(connected);
        }
    }

    /**
     * Notify listeners that the network state changed.
     *
     * @param connected whether the network is connected or not.
     */
    private void notifyNetworkStateUpdated(boolean connected) {
        for (Listener listener : mListeners) {
            listener.onNetworkStateUpdated(connected);
        }
    }

    @Override
    public synchronized void close() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
            mAvailableNetworks.clear();
        } else {
            mContext.unregisterReceiver(mConnectivityReceiver);
            mNetworkType = null;
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
