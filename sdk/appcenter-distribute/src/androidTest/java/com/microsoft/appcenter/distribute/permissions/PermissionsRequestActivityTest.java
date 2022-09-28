package com.microsoft.appcenter.distribute.permissions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.Context;
import android.content.Intent;

import androidx.test.platform.app.InstrumentationRegistry;

import com.microsoft.appcenter.utils.async.AppCenterConsumer;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;

import org.junit.Test;

public class PermissionsRequestActivityTest {

    @Test
    public void oneTimePermissionsRequest() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        AppCenterFuture<PermissionRequestActivity.Result> future = PermissionUtils.requestPermissions(context, Manifest.permission.POST_NOTIFICATIONS);
        future.thenAccept(new AppCenterConsumer<PermissionRequestActivity.Result>() {
            @Override
            public void accept(PermissionRequestActivity.Result result) {
                assertTrue(result.isPermissionGranted);
                assertNull(result.exception);
            }
        });
        AppCenterFuture<PermissionRequestActivity.Result> nullFuture = PermissionUtils.requestPermissions(context, Manifest.permission.POST_NOTIFICATIONS);
        assertNull(nullFuture);
    }

    @Test
    public void requestPermissionsWithoutIntentExtras() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();

        /* Prepare intent without extras. */
        Intent intent = new Intent(context, PermissionRequestActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        /* Setup future. */
        PermissionRequestActivity.sResultFuture = new DefaultAppCenterFuture<>();
        PermissionRequestActivity.sResultFuture.thenAccept(new AppCenterConsumer<PermissionRequestActivity.Result>() {
            @Override
            public void accept(PermissionRequestActivity.Result result) {
                assertFalse(result.isPermissionGranted);
                assertNotNull(result.exception);
            }
        });

        /* Start activity. */
        context.startActivity(intent);
    }
}
