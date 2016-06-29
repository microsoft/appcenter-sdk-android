package avalanche.base.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.util.Locale;
import java.util.TimeZone;

import avalanche.base.BuildConfig;
import avalanche.base.ingestion.models.DeviceLog;

/**
 * DeviceInfoHelper class to retrieve device information.
 */
public class DeviceInfoHelper {
    /**
     * Gets device information.
     *
     * @param context The context of the application.
     * @return {@link DeviceLog}
     */
    public static DeviceLog getDeviceLog(Context context) {
        DeviceLog deviceLog = new DeviceLog();

        /* Application version. */
        PackageInfo packageInfo;
        try {
            PackageManager packageManager = context.getPackageManager();
            packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            deviceLog.setAppVersion(packageInfo.versionName);
        } catch (Exception e) {
            AvalancheLog.error("Cannot retrieve package info", e);
        }

        /* Carrier info. */
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            deviceLog.setCarrierCountry(telephonyManager.getNetworkCountryIso());
            deviceLog.setCarrierName(telephonyManager.getNetworkOperatorName());
        } catch (Exception e) {
            AvalancheLog.error("Cannot retrieve carrier info", e);
        }

        /* Locale. */
        deviceLog.setLocale(Locale.getDefault().toString());

        /* Hardware info. */
        deviceLog.setModel(Build.MODEL);
        deviceLog.setOemName(Build.MANUFACTURER);

        /* OS version. */
        deviceLog.setOsApiLevel(Build.VERSION.SDK_INT);
        deviceLog.setOsName(Build.ID);
        deviceLog.setOsVersion(Build.VERSION.RELEASE);

        /* Screen size. */
        try {
            deviceLog.setScreenSize(getScreenSize(context));
        } catch (Exception e) {
            AvalancheLog.error("Cannot retrieve screen size", e);
        }

        /* SDK version. */
        deviceLog.setSdkVersion(BuildConfig.VERSION_NAME);

        /* Timezone offset in minutes (including DST). */
        deviceLog.setTimeZoneOffset(TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60 / 1000);

        return deviceLog;
    }

    /**
     * Gets a size of a device for base orientation.
     *
     * @param context The context of the application.
     * @return A string with {@code <width>x<height>} format.
     */
    private static String getScreenSize(Context context) {
        /* Guess resolution based on the natural device orientation */
        int screenWidth;
        int screenHeight;
        Display defaultDisplay = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        Point size = new Point();
        defaultDisplay.getSize(size);
        switch (defaultDisplay.getRotation()) {
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                screenHeight = size.x;
                screenWidth = size.y;
                break;
            default:
                screenWidth = size.x;
                screenHeight = size.y;
        }

         /* Serialize screen resolution */
        return screenWidth + "x" + screenHeight;
    }
}