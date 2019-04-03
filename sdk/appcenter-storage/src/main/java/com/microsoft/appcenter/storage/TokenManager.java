/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage;

import com.microsoft.appcenter.storage.models.TokenResult;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

/**
 * Token cache service.
 */
public class TokenManager {

    /**
     * Shared token manager instance.
     */
    private static TokenManager sInstance;

    private TokenManager() {
    }

    /**
     * Get token manager instance.
     *
     * @return Shared token manager instance.
     */
    public static TokenManager getInstance() {
        if (sInstance == null) {
            sInstance = new TokenManager();
        }
        return sInstance;
    }

    /**
     * List all cached tokens' partition names.
     *
     * @return Set of cached tokens' partition name.
     */
    private Set<String> getPartitionNames() {
        Set<String> partitionNames = SharedPreferencesManager.getStringSet(Constants.PARTITION_NAMES);
        return partitionNames == null ? new HashSet<String>() : partitionNames;
    }

    /**
     * Get the cached token access to given partition.
     *
     * @param partitionName The partition name for get the token.
     * @return Cached token.
     */
    public TokenResult getCachedToken(String partitionName) {
        TokenResult token = Utils.getGson().fromJson(SharedPreferencesManager.getString(partitionName), TokenResult.class);
        if (token != null) {
            Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

            /* The token is considered expired. */
            if (utcCalendar.getTime().compareTo(token.expiresOn()) > 0) {
                removeCachedToken(partitionName);
                return null;
            }
        }
        return token;
    }

    /**
     * Set the token to cache.
     *
     * @param tokenResult The token to be cached.
     */
    public synchronized void setCachedToken(TokenResult tokenResult) {
        Set<String> partitionNamesSet = getPartitionNames();
        String removedAccountIdPartition = Utils.removeAccountIdFromPartitionName(tokenResult.partition());
        if (!partitionNamesSet.contains(removedAccountIdPartition)) {
            partitionNamesSet.add(removedAccountIdPartition);
            SharedPreferencesManager.putStringSet(Constants.PARTITION_NAMES, partitionNamesSet);
        }
        SharedPreferencesManager.putString(removedAccountIdPartition, Utils.getGson().toJson(tokenResult));
    }


    /**
     * Remove the cached token access to specific partition.
     *
     * @param partitionName The partition name used to access the token.
     */
    private synchronized void removeCachedToken(String partitionName) {
        Set<String> partitionNamesSet = getPartitionNames();
        partitionNamesSet.remove(partitionName);
        SharedPreferencesManager.putStringSet(Constants.PARTITION_NAMES, partitionNamesSet);
        SharedPreferencesManager.remove(partitionName);
    }

    /**
     * Remove all the cached access tokens for all partition names.
     */
    @SuppressWarnings("WeakerAccess")
    public synchronized void removeAllCachedTokens() {
        Set<String> partitionNamesSet = getPartitionNames();
        for (String partitionName : partitionNamesSet) {
            if (partitionName.equals(Constants.READONLY)) {
                continue;
            }
            SharedPreferencesManager.remove(partitionName);
        }
        partitionNamesSet.clear();
        SharedPreferencesManager.putStringSet(Constants.PARTITION_NAMES, partitionNamesSet);
    }
}
