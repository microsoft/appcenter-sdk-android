package com.microsoft.appcenter.distribute.permissions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;

public class PermissionRequestActivity extends Activity {

    public static class Result {
        public final boolean isPermissionGranted;
        public final Exception exception;

        public Result(boolean isPermissionGranted, @Nullable Exception exception) {
            this.isPermissionGranted = isPermissionGranted;
            this.exception = exception;
        }
    }

    static final String EXTRA_PERMISSIONS = "intent.extra.PERMISSIONS";

    @VisibleForTesting
    static final int REQUEST_CODE = PermissionRequestActivity.class.getName().hashCode();

    /**
     * Tracking last request to send result to the listener.
     */
    @VisibleForTesting
    static DefaultAppCenterFuture<Result> sResultFuture;

    public static AppCenterFuture<Result> requestPermissions(Context context, String... permissions) {
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

    private static void complete(Result result) {
        if (sResultFuture != null) {
            sResultFuture.complete(result);
            sResultFuture = null;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            AppCenterLog.error(LOG_TAG, "Android version incompatible.");
            complete(new Result(false, null));
            finish();
            return;
        }

        final String[] permissions;
        try {
            permissions = getIntent().getExtras().getStringArray(EXTRA_PERMISSIONS);
        } catch (Exception e) {
            AppCenterLog.error(LOG_TAG, "Error while getting permissions list.", e);
            complete(new Result(false, e));
            finish();
            return;
        }
        requestPermissions(permissions, REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            boolean isGranted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            complete(new Result(isGranted, null));
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