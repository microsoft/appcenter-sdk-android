package com.microsoft.appcenter.utils;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.AppCenter;

import java.io.Closeable;
import java.util.HashSet;
import java.util.Set;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;

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
     * Our connectivity event receiver.
     */
    private final ConnectivityReceiver mConnectivityReceiver;

    /**
     * Network state listeners that will subscribe to us.
     */
    private final Set<Listener> mListeners = new HashSet<>();

    /**
     * Current network type, null for disconnected.
     */
    private String mNetworkType;

    /**
     * Init.
     *
     * @param context any Android context.
     */
    @VisibleForTesting
    NetworkStateHelper(Context context) {
        mContext = context.getApplicationContext();
        mConnectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        mConnectivityReceiver = new ConnectivityReceiver();
        updateNetworkType();
        context.registerReceiver(mConnectivityReceiver, new IntentFilter(CONNECTIVITY_ACTION));
    }

    /**
     * Get shared instance.
     *
     * @param context any context.
     * @return shared instance.
     */
    public static NetworkStateHelper getSharedInstance(Context context) {
        if (sSharedInstance == null) {
            sSharedInstance = new NetworkStateHelper(context);
        }
        return sSharedInstance;
    }

    /**
     * Check whether the network is currently connected.
     *
     * @return true for connected, false for disconnected.
     */
    public boolean isNetworkConnected() {
        return mNetworkType != null;
    }

    /**
     * Update network type by polling.
     */
    private void updateNetworkType() {

        /* Get active network info */
        NetworkInfo networkInfo;
        try {
            networkInfo = mConnectivityManager.getActiveNetworkInfo();
        } catch (RuntimeException e) {

            /*
             * Can fail with either SecurityException or even NullPointerException on "corrupted" devices.
             */
            networkInfo = null;
            AppCenterLog.error(AppCenter.LOG_TAG, "Could not get network info and thus stuck in disconnected state, please check you declared android.permission.ACCESS_NETWORK_STATE");
        }
        updateNetworkType(networkInfo);
    }

    /**
     * Update network type with the specified update.
     */
    private void updateNetworkType(NetworkInfo networkInfo) {
        AppCenterLog.debug(AppCenter.LOG_TAG, "Active network info=" + networkInfo);

        /* Update network type. null for not connected. */
        if (networkInfo != null && networkInfo.isConnected()) {
            mNetworkType = networkInfo.getTypeName() + networkInfo.getSubtypeName();
        } else {
            mNetworkType = null;
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
    public void close() {
        mContext.unregisterReceiver(mConnectivityReceiver);
    }

    /**
     * Add a network state listener.
     *
     * @param listener listener to add.
     */
    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    /**
     * Remove a network state listener.
     *
     * @param listener listener to remove.
     */
    public void removeListener(Listener listener) {
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

            /*
             * This code is used to notify listeners only when the network state goes from
             * connected to disconnected and vice versa
             * (without duplicate calls, the sequence will be consistent).
             *
             * If we switch from WIFI to Mobile and vice versa,
             * it can take a while for pending network calls to fail because of that.
             * We'll simulate a network state down event to the listeners to help with that scenario.
             */
            String previousNetworkType = mNetworkType;

            /*
             * getActiveNetworkInfo has a bug on Android 8
             * so we need to use the deprecated extra info.
             * See https://issuetracker.google.com/issues/37137911.
             */
            @SuppressWarnings("deprecation")
            NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            updateNetworkType(networkInfo);
            boolean networkTypeChanged = previousNetworkType == null ? mNetworkType != null : !previousNetworkType.equals(mNetworkType);
            if (networkTypeChanged) {
                boolean connected = isNetworkConnected();
                if (connected && previousNetworkType != null) {
                    notifyNetworkStateUpdated(false);
                }
                notifyNetworkStateUpdated(connected);
            }
        }
    }
}
