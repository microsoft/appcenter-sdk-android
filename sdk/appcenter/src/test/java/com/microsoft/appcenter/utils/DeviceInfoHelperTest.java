package com.microsoft.appcenter.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.BuildConfig;
import com.microsoft.appcenter.ingestion.models.Device;
import com.microsoft.appcenter.ingestion.models.WrapperSdk;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest({Build.class, AppCenterLog.class, TextUtils.class})
public class DeviceInfoHelperTest {

    @Before
    @After
    public void cleanWrapperSdk() {
        DeviceInfoHelper.setWrapperSdk(null);
    }

    @Test
    public void getDeviceInfo() throws PackageManager.NameNotFoundException, DeviceInfoHelper.DeviceInfoException {

        /* Mock data. */
        final String appVersion = "1.0";
        final String appBuild = "1";
        //noinspection SpellCheckingInspection
        final String appNamespace = "com.contoso.app";
        final String carrierCountry = "us";
        final String carrierName = "mock-service";
        final Locale locale = Locale.KOREA;
        final String model = "mock-model";
        final String oemName = "mock-manufacture";
        final Integer osApiLevel = 23;
        final String osName = "Android";
        final String osVersion = "mock-version";
        final String osBuild = "mock-os-build";
        final String screenSizeLandscape = "100x200";
        final String screenSizePortrait = "200x100";
        final TimeZone timeZone = TimeZone.getTimeZone("KST");
        final Integer timeZoneOffset = timeZone.getOffset(System.currentTimeMillis());

        Locale.setDefault(locale);
        TimeZone.setDefault(timeZone);

        /* Mocking instances. */
        Context contextMock = mock(Context.class);
        PackageManager packageManagerMock = mock(PackageManager.class);
        PackageInfo packageInfoMock = mock(PackageInfo.class);
        WindowManager windowManagerMock = mock(WindowManager.class);
        TelephonyManager telephonyManagerMock = mock(TelephonyManager.class);
        Display displayMock = mock(Display.class);

        /* Delegates to mock instances. */
        when(contextMock.getPackageName()).thenReturn(appNamespace);
        when(contextMock.getPackageManager()).thenReturn(packageManagerMock);
        //noinspection WrongConstant
        when(contextMock.getSystemService(eq(Context.TELEPHONY_SERVICE))).thenReturn(telephonyManagerMock);
        //noinspection WrongConstant
        when(contextMock.getSystemService(eq(Context.WINDOW_SERVICE))).thenReturn(windowManagerMock);
        //noinspection WrongConstant
        when(packageManagerMock.getPackageInfo(anyString(), eq(0))).thenReturn(packageInfoMock);
        when(telephonyManagerMock.getNetworkCountryIso()).thenReturn(carrierCountry);
        when(telephonyManagerMock.getNetworkOperatorName()).thenReturn(carrierName);
        when(windowManagerMock.getDefaultDisplay()).thenReturn(displayMock);
        when(displayMock.getRotation()).thenReturn(Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) {
                Object[] args = invocationOnMock.getArguments();
                /* DO NOT call set method and assign values directly to variables. */
                ((Point) args[0]).x = 100;
                ((Point) args[0]).y = 200;
                return null;
            }
        }).when(displayMock).getSize(any(Point.class));

        /* Sets values of fields for static classes. */
        Whitebox.setInternalState(packageInfoMock, "versionName", appVersion);
        Whitebox.setInternalState(packageInfoMock, "versionCode", Integer.parseInt(appBuild));
        Whitebox.setInternalState(Build.class, "MODEL", model);
        Whitebox.setInternalState(Build.class, "MANUFACTURER", oemName);
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", osApiLevel);
        Whitebox.setInternalState(Build.class, "ID", osBuild);
        Whitebox.setInternalState(Build.VERSION.class, "RELEASE", osVersion);

        /* First call */
        Device device = DeviceInfoHelper.getDeviceInfo(contextMock);

        /* Verify device information. */
        assertNull(device.getWrapperSdkName());
        assertNull(device.getWrapperSdkVersion());
        assertEquals(BuildConfig.VERSION_NAME, device.getSdkVersion());
        assertEquals(appVersion, device.getAppVersion());
        assertEquals(appBuild, device.getAppBuild());
        assertEquals(appNamespace, device.getAppNamespace());
        assertEquals(carrierCountry, device.getCarrierCountry());
        assertEquals(carrierName, device.getCarrierName());
        assertEquals(locale.toString(), device.getLocale());
        assertEquals(model, device.getModel());
        assertEquals(oemName, device.getOemName());
        assertEquals(osApiLevel, device.getOsApiLevel());
        assertEquals(osName, device.getOsName());
        assertEquals(osVersion, device.getOsVersion());
        assertEquals(osBuild, device.getOsBuild());
        assertEquals(screenSizeLandscape, device.getScreenSize());
        assertEquals(timeZoneOffset, device.getTimeZoneOffset());

        /* Verify screen size based on different orientations (Surface.ROTATION_90). */
        device = DeviceInfoHelper.getDeviceInfo(contextMock);
        assertEquals(screenSizePortrait, device.getScreenSize());

        /* Verify screen size based on different orientations (Surface.ROTATION_180). */
        device = DeviceInfoHelper.getDeviceInfo(contextMock);
        assertEquals(screenSizeLandscape, device.getScreenSize());

        /* Verify screen size based on different orientations (Surface.ROTATION_270). */
        device = DeviceInfoHelper.getDeviceInfo(contextMock);
        assertEquals(screenSizePortrait, device.getScreenSize());

        /* Make sure screen size is verified for all orientations. */
        verify(displayMock, times(4)).getRotation();

        /* Set wrapper sdk information. */
        WrapperSdk wrapperSdk = new WrapperSdk();
        wrapperSdk.setWrapperSdkVersion("1.2.3.4");
        wrapperSdk.setWrapperSdkName("ReactNative");
        wrapperSdk.setWrapperRuntimeVersion("4.13");
        wrapperSdk.setLiveUpdateReleaseLabel("2.0.3-beta2");
        wrapperSdk.setLiveUpdateDeploymentKey("staging");
        wrapperSdk.setLiveUpdatePackageHash("aa896f791b26a7f464c0f62b0ba69f2b");
        DeviceInfoHelper.setWrapperSdk(wrapperSdk);
        Device device2 = DeviceInfoHelper.getDeviceInfo(contextMock);
        assertEquals(wrapperSdk.getWrapperSdkVersion(), device2.getWrapperSdkVersion());
        assertEquals(wrapperSdk.getWrapperSdkName(), device2.getWrapperSdkName());
        assertEquals(wrapperSdk.getWrapperRuntimeVersion(), device2.getWrapperRuntimeVersion());
        assertEquals(wrapperSdk.getLiveUpdateReleaseLabel(), device2.getLiveUpdateReleaseLabel());
        assertEquals(wrapperSdk.getLiveUpdateDeploymentKey(), device2.getLiveUpdateDeploymentKey());
        assertEquals(wrapperSdk.getLiveUpdatePackageHash(), device2.getLiveUpdatePackageHash());

        /* Check non wrapped sdk information are still generated correctly. */
        device2.setWrapperSdkVersion(null);
        device2.setWrapperSdkName(null);
        device2.setWrapperRuntimeVersion(null);
        device2.setLiveUpdateReleaseLabel(null);
        device2.setLiveUpdateDeploymentKey(null);
        device2.setLiveUpdatePackageHash(null);
        assertEquals(device, device2);

        /* Remove wrapper SDK information. */
        DeviceInfoHelper.setWrapperSdk(null);
        assertEquals(device, DeviceInfoHelper.getDeviceInfo(contextMock));
    }

    @Test(expected = DeviceInfoHelper.DeviceInfoException.class)
    public void getDeviceInfoWithException() throws PackageManager.NameNotFoundException, DeviceInfoHelper.DeviceInfoException {

        /* Mocking instances. */
        Context contextMock = mock(Context.class);
        PackageManager packageManagerMock = mock(PackageManager.class);

        /* Delegates to mock instances. */
        when(contextMock.getPackageManager()).thenReturn(packageManagerMock);
        //noinspection WrongConstant
        when(packageManagerMock.getPackageInfo(anyString(), eq(0))).thenThrow(new PackageManager.NameNotFoundException());

        DeviceInfoHelper.getDeviceInfo(contextMock);
    }

    @Test
    public void getDeviceInfoMissingCarrierInfo() throws DeviceInfoHelper.DeviceInfoException, PackageManager.NameNotFoundException {

        /* Mocking instances. */
        Context contextMock = mock(Context.class);
        PackageManager packageManagerMock = mock(PackageManager.class);
        WindowManager windowManagerMock = mock(WindowManager.class);
        mockStatic(AppCenterLog.class);

        /* Delegates to mock instances. */
        when(contextMock.getPackageManager()).thenReturn(packageManagerMock);
        when(contextMock.getSystemService(Context.TELEPHONY_SERVICE)).thenThrow(new RuntimeException());
        when(contextMock.getSystemService(Context.WINDOW_SERVICE)).thenReturn(windowManagerMock);
        //noinspection WrongConstant
        when(packageManagerMock.getPackageInfo(anyString(), anyInt())).thenReturn(mock(PackageInfo.class));
        when(windowManagerMock.getDefaultDisplay()).thenReturn(mock(Display.class));

        /* Verify. */
        Device device = DeviceInfoHelper.getDeviceInfo(contextMock);
        assertNull(device.getCarrierCountry());
        assertNull(device.getCarrierName());
        verifyStatic();
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(Exception.class));
    }

    @Test
    public void getDeviceInfoEmptyCarrierInfo() throws DeviceInfoHelper.DeviceInfoException, PackageManager.NameNotFoundException {

        /* Mocking instances. */
        Context contextMock = mock(Context.class);
        PackageManager packageManagerMock = mock(PackageManager.class);

        /* Delegates to mock instances. */
        when(contextMock.getPackageManager()).thenReturn(packageManagerMock);
        //noinspection WrongConstant
        when(packageManagerMock.getPackageInfo(anyString(), anyInt())).thenReturn(mock(PackageInfo.class));
        TelephonyManager telephonyManager = mock(TelephonyManager.class);
        when(telephonyManager.getNetworkCountryIso()).thenReturn("");
        when(telephonyManager.getNetworkOperatorName()).thenReturn("");
        when(contextMock.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(telephonyManager);
        mockStatic(TextUtils.class);
        when(TextUtils.isEmpty(anyString())).thenReturn(true);

        /* Verify. */
        Device device = DeviceInfoHelper.getDeviceInfo(contextMock);
        assertNull(device.getCarrierCountry());
        assertNull(device.getCarrierName());
    }

    @Test
    public void getDeviceInfoMissingScreenSize() throws DeviceInfoHelper.DeviceInfoException, PackageManager.NameNotFoundException {

        /* Mocking instances. */
        Context contextMock = mock(Context.class);
        PackageManager packageManagerMock = mock(PackageManager.class);
        mockStatic(AppCenterLog.class);

        /* Delegates to mock instances. */
        when(contextMock.getPackageManager()).thenReturn(packageManagerMock);
        when(contextMock.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mock(TelephonyManager.class));
        when(contextMock.getSystemService(Context.WINDOW_SERVICE)).thenThrow(new RuntimeException());
        //noinspection WrongConstant
        when(packageManagerMock.getPackageInfo(anyString(), anyInt())).thenReturn(mock(PackageInfo.class));

        /* Verify. */
        Device device = DeviceInfoHelper.getDeviceInfo(contextMock);
        assertNull(device.getScreenSize());
        verifyStatic();
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(Exception.class));
    }
}