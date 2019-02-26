package com.microsoft.appcenter.storage;

import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;
import com.google.gson.Gson;
import com.microsoft.appcenter.storage.models.TokenResult;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

public class TokenManager {
    private final Gson gson; // TODO use Gson object from shared module.

    private static TokenManager tInstance;

    private TokenManager() {
        gson = new Gson();
    }

    public static TokenManager getInstance() {
        if (tInstance == null) {
            tInstance = new TokenManager();
        }
        return tInstance;
    }

    public Set<String> getPartitionNamesSet() {
        Set<String> partitionNamesSet = SharedPreferencesManager.getStringSet(Constants.PARTITION_NAMES);
        return partitionNamesSet == null ? new HashSet<String>() : partitionNamesSet;
    }

    public TokenResult getCachedToken(String partitionName) {
        TokenResult token = gson.fromJson(SharedPreferencesManager.getString(partitionName), TokenResult.class);
        if (token != null){
            Calendar aGMTCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

            /** The token is considered expired. **/
            if (aGMTCalendar.getTime().getTime() > token.ttl()) {
                removeCachedToken(partitionName);
                return null;
            }
        }
        return token;
    }

    public synchronized void setCachedToken(TokenResult tokenResult) {
        Set<String> partitionNamesSet = getPartitionNamesSet();
        if (!partitionNamesSet.contains(tokenResult.partition())) {
            partitionNamesSet.add(tokenResult.partition());
            SharedPreferencesManager.putStringSet(Constants.PARTITION_NAMES, partitionNamesSet);
        }
        SharedPreferencesManager.putString(tokenResult.partition(), gson.toJson(tokenResult));
    }

    public synchronized void removeCachedToken(String partitionName) {
        Set<String> partitionNamesSet = getPartitionNamesSet();
        partitionNamesSet.remove(partitionName);
        SharedPreferencesManager.putStringSet(Constants.PARTITION_NAMES, partitionNamesSet);
        SharedPreferencesManager.remove(partitionName);
    }
}
