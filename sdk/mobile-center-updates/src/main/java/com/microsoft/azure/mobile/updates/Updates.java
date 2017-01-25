package com.microsoft.azure.mobile.updates;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.azure.mobile.AbstractMobileCenterService;
import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.utils.MobileCenterLog;

import java.util.List;

public class Updates extends AbstractMobileCenterService {

    static final String LOG_TAG = MobileCenter.LOG_TAG + "Updates";

    private static final String GOOGLE_CHROME_URL_SCHEME = "googlechrome://navigate?url=";

    private static final String GENERIC_BROWSER_URL_SCHEME = "http://";

    private static final String DEFAULT_LOGIN_PAGE_URL = "10.123.212.163:8080/default.htm";

    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Updates sInstance = null;

    private Activity mForegroundActivity;

    private boolean mLoginChecked;

    /**
     * Get shared instance.
     *
     * @return shared instance.
     */
    public static synchronized Updates getInstance() {
        if (sInstance == null) {
            sInstance = new Updates();
        }
        return sInstance;
    }

    @VisibleForTesting
    static synchronized void unsetInstance() {
        sInstance = null;
    }

    @Override
    protected String getGroupName() {
        return null;
    }

    @Override
    protected String getServiceName() {
        return "Updates";
    }

    @Override
    protected String getLoggerTag() {
        return LOG_TAG;
    }

    @Override
    public synchronized void onChannelReady(@NonNull Context context, @NonNull Channel channel) {
        super.onChannelReady(context, channel);
        checkLogin();
    }

    @Override
    public void onActivityResumed(Activity activity) {
        mForegroundActivity = activity;
        checkLogin();
    }

    @Override
    public void onActivityPaused(Activity activity) {
        mForegroundActivity = null;
    }

    private void checkLogin() {
        if (mForegroundActivity != null && !mLoginChecked) {
            String baseUrl = DEFAULT_LOGIN_PAGE_URL + "?package=" + mForegroundActivity.getPackageName();
            Intent intent = new Intent(Intent.ACTION_VIEW);

            /* Try to force using Chrome first, we want fall back url support for intent. */
            try {
                intent.setData(Uri.parse(GOOGLE_CHROME_URL_SCHEME + baseUrl));
                mForegroundActivity.startActivity(intent);
            } catch (ActivityNotFoundException e) {

                /* Fall back using a browser but we don't want a chooser U.I. to pop. */
                MobileCenterLog.debug(LOG_TAG, "Google Chrome not found, pick another one.");
                intent.setData(Uri.parse(GENERIC_BROWSER_URL_SCHEME + baseUrl));
                List<ResolveInfo> browsers = mForegroundActivity.getPackageManager().queryIntentActivities(intent, 0);
                if (browsers.isEmpty()) {
                    MobileCenterLog.error(LOG_TAG, "No browser found on device, abort login.");
                } else {

                    /*
                     * Check the default browser is not the picker,
                     * last thing we want is app to start and suddenly asks user to pick
                     * between 2 browsers without explaining why.
                     */
                    String defaultBrowserPackageName = null;
                    String defaultBrowserClassName = null;
                    ResolveInfo defaultBrowser = mForegroundActivity.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
                    if (defaultBrowser != null) {
                        ActivityInfo activityInfo = defaultBrowser.activityInfo;
                        defaultBrowserPackageName = activityInfo.packageName;
                        defaultBrowserClassName = activityInfo.name;
                        MobileCenterLog.debug(LOG_TAG, "Default browser seems to be " + defaultBrowserPackageName + "/" + defaultBrowserClassName);
                    }
                    String selectedPackageName = null;
                    String selectedClassName = null;
                    for (ResolveInfo browser : browsers) {
                        ActivityInfo activityInfo = browser.activityInfo;
                        if (activityInfo.packageName.equals(defaultBrowserPackageName) && activityInfo.name.equals(defaultBrowserClassName)) {
                            selectedPackageName = defaultBrowserPackageName;
                            selectedClassName = defaultBrowserClassName;
                            MobileCenterLog.debug(LOG_TAG, "And its not the picker.");
                            break;
                        }
                    }
                    if (defaultBrowser != null && selectedPackageName == null) {
                        MobileCenterLog.debug(LOG_TAG, "Default browser is actually a picker...");
                    }

                    /* If no default browser found, pick first one we can find. */
                    if (selectedPackageName == null) {
                        MobileCenterLog.debug(LOG_TAG, "Picking first browser in list.");
                        ResolveInfo browser = browsers.iterator().next();
                        ActivityInfo activityInfo = browser.activityInfo;
                        selectedPackageName = activityInfo.packageName;
                        selectedClassName = activityInfo.name;
                    }

                    /* Launch generic browser. */
                    MobileCenterLog.debug(LOG_TAG, "Launch browser=" + selectedPackageName + "/" + selectedClassName);
                    intent.setClassName(selectedPackageName, selectedClassName);
                    mForegroundActivity.startActivity(intent);
                }
            }
            mLoginChecked = true;
        }
    }
}
