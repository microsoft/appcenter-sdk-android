package com.microsoft.appcenter.storage;

import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;
import com.google.gson.Gson;
import com.microsoft.appcenter.storage.models.TokenResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class TokenManager {

    private final Gson gson;

    private static TokenManager tInstance;

    private TokenManager() {
        gson = new Gson();
    }

    public static synchronized TokenManager getInstance() {
        if (tInstance == null) {
            tInstance = new TokenManager();
        }
        return tInstance;
    }

    public List<String> ListPartitionNames() {
        String partitionNameStrings = SharedPreferencesManager.getString(Constants.PARTITION_NAMES);
        return partitionNameStrings == null ? new ArrayList<String>() : new ArrayList<>(Arrays.asList(gson.fromJson(partitionNameStrings, String[].class)));
    }

    public TokenResult cachedToken(String partitionName) {
        TokenResult token = gson.fromJson(SharedPreferencesManager.getString(partitionName), TokenResult.class);
        if (token != null){
            long now = new Date().getTime();

            // the token is considered expired
            if (now > token.ttl()) {
                removeToken(partitionName);
                return null;
            }
        }
        return token;
    }

    public void setToken(TokenResult tokenResult) {
        List<String> partitionNames = ListPartitionNames();
        if (!partitionNames.contains(tokenResult.partition())) {
            partitionNames.add(tokenResult.partition());
            SharedPreferencesManager.putString(Constants.PARTITION_NAMES, gson.toJson(partitionNames.toArray()));
        }
        SharedPreferencesManager.putString(tokenResult.partition(), gson.toJson(tokenResult));
    }

    public void removeToken(String partitionName) {
        SharedPreferencesManager.remove(partitionName);
    }
}
