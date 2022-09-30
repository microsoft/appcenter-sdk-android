/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.permissions;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.permissions.PermissionRequestActivity.EXTRA_PERMISSIONS;
import static com.microsoft.appcenter.distribute.permissions.PermissionRequestActivity.REQUEST_CODE;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.powermock.reflect.Whitebox;

@PrepareForTest({
        AppCenterLog.class,
        PermissionRequestActivity.class
})
public class PermissionsRequestActivityTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    public Intent mIntent;

    private PermissionRequestActivity mPermissionRequestActivity;

    private void verifyCompleteWithIllegalArgumentException() {
        PermissionRequestActivity.complete(ArgumentMatchers.argThat(new ArgumentMatcher<PermissionRequestActivity.Result>() {
            @Override
            public boolean matches(PermissionRequestActivity.Result argument) {
                return argument.exception instanceof IllegalArgumentException &&
                        !argument.isAllPermissionsGranted() &&
                        argument.permissionRequestResults == null;
            }
        }));
    }

    private void verifyCompleteWithNotGrantedPermissions() {
        PermissionRequestActivity.complete(ArgumentMatchers.argThat(new ArgumentMatcher<PermissionRequestActivity.Result>() {
            @Override
            public boolean matches(PermissionRequestActivity.Result argument) {
                return argument.exception == null &&
                        !argument.isAllPermissionsGranted() &&
                        argument.permissionRequestResults != null &&
                        Boolean.FALSE.equals(argument.permissionRequestResults.get(Manifest.permission.POST_NOTIFICATIONS));
            }
        }));
    }

    @Before
    public void setUp() {
        PermissionRequestActivity.sResultFuture = null;
        mPermissionRequestActivity = spy(new PermissionRequestActivity());
    }

    @Test
    public void onRequestPermissionsResultGranted() {
        mockStatic(PermissionRequestActivity.class);
        mPermissionRequestActivity.onRequestPermissionsResult(REQUEST_CODE, new String[]{Manifest.permission.POST_NOTIFICATIONS}, new int[]{PackageManager.PERMISSION_GRANTED});
        verifyStatic(PermissionRequestActivity.class);
        PermissionRequestActivity.complete(ArgumentMatchers.argThat(new ArgumentMatcher<PermissionRequestActivity.Result>() {
            @Override
            public boolean matches(PermissionRequestActivity.Result argument) {
                return argument.exception == null &&
                        argument.isAllPermissionsGranted() &&
                        argument.permissionRequestResults != null &&
                        Boolean.TRUE.equals(argument.permissionRequestResults.get(Manifest.permission.POST_NOTIFICATIONS));
            }
        }));
    }

    @Test
    public void onRequestPermissionsResultNotGranted() {
        mockStatic(PermissionRequestActivity.class);
        mPermissionRequestActivity.onRequestPermissionsResult(REQUEST_CODE, new String[]{Manifest.permission.POST_NOTIFICATIONS}, new int[]{PackageManager.PERMISSION_DENIED});
        verifyStatic(PermissionRequestActivity.class);
        verifyCompleteWithNotGrantedPermissions();
    }

    @Test
    public void onRequestPermissionsResultNotGrantedWithEmptyPermissionsList() {
        mockStatic(PermissionRequestActivity.class);
        mPermissionRequestActivity.onRequestPermissionsResult(REQUEST_CODE, new String[0], new int[]{PackageManager.PERMISSION_DENIED});
        verifyStatic(PermissionRequestActivity.class);
        PermissionRequestActivity.complete(ArgumentMatchers.argThat(new ArgumentMatcher<PermissionRequestActivity.Result>() {
            @Override
            public boolean matches(PermissionRequestActivity.Result argument) {
                return argument.exception == null &&
                        !argument.isAllPermissionsGranted() &&
                        argument.permissionRequestResults != null &&
                        argument.permissionRequestResults.size() == 0;
            }
        }));
    }

    @Test
    public void onRequestPermissionsResultWithoutResponse() {
        mockStatic(PermissionRequestActivity.class);
        mPermissionRequestActivity.onRequestPermissionsResult(REQUEST_CODE, new String[]{Manifest.permission.POST_NOTIFICATIONS}, new int[0]);
        verifyStatic(PermissionRequestActivity.class);
        verifyCompleteWithNotGrantedPermissions();
    }

    @Test
    public void onRequestPermissionsResultWithDifferentRequestCode() {
        mockStatic(PermissionRequestActivity.class);
        mPermissionRequestActivity.onRequestPermissionsResult(0, new String[]{Manifest.permission.POST_NOTIFICATIONS}, new int[]{PackageManager.PERMISSION_GRANTED});
        verifyStatic(PermissionRequestActivity.class, never());
        PermissionRequestActivity.complete(any(PermissionRequestActivity.Result.class));
    }

    @Test
    public void completeWithNullFuture() {
        mockStatic(AppCenterLog.class);
        PermissionRequestActivity.complete(mock(PermissionRequestActivity.Result.class));
        verifyStatic(AppCenterLog.class);
        AppCenterLog.debug(eq(LOG_TAG), anyString());
    }

    @Test
    public void complete() {
        DefaultAppCenterFuture<PermissionRequestActivity.Result> futureMock = mock(DefaultAppCenterFuture.class);
        PermissionRequestActivity.Result resultMock = mock(PermissionRequestActivity.Result.class);
        PermissionRequestActivity.sResultFuture = futureMock;
        PermissionRequestActivity.complete(resultMock);
        verify(futureMock).complete(resultMock);
        assertNull(PermissionRequestActivity.sResultFuture);
    }

    @Test
    public void onCreateWithBuildVersionLowerThenM() {
        mockStatic(AppCenterLog.class);
        mockStatic(PermissionRequestActivity.class);

        /* With build version lower then "Marshmallow" (6.0). */
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.LOLLIPOP_MR1);
        mPermissionRequestActivity.onCreate(null);
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(LOG_TAG), anyString(), any(Exception.class));
        verifyStatic(PermissionRequestActivity.class);
        PermissionRequestActivity.complete(ArgumentMatchers.argThat(new ArgumentMatcher<PermissionRequestActivity.Result>() {
            @Override
            public boolean matches(PermissionRequestActivity.Result argument) {
                return argument.exception instanceof UnsupportedOperationException &&
                        !argument.isAllPermissionsGranted() &&
                        argument.permissionRequestResults == null;
            }
        }));
    }

    @Test
    public void onCreateButIntentIsNull() {
        mockStatic(AppCenterLog.class);
        mockStatic(PermissionRequestActivity.class);

        /* With "Marshmallow" (6.0) build version. */
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.M);
        mPermissionRequestActivity.onCreate(null);
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(LOG_TAG), anyString(), any(Exception.class));
        verifyStatic(PermissionRequestActivity.class);
        verifyCompleteWithIllegalArgumentException();

    }

    @Test
    public void onCreateButExtrasIsNull() {
        mockStatic(AppCenterLog.class);
        mockStatic(PermissionRequestActivity.class);

        /* Setup getIntent() for mPermissionRequestActivity. */

        when(mPermissionRequestActivity.getIntent()).thenReturn(mIntent);

        /* With "Marshmallow" (6.0) build version. */
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.M);
        mPermissionRequestActivity.onCreate(null);
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(LOG_TAG), anyString(), any(Exception.class));
        verifyStatic(PermissionRequestActivity.class);
        verifyCompleteWithIllegalArgumentException();
    }

    @Test
    public void onCreate() {
        mockStatic(AppCenterLog.class);
        mockStatic(PermissionRequestActivity.class);

        /* Setup getIntent() for mPermissionRequestActivity. */
        Bundle bundleMock = mock(Bundle.class);
        when(mPermissionRequestActivity.getIntent()).thenReturn(mIntent);
        when(mIntent.getExtras()).thenReturn(bundleMock);
        String[] permissionsForRequest = {Manifest.permission.POST_NOTIFICATIONS};
        when(bundleMock.getStringArray(eq(EXTRA_PERMISSIONS))).thenReturn(permissionsForRequest);

        /* With "Marshmallow" (6.0) build version. */
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.M);
        mPermissionRequestActivity.onCreate(null);
        verify(mPermissionRequestActivity).requestPermissions(eq(permissionsForRequest), anyInt());
    }
}
