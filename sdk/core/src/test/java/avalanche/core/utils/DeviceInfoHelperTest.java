package avalanche.core.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.Locale;
import java.util.TimeZone;

import avalanche.core.BuildConfig;
import avalanche.core.ingestion.models.Device;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest(Build.class)
public class DeviceInfoHelperTest {

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
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
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
        Device log = DeviceInfoHelper.getDeviceInfo(contextMock);

        /* Verify device information. */
        assertEquals(BuildConfig.VERSION_NAME, log.getSdkVersion());
        assertEquals(appVersion, log.getAppVersion());
        assertEquals(appBuild, log.getAppBuild());
        assertEquals(appNamespace, log.getAppNamespace());
        assertEquals(carrierCountry, log.getCarrierCountry());
        assertEquals(carrierName, log.getCarrierName());
        assertEquals(locale.toString(), log.getLocale());
        assertEquals(model, log.getModel());
        assertEquals(oemName, log.getOemName());
        assertEquals(osApiLevel, log.getOsApiLevel());
        assertEquals(osName, log.getOsName());
        assertEquals(osVersion, log.getOsVersion());
        assertEquals(osBuild, log.getOsBuild());
        assertEquals(screenSizeLandscape, log.getScreenSize());
        assertEquals(timeZoneOffset, log.getTimeZoneOffset());

        /* Verify screen size based on different orientations (Surface.ROTATION_90). */
        log = DeviceInfoHelper.getDeviceInfo(contextMock);
        assertEquals(screenSizePortrait, log.getScreenSize());

        /* Verify screen size based on different orientations (Surface.ROTATION_180). */
        log = DeviceInfoHelper.getDeviceInfo(contextMock);
        assertEquals(screenSizeLandscape, log.getScreenSize());

        /* Verify screen size based on different orientations (Surface.ROTATION_270). */
        log = DeviceInfoHelper.getDeviceInfo(contextMock);
        assertEquals(screenSizePortrait, log.getScreenSize());

        /* Make sure screen size is verified for all orientations. */
        verify(displayMock, times(4)).getRotation();
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
}