package com.microsoft.appcenter.sasquatch.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.microsoft.appcenter.sasquatch.R;

import static com.microsoft.appcenter.sasquatch.activities.MainActivity.LOG_TAG;

public class StorageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage);

        /* TODO remove reflection once Storage published to jCenter. */
        try {
            final Class<?> storage = Class.forName("com.microsoft.appcenter.storage.Storage");
            storage.getMethod("read").invoke(null);
        }catch (Exception ignore) {
            Log.e(LOG_TAG, "Storage.Read failed", ignore);
        }
    }
}
