package com.microsoft.azure.mobile.sasquatch.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
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
import com.microsoft.azure.mobile.MobileCenterService;
import com.microsoft.azure.mobile.analytics.Analytics;
import com.microsoft.azure.mobile.analytics.AnalyticsPrivateHelper;
import com.microsoft.azure.mobile.analytics.channel.AnalyticsListener;
import com.microsoft.azure.mobile.analytics.ingestion.models.EventLog;
import com.microsoft.azure.mobile.analytics.ingestion.models.PageLog;
import com.microsoft.azure.mobile.crashes.AbstractCrashesListener;
import com.microsoft.azure.mobile.crashes.Crashes;
import com.microsoft.azure.mobile.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.azure.mobile.crashes.model.ErrorReport;
import com.microsoft.azure.mobile.distribute.Distribute;
import com.microsoft.azure.mobile.ingestion.models.LogWithProperties;
import com.microsoft.azure.mobile.push.Push;
import com.microsoft.azure.mobile.push.PushListener;
import com.microsoft.azure.mobile.push.PushNotification;
import com.microsoft.azure.mobile.sasquatch.R;
import com.microsoft.azure.mobile.sasquatch.SasquatchDistributeListener;
import com.microsoft.azure.mobile.sasquatch.features.TestFeatures;
import com.microsoft.azure.mobile.sasquatch.features.TestFeaturesListAdapter;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.async.MobileCenterConsumer;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    public static final String LOG_TAG = "MobileCenterSasquatch";
    static final String APP_SECRET_KEY = "appSecret";
    static final String LOG_URL_KEY = "logUrl";
    static final String FIREBASE_ENABLED_KEY = "firebaseEnabled";
    @VisibleForTesting
    static final CountingIdlingResource analyticsIdlingResource = new CountingIdlingResource("analytics");
    @VisibleForTesting
    static final CountingIdlingResource crashesIdlingResource = new CountingIdlingResource("crashes");
    static SharedPreferences sSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sSharedPreferences = getSharedPreferences("Sasquatch", Context.MODE_PRIVATE);
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().build());

        /* Set custom log URL if one was configured in settings. */
        String logUrl = sSharedPreferences.getString(LOG_URL_KEY, getString(R.string.log_url));
        if (!TextUtils.isEmpty(logUrl)) {
            MobileCenter.setLogUrl(logUrl);
        }

        /* Set listeners. */
        AnalyticsPrivateHelper.setListener(getAnalyticsListener());
        Crashes.setListener(getCrashesListener());
        Distribute.setListener(new SasquatchDistributeListener());
        Push.setListener(getPushListener());

        /* Set distribute urls. */
        String installUrl = getString(R.string.install_url);
        if (!TextUtils.isEmpty(installUrl)) {
            Distribute.setInstallUrl(installUrl);
        }
        String apiUrl = getString(R.string.api_url);
        if (!TextUtils.isEmpty(apiUrl)) {
            Distribute.setApiUrl(apiUrl);
        }

        /* Enable Firebase analytics if we enabled the setting previously. */
        if (sSharedPreferences.getBoolean(FIREBASE_ENABLED_KEY, false)) {
            Push.enableFirebaseAnalytics(this);
        }

        /* Start Mobile center. */
        MobileCenter.start(getApplication(), sSharedPreferences.getString(APP_SECRET_KEY, getString(R.string.app_secret)), Analytics.class, Crashes.class, Distribute.class, Push.class);

        /* If rum available, use it. */
        try {
            @SuppressWarnings("unchecked")
            Class<? extends MobileCenterService> rum = (Class<? extends MobileCenterService>) Class.forName("com.microsoft.azure.mobile.rum.RealUserMeasurements");
            rum.getMethod("setRumKey", String.class).invoke(null, getString(R.string.rum_key));

            /* Start rum. */
            MobileCenter.start(rum);
        } catch (Exception e) {
            MobileCenterLog.error(LOG_TAG, "Rum cannot be used.", e);
        }

        /* Use some mobile center getters. */
        MobileCenter.getInstallId().thenAccept(new MobileCenterConsumer<UUID>() {

            @Override
            public void accept(UUID uuid) {
                Log.i(LOG_TAG, "InstallId=" + uuid);
            }
        });

        /* Print last crash. */
        Crashes.hasCrashedInLastSession().thenAccept(new MobileCenterConsumer<Boolean>() {

            @Override
            public void accept(Boolean crashed) {
                Log.i(LOG_TAG, "Crashes.hasCrashedInLastSession=" + crashed);
            }
        });
        Crashes.getLastSessionCrashReport().thenAccept(new MobileCenterConsumer<ErrorReport>() {

            @Override
            public void accept(ErrorReport data) {
                if (data != null) {
                    Log.i(LOG_TAG, "Crashes.getLastSessionCrashReport().getThrowable()=", data.getThrowable());
                }
            }
        });

        /* Populate UI. */
        ((TextView) findViewById(R.id.package_name)).setText(String.format(getString(R.string.sdk_source_format), getPackageName().substring(getPackageName().lastIndexOf(".") + 1)));
        TestFeatures.initialize(this);
        ListView listView = findViewById(R.id.list);
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(LOG_TAG, "onNewIntent triggered");
        Push.checkLaunchedFromNotification(this, intent);
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

    @NonNull
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

            @Override
            public Iterable<ErrorAttachmentLog> getErrorAttachments(ErrorReport report) {

                /* Attach some text. */
                ErrorAttachmentLog textLog = ErrorAttachmentLog.attachmentWithText("This is a text attachment.", "text.txt");

                /* Attach app icon to test binary. */
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] bitMapData = stream.toByteArray();
                ErrorAttachmentLog binaryLog = ErrorAttachmentLog.attachmentWithBinary(bitMapData, "icon.jpeg", "image/jpeg");

                /* Return attachments as list. */
                return Arrays.asList(textLog, binaryLog);
            }

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

            @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
            @Override
            public void onSendingSucceeded(ErrorReport report) {
                String message = String.format("%s\nCrash ID: %s", getString(R.string.crash_sent_succeeded), report.getId());
                if (report.getThrowable() != null) {
                    message += String.format("\nThrowable: %s", report.getThrowable().toString());
                }
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                crashesIdlingResource.decrement();
            }
        };
    }

    @NonNull
    private PushListener getPushListener() {
        return new PushListener() {

            @Override
            public void onPushNotificationReceived(Activity activity, PushNotification pushNotification) {
                String title = pushNotification.getTitle();
                String message = pushNotification.getMessage();
                Map<String, String> customData = pushNotification.getCustomData();
                MobileCenterLog.info(MainActivity.LOG_TAG, "Push received title=" + title + " message=" + message + " customData=" + customData + " activity=" + activity);
                if (message != null) {
                    android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(activity);
                    dialog.setTitle(title);
                    dialog.setMessage(message);
                    if (!customData.isEmpty()) {
                        dialog.setMessage(message + "\n" + customData);
                    }
                    dialog.setPositiveButton(android.R.string.ok, null);
                    dialog.show();
                } else {
                    Toast.makeText(activity, String.format(activity.getString(R.string.push_toast), customData), Toast.LENGTH_LONG).show();
                }
            }
        };
    }
}
