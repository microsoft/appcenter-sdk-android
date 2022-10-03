/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.permissions;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity for requesting permissions.
 */
public class PermissionRequestActivity extends Activity {

    /**
     * Result of requesting permissions.
     */
    public static class Result {
        public final Exception exception;
        public final Map<String, Boolean> permissionRequestResults;

        public Result(@Nullable Map<String, Boolean> permissionRequestResults, @Nullable Exception exception) {
            this.permissionRequestResults = permissionRequestResults;
            this.exception = exception;
        }

        public boolean areAllPermissionsGranted() {
            if (permissionRequestResults != null && permissionRequestResults.size() > 0) {
                return !permissionRequestResults.containsValue(false);
            }
            return false;
        }
    }

    @VisibleForTesting
    static final String EXTRA_PERMISSIONS = "intent.extra.PERMISSIONS";

    @VisibleForTesting
    static final int REQUEST_CODE = PermissionRequestActivity.class.getName().hashCode();

    /**
     * Tracking last request to send result to the listener.
     */
    @VisibleForTesting
    static DefaultAppCenterFuture<Result> sResultFuture;

    /**
     * Start activity for requesting permissions.
     *
     * @param context The context from which the activity will be started.
     * @param permissions List of requested permissions.
     * @return Future with the result of a permissions request.
     */
    public static AppCenterFuture<Result> requestPermissions(@NonNull Context context, String... permissions) {
        if (sResultFuture != null) {
            AppCenterLog.error(LOG_TAG, "Result future flag is null.");
            return null;
        }
        sResultFuture = new DefaultAppCenterFuture<>();
        Intent intent = new Intent(context, PermissionRequestActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(EXTRA_PERMISSIONS, permissions);
        context.startActivity(intent);
        return sResultFuture;
    }

    @VisibleForTesting
    static void complete(@NonNull Result result) {
        if (sResultFuture != null) {
            sResultFuture.complete(result);
            sResultFuture = null;
            return;
        }
        AppCenterLog.debug(LOG_TAG, "The start of the activity was not called using the requestPermissions function or the future has already been completed");
    }

    @Nullable
    private String[] getPermissionsList() {
        Intent intent = getIntent();
        if (intent == null) {
            return null;
        }
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return null;
        }
        return extras.getStringArray(EXTRA_PERMISSIONS);
    }

    private Map<String, Boolean> getPermissionsRequestResultMap(String[] permissions, int[] results) {
        Map<String, Boolean> resultsMap = new HashMap<>();
        if (permissions.length != results.length) {
            AppCenterLog.error(LOG_TAG, "Invalid argument array sizes.");
            return null;
        }
        for (int i = 0; i < permissions.length; i++) {
            resultsMap.put(permissions[i], results[i] == PackageManager.PERMISSION_GRANTED);
        }
        return resultsMap;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Exception exception = new UnsupportedOperationException("There is no need to request permissions in runtime on Android earlier than 6.0.");
            AppCenterLog.error(LOG_TAG, "Android version incompatible.", exception);
            complete(new Result(null, exception));
            finish();
            return;
        }
        String[] permissions = getPermissionsList();
        if (permissions == null) {
            Exception exception = new IllegalArgumentException("Failed to get permissions list from intents extras.");
            AppCenterLog.error(LOG_TAG, "Failed to get permissions list.", exception);
            complete(new Result(null, exception));
            finish();
            return;
        }
        requestPermissions(permissions, REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            Map<String, Boolean> results = getPermissionsRequestResultMap(permissions, grantResults);
            if (results == null) {
                complete(new Result(null, new IllegalArgumentException("Error while getting permission request results.")));
                return;
            }
            complete(new Result(results, null));
            finish();
        }
    }

    @Override
    public void finish() {
        super.finish();

        /*
         * Prevent closing animation because we don't need any animation or visual effect
         * from this wrapper activity.
         */
        overridePendingTransition(0, 0);
    }
}