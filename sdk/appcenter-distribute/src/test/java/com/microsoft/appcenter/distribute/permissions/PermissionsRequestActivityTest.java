package com.microsoft.appcenter.distribute.permissions;


import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.permissions.PermissionRequestActivity.REQUEST_CODE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.microsoft.appcenter.distribute.AbstractDistributeTest;
import com.microsoft.appcenter.utils.AppCenterLog;

import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

@PrepareForTest({
        AppCenterLog.class,
        PermissionRequestActivity.class,
        Build.VERSION.class
})
public class PermissionsRequestActivityTest extends AbstractDistributeTest {

    @Test
    public void onRequestPermissionsResult() {
        mockStatic(PermissionRequestActivity.class);
        PermissionRequestActivity permissionRequestActivity = new PermissionRequestActivity();

        /* With right request code. */
        permissionRequestActivity.onRequestPermissionsResult(REQUEST_CODE, new String[0], new int[]{PackageManager.PERMISSION_GRANTED});
        verifyStatic(PermissionRequestActivity.class);
        PermissionRequestActivity.complete(any(PermissionRequestActivity.Result.class));

        /* With right request code, but not granted. */
        permissionRequestActivity.onRequestPermissionsResult(REQUEST_CODE, new String[0], new int[0]);
        verifyStatic(PermissionRequestActivity.class, times(2));
        PermissionRequestActivity.complete(any(PermissionRequestActivity.Result.class));

        /* With different request code. */
        permissionRequestActivity.onRequestPermissionsResult(0, new String[0], new int[]{PackageManager.PERMISSION_GRANTED});
        verifyStatic(PermissionRequestActivity.class, times(2));
        PermissionRequestActivity.complete(any(PermissionRequestActivity.Result.class));
    }

    @Test
    public void completeWithNullFuture() {
        mockStatic(AppCenterLog.class);
        PermissionRequestActivity.sResultFuture = null;
        PermissionRequestActivity.complete(mock(PermissionRequestActivity.Result.class));
        verifyStatic(AppCenterLog.class);
        AppCenterLog.debug(eq(LOG_TAG), anyString());
    }

    @Test
    public void onCreate() {
        mockStatic(AppCenterLog.class);
        mockStatic(PermissionRequestActivity.class);
        PermissionRequestActivity permissionRequestActivity = new PermissionRequestActivity();

        /* With build version lower then "M". */
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.M - 1);
        permissionRequestActivity.onCreate(new Bundle());
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(LOG_TAG), anyString(), any(Exception.class));
        verifyStatic(PermissionRequestActivity.class);
        PermissionRequestActivity.complete(any(PermissionRequestActivity.Result.class));

        /* With "M" build version. */
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.M);
        permissionRequestActivity.onCreate(new Bundle());
        verifyStatic(AppCenterLog.class, times(2));
        AppCenterLog.error(eq(LOG_TAG), anyString(), any(Exception.class));
        verifyStatic(PermissionRequestActivity.class, times(2));
        PermissionRequestActivity.complete(any(PermissionRequestActivity.Result.class));
    }
}
