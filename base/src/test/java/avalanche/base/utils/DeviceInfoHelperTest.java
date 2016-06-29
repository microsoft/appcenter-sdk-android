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
    public void getDeviceInfo() throws PackageManager.NameNotFoundException {
        Log.i(TAG, "Testing device info");

        /* Mock data. */
        final String versionName = "1.0";
        final String networkCountryIso = "us";
        final String networkOperatorName = "mock-service";
        final Locale defaultLocale = Locale.KOREA;
        final String model = "mock-model";
        final String manufacture = "mock-manufacture";
        final Integer sdkVersion = 23;
        final String id = "MOC64";
        final String release = "mock-version";
        final String screenSizeLandscape = "100x200";
        final String screenSizePortrait = "200x100";
        final Integer tzOffset = -300;
        final int MIN_IN_MILLI = 60 * 1000;

        /* Mocking instances. */
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        PackageInfo packageInfo = mock(PackageInfo.class);
        WindowManager windowManager = mock(WindowManager.class);
        TelephonyManager telephonyManager = mock(TelephonyManager.class);
        Display display = mock(Display.class);
        TimeZone timeZone = mock(TimeZone.class);

        /* Mocking static classes. */
        mockStatic(Locale.class);
        mockStatic(TimeZone.class);

        /* Delegates to mock instances. */
        when(context.getPackageManager()).thenReturn(packageManager);
        when(context.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(telephonyManager);
        when(context.getSystemService(Context.WINDOW_SERVICE)).thenReturn(windowManager);
        when(packageManager.getPackageInfo(anyString(), eq(0))).thenReturn(packageInfo);
        when(telephonyManager.getNetworkCountryIso()).thenReturn(networkCountryIso);
        when(telephonyManager.getNetworkOperatorName()).thenReturn(networkOperatorName);
        when(windowManager.getDefaultDisplay()).thenReturn(display);
        when(display.getRotation()).thenReturn(Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270);
        when(Locale.getDefault()).thenReturn(defaultLocale);
        when(TimeZone.getDefault()).thenReturn(timeZone);
        when(timeZone.getOffset(anyLong())).thenReturn(tzOffset * MIN_IN_MILLI);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                /* DO NOT call set method and assign values directly to variables. */
                ((Point) args[0]).x = 100;
                ((Point) args[0]).y = 200;
                return null;
            }
        }).when(display).getSize(any(Point.class));

        /* Sets values of fields for static classes. */
        Whitebox.setInternalState(packageInfo, "versionName", versionName);
        Whitebox.setInternalState(Build.class, "MODEL", model);
        Whitebox.setInternalState(Build.class, "MANUFACTURER", manufacture);
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", sdkVersion);
        Whitebox.setInternalState(Build.class, "ID", id);
        Whitebox.setInternalState(Build.VERSION.class, "RELEASE", release);

        /* First call */
        DeviceLog log = DeviceInfoHelper.getDeviceLog(context);

        /* Verify device information. */
        assertEquals(versionName, log.getAppVersion());
        assertEquals(networkCountryIso, log.getCarrierCountry());
        assertEquals(networkOperatorName, log.getCarrierName());
        assertEquals(defaultLocale.toString(), log.getLocale());
        assertEquals(model, log.getModel());
        assertEquals(manufacture, log.getOemName());
        assertEquals(sdkVersion, log.getOsApiLevel());
        assertEquals(id, log.getOsName());
        assertEquals(release, log.getOsVersion());
        assertEquals(screenSizeLandscape, log.getScreenSize());
        assertEquals(tzOffset, log.getTimeZoneOffset());

        /* Verify screen size based on different orientations (Surface.ROTATION_90). */
        log = DeviceInfoHelper.getDeviceLog(context);
        assertEquals(screenSizePortrait, log.getScreenSize());

        /* Verify screen size based on different orientations (Surface.ROTATION_180). */
        log = DeviceInfoHelper.getDeviceLog(context);
        assertEquals(screenSizeLandscape, log.getScreenSize());

        /* Verify screen size based on different orientations (Surface.ROTATION_270). */
        log = DeviceInfoHelper.getDeviceLog(context);
        assertEquals(screenSizePortrait, log.getScreenSize());

        /* Make sure screen size is verified for all orientations. */
        verify(display, times(4)).getRotation();
    }
}