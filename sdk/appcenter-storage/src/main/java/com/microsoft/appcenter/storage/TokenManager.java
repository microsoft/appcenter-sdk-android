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

    public String[] ListPartitionNames() {
        return new Gson().fromJson(SharedPreferencesManager.getString(Constants.PARTITION_NAMES, "[]"), String[].class);
    }

    public TokenResult getToken(String partitionName) {
        TokenResult token = new Gson().fromJson(SharedPreferencesManager.getString(partitionName, null), TokenResult.class);
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
        List<String> partitionNames = new ArrayList<String>(Arrays.asList(ListPartitionNames()));
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
