package com.microsoft.appcenter.distribute.permissions;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.Manifest;
import android.content.Context;

import com.microsoft.appcenter.utils.async.AppCenterFuture;

import org.junit.Test;

public class PermissionsUtilsTest {

    @Test
    public void requestPermissions() {
        Context contextMock = mock(Context.class);

        {
            /* Invoke with start activity */
            AppCenterFuture<PermissionRequestActivity.Result> future = PermissionUtils.requestPermissions(contextMock, Manifest.permission.POST_NOTIFICATIONS);
            verify(contextMock).startActivity(any());
            assertNotNull(future);
        }

        {
            /* Invoke with return null */
            AppCenterFuture<PermissionRequestActivity.Result> future = PermissionUtils.requestPermissions(contextMock, Manifest.permission.POST_NOTIFICATIONS);
            assertNull(future);
        }
    }
}
