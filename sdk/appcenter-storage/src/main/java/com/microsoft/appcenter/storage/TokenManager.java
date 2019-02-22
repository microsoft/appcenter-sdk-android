package com.microsoft.appcenter.storage;

import android.content.SharedPreferences;
import android.content.Context;
import com.google.gson.Gson;
import com.microsoft.appcenter.storage.models.TokenResult;
import java.time.Instant;
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

    private SharedPreferences getTokenCacheHandler(Context context) {
        return context.getSharedPreferences(Constants.TOKEN_CACHE_FILE, context.MODE_PRIVATE);
    }

    private SharedPreferences.Editor getTokenCacheWriteHandler(Context context) {
        return getTokenCacheHandler(context).edit();
    }

    public String[] ListPartitionNames(Context context) {
        SharedPreferences handler = getTokenCacheHandler(context);
        return new Gson().fromJson(handler.getString(Constants.PARTITION_NAMES, "[]"), String[].class);
    }

    public TokenResult getToken(Context context, String partitionName) {
        SharedPreferences handler = context.getSharedPreferences(Constants.TOKEN_CACHE_FILE, context.MODE_PRIVATE);
        TokenResult token = new Gson().fromJson(handler.getString(partitionName, null), TokenResult.class);
        if (token != null){
            long now = Date.from(Instant.now()).getTime();

            // the token is considered expired
            if (now > token.ttl()) {
                removeToken(context, partitionName);
                return null;
            }
        }
        return token;
    }

    public void setToken(Context context, TokenResult tokenResult) {
        SharedPreferences.Editor writeHandler = getTokenCacheWriteHandler(context);
        List<String> partitionNames = Arrays.asList(ListPartitionNames(context));
        if (!partitionNames.contains(tokenResult.partition())) {
            partitionNames.add(tokenResult.partition());
            writeHandler.putString(Constants.PARTITION_NAMES, new Gson().toJson(partitionNames));
        }
        writeHandler.putString(tokenResult.partition(), new Gson().toJson(tokenResult)).commit();
    }

    public void removeToken(Context context, String partitionName) {
        SharedPreferences.Editor writeHandler = getTokenCacheWriteHandler(context);
        writeHandler.remove(partitionName).commit();
    }
}
