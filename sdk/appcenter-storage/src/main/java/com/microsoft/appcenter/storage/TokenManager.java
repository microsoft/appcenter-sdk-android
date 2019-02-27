package com.microsoft.appcenter.storage;

import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;
import com.google.gson.Gson;
import com.microsoft.appcenter.storage.models.TokenResult;
import org.json.JSONException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

public class TokenManager {

    // TODO use Gson object from shared module.
    private final Gson gson;

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

    public Set<String> getPartitionNames() {
        Set<String> partitionNames = SharedPreferencesManager.getStringSet(Constants.PARTITION_NAMES);
        return partitionNames == null ? new HashSet<String>() : partitionNames;
    }

    public TokenResult getCachedToken(String partitionName) {
        TokenResult token = gson.fromJson(SharedPreferencesManager.getString(partitionName), TokenResult.class);
        if (token != null){
            Calendar aGMTCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

            /* The token is considered expired. */
            try {
                if (aGMTCalendar.getTime().compareTo(token.expiresOn()) > 0) {
                    removeCachedToken(partitionName);
                    return null;
                }
            } catch(JSONException ex) {
                return null;
            }
        }
        return token;
    }

    public synchronized void setCachedToken(TokenResult tokenResult) {
        Set<String> partitionNamesSet = getPartitionNames();
        if (!partitionNamesSet.contains(tokenResult.partition())) {
            partitionNamesSet.add(tokenResult.partition());
            SharedPreferencesManager.putStringSet(Constants.PARTITION_NAMES, partitionNamesSet);
        }
        SharedPreferencesManager.putString(tokenResult.partition(), gson.toJson(tokenResult));
    }

    public synchronized void removeCachedToken(String partitionName) {
        Set<String> partitionNamesSet = getPartitionNames();
        partitionNamesSet.remove(partitionName);
        SharedPreferencesManager.putStringSet(Constants.PARTITION_NAMES, partitionNamesSet);
        SharedPreferencesManager.remove(partitionName);
    }
}
