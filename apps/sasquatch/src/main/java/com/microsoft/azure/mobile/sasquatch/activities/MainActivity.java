package com.microsoft.azure.mobile.sasquatch.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.test.espresso.idling.CountingIdlingResource;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.ResultCallback;
import com.microsoft.azure.mobile.analytics.Analytics;
import com.microsoft.azure.mobile.analytics.AnalyticsPrivateHelper;
import com.microsoft.azure.mobile.analytics.channel.AnalyticsListener;
import com.microsoft.azure.mobile.analytics.ingestion.models.EventLog;
import com.microsoft.azure.mobile.analytics.ingestion.models.PageLog;
import com.microsoft.azure.mobile.crashes.AbstractCrashesListener;
import com.microsoft.azure.mobile.crashes.Crashes;
import com.microsoft.azure.mobile.crashes.model.ErrorReport;
import com.microsoft.azure.mobile.distribute.Distribute;
import com.microsoft.azure.mobile.ingestion.models.LogWithProperties;
import com.microsoft.azure.mobile.sasquatch.R;
import com.microsoft.azure.mobile.sasquatch.features.TestFeatures;
import com.microsoft.azure.mobile.sasquatch.features.TestFeaturesListAdapter;
import com.microsoft.azure.mobile.sasquatch.utils.SasquatchDistributeListener;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    static final String APP_SECRET_KEY = "appSecret";
    static final String LOG_URL_KEY = "logUrl";
    @VisibleForTesting
    static final CountingIdlingResource analyticsIdlingResource = new CountingIdlingResource("analytics");
    @VisibleForTesting
    static final CountingIdlingResource crashesIdlingResource = new CountingIdlingResource("crashes");
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

        /* Set listeners. */
        AnalyticsPrivateHelper.setListener(getAnalyticsListener());
        Crashes.setListener(getCrashesListener());

        /* Set distribute listener. */
        try {
            Distribute.class.getMethod("setListener", Class.forName("com.microsoft.azure.mobile.distribute.DistributeListener")).invoke(null, new SasquatchDistributeListener());
        } catch (Exception e) {
            Log.i(LOG_TAG, "Distribute listener is not yet available in this flavor.");
        }

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

    private AbstractCrashesListener getCrashesListener() {
        return new AbstractCrashesListener() {
            @Override
            public boolean shouldAwaitUserConfirmation() {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder
                        .setTitle(R.string.crash_confirmation_dialog_title)
                        .setMessage(R.string.crash_confirmation_dialog_message)
                        .setPositiveButton(R.string.crash_confirmation_dialog_send_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Crashes.notifyUserConfirmation(Crashes.SEND);
                            }
                        })
                        .setNegativeButton(R.string.crash_confirmation_dialog_not_send_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Crashes.notifyUserConfirmation(Crashes.DONT_SEND);
                            }
                        })
                        .setNeutralButton(R.string.crash_confirmation_dialog_always_send_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Crashes.notifyUserConfirmation(Crashes.ALWAYS_SEND);
                            }
                        });
                builder.create().show();
                return true;
            }

            /* TODO (getErrorAttachment): Re-enable error attachment when the feature becomes available. */
//            @Override
//            public ErrorAttachment getErrorAttachment(ErrorReport report) {
//                return ErrorAttachments.attachment("This is a text attachment.", "This is a binary attachment.".getBytes(), "binary.txt", "text/plain");
//            }

            @Override
            public void onBeforeSending(ErrorReport report) {
                Toast.makeText(MainActivity.this, R.string.crash_before_sending, Toast.LENGTH_SHORT).show();
                crashesIdlingResource.increment();
            }

            @Override
            public void onSendingFailed(ErrorReport report, Exception e) {
                Toast.makeText(MainActivity.this, R.string.crash_sent_failed, Toast.LENGTH_SHORT).show();
                crashesIdlingResource.decrement();
            }

            @Override
            public void onSendingSucceeded(ErrorReport report) {

                @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
                String message = String.format("%s\nCrash ID: %s", getString(R.string.crash_sent_succeeded), report.getId());
                if (report.getThrowable() != null) {
                    message += String.format("\nThrowable: %s", report.getThrowable().toString());
                }
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                crashesIdlingResource.decrement();
            }
        };
    }

    private AnalyticsListener getAnalyticsListener() {
        return new AnalyticsListener() {

            @Override
            public void onBeforeSending(com.microsoft.azure.mobile.ingestion.models.Log log) {
                if (log instanceof EventLog) {
                    Toast.makeText(MainActivity.this, R.string.event_before_sending, Toast.LENGTH_SHORT).show();
                } else if (log instanceof PageLog) {
                    Toast.makeText(MainActivity.this, R.string.page_before_sending, Toast.LENGTH_SHORT).show();
                }
                analyticsIdlingResource.increment();
            }

            @Override
            public void onSendingFailed(com.microsoft.azure.mobile.ingestion.models.Log log, Exception e) {
                String message = null;
                if (log instanceof EventLog) {
                    message = getString(R.string.event_sent_failed);
                } else if (log instanceof PageLog) {
                    message = getString(R.string.page_sent_failed);
                }
                if (message != null) {
                    message = String.format("%s\nException: %s", message, e.toString());
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
                analyticsIdlingResource.decrement();
            }

            @Override
            public void onSendingSucceeded(com.microsoft.azure.mobile.ingestion.models.Log log) {
                String message = null;
                if (log instanceof EventLog) {
                    message = String.format("%s\nName: %s", getString(R.string.event_sent_succeeded), ((EventLog) log).getName());
                } else if (log instanceof PageLog) {
                    message = String.format("%s\nName: %s", getString(R.string.page_sent_succeeded), ((PageLog) log).getName());
                }
                if (message != null) {
                    if (((LogWithProperties) log).getProperties() != null) {
                        message += String.format("\nProperties: %s", new JSONObject(((LogWithProperties) log).getProperties()).toString());
                    }
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
                analyticsIdlingResource.decrement();
            }
        };
    }
}
