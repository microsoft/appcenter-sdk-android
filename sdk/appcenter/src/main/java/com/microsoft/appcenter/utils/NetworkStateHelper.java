/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

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
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final Set<Listener> mListeners = new CopyOnWriteArraySet<>();

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
    private final AtomicBoolean mConnected = new AtomicBoolean();

    /**
     * Init.
     *
     * @param context any Android context.
     */
    @VisibleForTesting
    public NetworkStateHelper(Context context) {
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
    public void reopen() {
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
            mConnected.set(true);
        }
    }

    @NonNull
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private IntentFilter getOldIntentFilter() {
        return new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    }

    /**
     * Check whether the network is currently connected.
     *
     * @return true for connected, false for disconnected.
     */
    public boolean isNetworkConnected() {
        return mConnected.get() || isAnyNetworkConnected();
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

            @SuppressWarnings({"deprecation", "RedundantSuppression"})
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
    private void onNetworkAvailable(Network network) {
        AppCenterLog.debug(LOG_TAG, "Network " + network + " is available.");
        if (mConnected.compareAndSet(false, true)) {
            notifyNetworkStateUpdated(true);
        }
    }

    /**
     * Handle network lost update on API level >= 21.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void onNetworkLost(Network network) {
        AppCenterLog.debug(LOG_TAG, "Network " + network + " is lost.");
        Network[] networks = mConnectivityManager.getAllNetworks();
        boolean noNetwork = networks == null || networks.length == 0 ||
                Arrays.equals(networks, new Network[]{network});
        if (noNetwork && mConnected.compareAndSet(true, false)) {
            notifyNetworkStateUpdated(false);
        }
    }

    /**
     * Handle network state update on API level < 21.
     */
    private void handleNetworkStateUpdate() {
        boolean connected = isAnyNetworkConnected();
        if (mConnected.compareAndSet(!connected, connected)) {
            notifyNetworkStateUpdated(connected);
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
    public void close() {
        mConnected.set(false);
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
            handleNetworkStateUpdate();
        }
    }
}
