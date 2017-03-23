package com.microsoft.azure.mobile.sasquatch.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.Nullable;
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
import com.microsoft.azure.mobile.crashes.AbstractCrashesListener;
import com.microsoft.azure.mobile.crashes.Crashes;
import com.microsoft.azure.mobile.crashes.model.ErrorReport;
import com.microsoft.azure.mobile.distribute.Distribute;
import com.microsoft.azure.mobile.distribute.DistributeListener;
import com.microsoft.azure.mobile.distribute.ReleaseDetails;
import com.microsoft.azure.mobile.distribute.UserUpdateAction;
import com.microsoft.azure.mobile.sasquatch.R;
import com.microsoft.azure.mobile.sasquatch.features.TestFeatures;
import com.microsoft.azure.mobile.sasquatch.features.TestFeaturesListAdapter;

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
        Crashes.setListener(getCrashesListener());

        Distribute.setListener(new DistributeListener() {

            @Override
            public boolean shouldCustomizeUpdateDialog(ReleaseDetails releaseDetails) {
                final String releaseNotes = releaseDetails.getReleaseNotes();
                return releaseNotes != null && releaseNotes.toLowerCase().contains("custom");
            }

            @Override
            public Dialog buildUpdateDialog(Activity activity, ReleaseDetails releaseDetails) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
                dialogBuilder.setTitle("Version " + releaseDetails.getShortVersion() + " available!");
                String releaseNotes = releaseDetails.getReleaseNotes();
                if (TextUtils.isEmpty(releaseNotes))
                    dialogBuilder.setMessage(com.microsoft.azure.mobile.distribute.R.string.mobile_center_distribute_update_dialog_message);
                else
                    dialogBuilder.setMessage(releaseNotes);
                dialogBuilder.setPositiveButton(com.microsoft.azure.mobile.distribute.R.string.mobile_center_distribute_update_dialog_download, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Distribute.notifyUserUpdateAction(UserUpdateAction.DOWNLOAD);
                    }
                });
                if (releaseDetails.isMandatoryUpdate()) {
                    dialogBuilder.setCancelable(false);
                } else {
                    dialogBuilder.setNegativeButton(com.microsoft.azure.mobile.distribute.R.string.mobile_center_distribute_update_dialog_ignore, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Distribute.notifyUserUpdateAction(UserUpdateAction.IGNORE);
                        }
                    });
                    dialogBuilder.setNeutralButton(com.microsoft.azure.mobile.distribute.R.string.mobile_center_distribute_update_dialog_postpone, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Distribute.notifyUserUpdateAction(UserUpdateAction.POSTPONE);
                        }
                    });
                    dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            Distribute.notifyUserUpdateAction(UserUpdateAction.POSTPONE);
                        }
                    });
                }
                dialogBuilder.create().show();
                return null;
            }
        });

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
            }

            @Override
            public void onSendingFailed(ErrorReport report, Exception e) {
                Toast.makeText(MainActivity.this, R.string.crash_sent_failed, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSendingSucceeded(ErrorReport report) {

                @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
                String message = String.format("%s\nCrash ID: %s\nThrowable: %s", R.string.crash_sent_succeeded, report.getId(), report.getThrowable().toString());
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        };
    }
}
