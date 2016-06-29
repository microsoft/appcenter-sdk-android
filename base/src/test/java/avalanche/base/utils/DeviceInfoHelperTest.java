package avalanche.base.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
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

import avalanche.base.ingestion.models.DeviceLog;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Build.class, Locale.class, TimeZone.class, DeviceInfoHelper.class})
public class DeviceInfoHelperTest {

    /**
     * Log tag.
     */
    private static final String TAG = "DeviceInfoHelperTest";

    @Test
    @SuppressWarnings("WrongConstant")
    public void getDeviceInfo() throws PackageManager.NameNotFoundException, DeviceInfoHelper.DeviceInfoException {
        Log.i(TAG, "Testing device info");

        /* Mock data. */
        final String appVersion = "1.0";
        final String carrierCountry = "us";
        final String carrierName = "mock-service";
        final Locale locale = Locale.KOREA;
        final String model = "mock-model";
        final String oemName = "mock-manufacture";
        final Integer osApiLevel = 23;
        final String osName = "MOC64";
        final String osVersion = "mock-version";
        final String screenSizeLandscape = "100x200";
        final String screenSizePortrait = "200x100";
        final Integer timeZoneOffset = -300;
        final int MIN_IN_MILLI = 60 * 1000;

        /* Mocking instances. */
        Context contextMock = mock(Context.class);
        PackageManager packageManagerMock = mock(PackageManager.class);
        PackageInfo packageInfoMock = mock(PackageInfo.class);
        WindowManager windowManagerMock = mock(WindowManager.class);
        TelephonyManager telephonyManagerMock = mock(TelephonyManager.class);
        Display displayMock = mock(Display.class);
        TimeZone timeZoneMock = mock(TimeZone.class);

        /* Mocking static classes. */
        mockStatic(Locale.class);
        mockStatic(TimeZone.class);

        /* Delegates to mock instances. */
        when(contextMock.getPackageManager()).thenReturn(packageManagerMock);
        when(contextMock.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(telephonyManagerMock);
        when(contextMock.getSystemService(Context.WINDOW_SERVICE)).thenReturn(windowManagerMock);
        when(packageManagerMock.getPackageInfo(anyString(), eq(0))).thenReturn(packageInfoMock);
        when(telephonyManagerMock.getNetworkCountryIso()).thenReturn(carrierCountry);
        when(telephonyManagerMock.getNetworkOperatorName()).thenReturn(carrierName);
        when(windowManagerMock.getDefaultDisplay()).thenReturn(displayMock);
        when(displayMock.getRotation()).thenReturn(Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270);
        when(Locale.getDefault()).thenReturn(locale);
        when(TimeZone.getDefault()).thenReturn(timeZoneMock);
        when(timeZoneMock.getOffset(anyLong())).thenReturn(timeZoneOffset * MIN_IN_MILLI);
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
        Whitebox.setInternalState(Build.class, "MODEL", model);
        Whitebox.setInternalState(Build.class, "MANUFACTURER", oemName);
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", osApiLevel);
        Whitebox.setInternalState(Build.class, "ID", osName);
        Whitebox.setInternalState(Build.VERSION.class, "RELEASE", osVersion);

        /* TODO: Implement mock for BuildConfig.VERSION_NAME and verify getSdkVersion(). Need a special way to do this since BuildConfig is a final class. */

        /* First call */
        DeviceLog log = DeviceInfoHelper.getDeviceLog(contextMock);

        /* Verify device information. */
        assertEquals(appVersion, log.getAppVersion());
        assertEquals(carrierCountry, log.getCarrierCountry());
        assertEquals(carrierName, log.getCarrierName());
        assertEquals(locale.toString(), log.getLocale());
        assertEquals(model, log.getModel());
        assertEquals(oemName, log.getOemName());
        assertEquals(osApiLevel, log.getOsApiLevel());
        assertEquals(osName, log.getOsName());
        assertEquals(osVersion, log.getOsVersion());
        assertEquals(screenSizeLandscape, log.getScreenSize());
        assertEquals(timeZoneOffset, log.getTimeZoneOffset());

        /* Verify screen size based on different orientations (Surface.ROTATION_90). */
        log = DeviceInfoHelper.getDeviceLog(contextMock);
        assertEquals(screenSizePortrait, log.getScreenSize());

        /* Verify screen size based on different orientations (Surface.ROTATION_180). */
        log = DeviceInfoHelper.getDeviceLog(contextMock);
        assertEquals(screenSizeLandscape, log.getScreenSize());

        /* Verify screen size based on different orientations (Surface.ROTATION_270). */
        log = DeviceInfoHelper.getDeviceLog(contextMock);
        assertEquals(screenSizePortrait, log.getScreenSize());

        /* Make sure screen size is verified for all orientations. */
        verify(displayMock, times(4)).getRotation();
    }

    @Test(expected = DeviceInfoHelper.DeviceInfoException.class)
    @SuppressWarnings("WrongConstant")
    public void getDeviceInfoWithException() throws PackageManager.NameNotFoundException, DeviceInfoHelper.DeviceInfoException {
        Log.i(TAG, "Testing device info with exception");

        /* Mocking instances. */
        Context contextMock = mock(Context.class);
        PackageManager packageManagerMock = mock(PackageManager.class);

        /* Delegates to mock instances. */
        when(contextMock.getPackageManager()).thenReturn(packageManagerMock);
        when(packageManagerMock.getPackageInfo(anyString(), eq(0))).thenThrow(new PackageManager.NameNotFoundException());

        DeviceInfoHelper.getDeviceLog(contextMock);
    }
}