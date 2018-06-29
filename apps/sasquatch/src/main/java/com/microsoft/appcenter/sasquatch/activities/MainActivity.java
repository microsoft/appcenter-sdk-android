package com.microsoft.appcenter.sasquatch.activities;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.analytics.AnalyticsPrivateHelper;
import com.microsoft.appcenter.analytics.channel.AnalyticsListener;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.crashes.CrashesListener;
import com.microsoft.appcenter.crashes.model.ErrorReport;
import com.microsoft.appcenter.distribute.Distribute;
import com.microsoft.appcenter.push.Push;
import com.microsoft.appcenter.push.PushListener;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.features.TestFeatures;
import com.microsoft.appcenter.sasquatch.features.TestFeaturesListAdapter;
import com.microsoft.appcenter.sasquatch.listeners.SasquatchAnalyticsListener;
import com.microsoft.appcenter.sasquatch.listeners.SasquatchCrashesListener;
import com.microsoft.appcenter.sasquatch.listeners.SasquatchDistributeListener;
import com.microsoft.appcenter.sasquatch.listeners.SasquatchPushListener;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;

import java.lang.reflect.Method;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final String LOG_TAG = "AppCenterSasquatch";

    static final String APP_SECRET_KEY = "appSecret";

    static final String TARGET_KEY = "target";

    static final String APPCENTER_START_TYPE = "appCenterStartType";

    static final String LOG_URL_KEY = "logUrl";

    static final String FIREBASE_ENABLED_KEY = "firebaseEnabled";

    private static final String SENDER_ID = "177539951155";

    private static final String TEXT_ATTACHMENT_KEY = "textAttachment";

    private static final String FILE_ATTACHMENT_KEY = "fileAttachment";

    static SharedPreferences sSharedPreferences;

    @SuppressLint("StaticFieldLeak")
    static SasquatchAnalyticsListener sAnalyticsListener;

    @SuppressLint("StaticFieldLeak")
    static SasquatchCrashesListener sCrashesListener;

    static SasquatchPushListener sPushListener;

    static {
        System.loadLibrary("SasquatchBreakpad");
    }

    public static void setTextAttachment(String textAttachment) {
        SharedPreferences.Editor editor = sSharedPreferences.edit();
        if (textAttachment == null) {
            editor.remove(TEXT_ATTACHMENT_KEY);
        } else {
            editor.putString(TEXT_ATTACHMENT_KEY, textAttachment);
        }
        editor.apply();
        sCrashesListener.setTextAttachment(textAttachment);
    }

    public static void setFileAttachment(Uri fileAttachment) {
        SharedPreferences.Editor editor = sSharedPreferences.edit();
        if (fileAttachment == null) {
            editor.remove(FILE_ATTACHMENT_KEY);
        } else {
            editor.putString(FILE_ATTACHMENT_KEY, fileAttachment.toString());
        }
        editor.apply();
        sCrashesListener.setFileAttachment(fileAttachment);
    }

    native void setupNativeCrashesListener(String path);

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
            AppCenter.setLogUrl(logUrl);
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
        Push.setSenderId(SENDER_ID);

        /* Set crash attachments. */
        sCrashesListener.setTextAttachment(sSharedPreferences.getString(TEXT_ATTACHMENT_KEY, null));
        String fileAttachment = sSharedPreferences.getString(FILE_ATTACHMENT_KEY, null);
        if (fileAttachment != null) {
            sCrashesListener.setFileAttachment(Uri.parse(fileAttachment));
        }

        /* Enable Firebase analytics if we enabled the setting previously. */
        if (sSharedPreferences.getBoolean(FIREBASE_ENABLED_KEY, false)) {
            Push.enableFirebaseAnalytics(this);
        }

        /* Start App Center. */
        String startType = sSharedPreferences.getString(APPCENTER_START_TYPE, StartType.APP_SECRET.toString());
        startAppCenter(getApplication(), startType);

        /* Attach NDK Crash Handler after SDK is initialized. */
        Crashes.getMinidumpDirectory().thenAccept(new AppCenterConsumer<String>() {

            @Override
            public void accept(String path) {

                /* Path is null when Crashes is disabled. */
                if (path != null) {
                    setupNativeCrashesListener(path);
                }
            }
        });

        /* Use some App Center getters. */
        AppCenter.getInstallId().thenAccept(new AppCenterConsumer<UUID>() {

            @Override
            public void accept(UUID uuid) {
                Log.i(LOG_TAG, "InstallId=" + uuid);
            }
        });

        /* Print last crash. */
        Crashes.hasCrashedInLastSession().thenAccept(new AppCenterConsumer<Boolean>() {

            @Override
            public void accept(Boolean crashed) {
                Log.i(LOG_TAG, "Crashes.hasCrashedInLastSession=" + crashed);
            }
        });
        Crashes.getLastSessionCrashReport().thenAccept(new AppCenterConsumer<ErrorReport>() {

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

    @NonNull
    private AnalyticsListener getAnalyticsListener() {
        if (sAnalyticsListener == null) {
            sAnalyticsListener = new SasquatchAnalyticsListener(this);
        }
        return sAnalyticsListener;
    }

    @NonNull
    private CrashesListener getCrashesListener() {
        if (sCrashesListener == null) {
            sCrashesListener = new SasquatchCrashesListener(this);
        }
        return sCrashesListener;
    }

    @NonNull
    private PushListener getPushListener() {
        if (sPushListener == null) {
            sPushListener = new SasquatchPushListener();
        }
        return sPushListener;
    }

    static void startAppCenter(Application application, String startTypeString) {
        StartType startType = StartType.valueOf(startTypeString);
        if (startType == StartType.SKIP_START) {
            return;
        }
        String appId = sSharedPreferences.getString(APP_SECRET_KEY, application.getString(R.string.app_secret));
        String targetId = sSharedPreferences.getString(TARGET_KEY, application.getString(R.string.target_id));
        String appIdArg = "";
        switch (startType) {
            case APP_SECRET:
                appIdArg = appId;
                break;
            case TARGET:
                appIdArg = String.format("target=%s", targetId);
                break;
            case BOTH:
                appIdArg = String.format("appsecret=%s;target=%s", appId, targetId);
                break;
            case NO_SECRET:

                /* TODO remove reflection once API available in jCenter. */
                try {

                    @SuppressWarnings("JavaReflectionMemberAccess")
                    Method startMethod = AppCenter.class.getMethod("start", Application.class, Class[].class);
                    Class[] services = {Analytics.class, Crashes.class, Distribute.class, Push.class};
                    startMethod.invoke(null, application, services);
                } catch (NoSuchMethodException nse) {

                    /* On jCenter we still have to pass null or empty as appSecret parameter. */
                    break;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return;
        }
        AppCenter.start(application, appIdArg, Analytics.class, Crashes.class, Distribute.class, Push.class);
    }

    public enum StartType {
        APP_SECRET,
        TARGET,
        BOTH,
        NO_SECRET,
        SKIP_START
    }
}
