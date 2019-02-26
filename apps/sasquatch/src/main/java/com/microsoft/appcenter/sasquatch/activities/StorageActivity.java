package com.microsoft.appcenter.sasquatch.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.gson.annotations.Expose;
import com.microsoft.appcenter.sasquatch.R;

import java.lang.reflect.ParameterizedType;

import static com.microsoft.appcenter.sasquatch.activities.MainActivity.LOG_TAG;

class Test{
    String test = "ABC";
}

public class StorageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage);

        /* TODO remove reflection once Storage published to jCenter. */
        try {
            final Class<?> storage = Class.forName("com.microsoft.appcenter.storage.Storage");
            storage.getMethod("read", String.class, String.class).invoke(null, "User124", "3456");
            storage.getMethod("delete", String.class, String.class).invoke(null, "User123c456q", "34567006");
            storage.getMethod("create", String.class, String.class, Object.class).invoke(null, "User1235", "dfrer", new Test());
        }catch (Exception ignore) {
            Log.e(LOG_TAG, "Storage.Module call failed", ignore);
        }
    }
}
