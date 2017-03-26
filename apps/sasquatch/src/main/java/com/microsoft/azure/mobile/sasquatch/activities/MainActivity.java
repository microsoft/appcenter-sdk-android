package com.microsoft.azure.mobile.sasquatch.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.ResultCallback;
import com.microsoft.azure.mobile.analytics.Analytics;
import com.microsoft.azure.mobile.crashes.Crashes;
import com.microsoft.azure.mobile.crashes.model.ErrorReport;
import com.microsoft.azure.mobile.distribute.Distribute;
import com.microsoft.azure.mobile.sasquatch.R;
import com.microsoft.azure.mobile.sasquatch.features.TestFeatures;
import com.microsoft.azure.mobile.sasquatch.features.TestFeaturesListAdapter;
import com.microsoft.azure.mobile.sasquatch.utils.CrashesListenerProvider;

public class MainActivity extends AppCompatActivity {

    static final String APP_SECRET_KEY = "appSecret";
    static final String LOG_URL_KEY = "logUrl";
    private static final String LOG_TAG = "MobileCenterSasquatch";
    static SharedPreferences sSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sSharedPreferences = getSharedPreferences("Sasquatch", Context.MODE_PRIVATE);
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().build());

        /* Set custom log URL if one was configured in settings. */
        String logUrl = sSharedPreferences.getString(LOG_URL_KEY, getString(R.string.log_url));
        if (!TextUtils.isEmpty(logUrl)) {
            MobileCenter.setLogUrl(logUrl);
        }

        /* Set crash listener. */
        Crashes.setListener(CrashesListenerProvider.provideCrashesListener(this));

        /* Set distribute urls. */
        String installUrl = getString(R.string.install_url);
        if (!TextUtils.isEmpty(installUrl)) {
            Distribute.setInstallUrl(installUrl);
        }
        String apiUrl = getString(R.string.api_url);
        if (!TextUtils.isEmpty(apiUrl)) {
            Distribute.setApiUrl(apiUrl);
        }

        /* Start Mobile center. */
        MobileCenter.start(getApplication(), sSharedPreferences.getString(APP_SECRET_KEY, getString(R.string.app_secret)), Analytics.class, Crashes.class, Distribute.class);

        /* Print last crash. */
        Log.i(LOG_TAG, "Crashes.hasCrashedInLastSession=" + Crashes.hasCrashedInLastSession());
        Crashes.getLastSessionCrashReport(new ResultCallback<ErrorReport>() {

            @Override
            public void onResult(@Nullable ErrorReport data) {
                if (data != null) {
                    Log.i(LOG_TAG, "Crashes.getLastSessionCrashReport().getThrowable()=", data.getThrowable());
                }
            }
        });

        /* Populate UI. */
        ((TextView) findViewById(R.id.package_name)).setText(String.format(getString(R.string.sdk_source_format), getPackageName().substring(getPackageName().lastIndexOf(".") + 1)));
        TestFeatures.initialize(this);
        ListView listView = (ListView) findViewById(R.id.list);
        listView.setAdapter(new TestFeaturesListAdapter(TestFeatures.getAvailableControls()));
        listView.setOnItemClickListener(TestFeatures.getOnItemClickListener());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
        return true;
    }
}
