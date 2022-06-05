/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.install;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;

/**
 * Invisible activity used for wrapping system dialogs.
 */
public class ReleaseInstallerActivity extends Activity {

    /**
     * Result of system dialog.
     */
    public static class Result {
        public final int code;
        public final String message;

        public Result(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    /**
     * Tracking last request to send result to the listener.
     */
    private static DefaultAppCenterFuture<Result> sResultFuture;

    /**
     * Starts wrapper activity and system dialog inside.
     *
     * @param context any context.
     * @param trackedIntent intent of tracked system dialog.
     * @return future with result.
     */
    public static AppCenterFuture<Result> startActivityForResult(Context context, Intent trackedIntent) {
        if (sResultFuture != null) {
            AppCenterLog.error(LOG_TAG, "Another installing activity already in progress.");
            return null;
        }
        sResultFuture = new DefaultAppCenterFuture<>();
        Intent intent = new Intent(context, ReleaseInstallerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(Intent.EXTRA_INTENT, trackedIntent);
        context.startActivity(intent);
        return sResultFuture;
    }

    private static void complete(Result result) {
        if (sResultFuture != null) {
            sResultFuture.complete(result);
            sResultFuture = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent().getParcelableExtra(Intent.EXTRA_INTENT);
        if (intent == null) {
            AppCenterLog.warn(LOG_TAG, "Missing extra intent.");
            finish();
            return;
        }
        try {
            startActivityForResult(intent, 0);
        } catch (SecurityException e) {
            complete(new Result(RESULT_FIRST_USER, e.getMessage()));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        AppCenterLog.verbose(LOG_TAG, "Release installer activity result=" + resultCode);
        complete(new Result(resultCode, null));
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}