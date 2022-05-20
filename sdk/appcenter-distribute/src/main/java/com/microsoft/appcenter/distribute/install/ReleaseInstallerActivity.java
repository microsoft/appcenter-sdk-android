package com.microsoft.appcenter.distribute.install;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;

public class ReleaseInstallerActivity extends Activity {

    public static class Result {
        public final int code;
        public final String message;

        public Result(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    private static DefaultAppCenterFuture<Result> sResultFuture;

    public static AppCenterFuture<Result> startActivityForResult(Context context, Intent trackedIntent) {
        if (sResultFuture != null) {
            AppCenterLog.error(LOG_TAG, "ALREADY IN PROGRESS");
            return null;
        }
        sResultFuture = new DefaultAppCenterFuture<>();
        Intent intent = new Intent(context, ReleaseInstallerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(Intent.EXTRA_INTENT, trackedIntent);

        // FIXME: StrictMode policy violation; ~duration=13 ms: android.os.strictmode.DiskReadViolation
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
            AppCenterLog.warn(LOG_TAG, "MISSING EXTRA INTENT");
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
        AppCenterLog.warn(LOG_TAG, "onActivityResult: " + resultCode);
        if (data != null && data.getExtras() != null) {
            for (String key : data.getExtras().keySet()) {
                AppCenterLog.warn(LOG_TAG, key + ": " + data.getExtras().get(key));
            }
        }
        complete(new Result(resultCode, null));
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}