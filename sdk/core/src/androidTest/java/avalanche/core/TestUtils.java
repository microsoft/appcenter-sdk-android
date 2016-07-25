package avalanche.core;

import java.util.Locale;
import java.util.Random;

import avalanche.core.ingestion.models.Device;
import avalanche.core.ingestion.models.json.MockLog;
import avalanche.core.utils.UUIDUtils;

public final class TestUtils {

    public static final String TAG = "TestRunner";

    private TestUtils() {
    }

    public static MockLog generateMockLog() {
        Random random = new Random(System.currentTimeMillis());

        Device device = new Device();
        device.setSdkVersion(String.format(Locale.ENGLISH, "%d.%d.%d", (random.nextInt(5) + 1), random.nextInt(10), random.nextInt(100)));
        device.setModel("S5");
        device.setOemName("HTC");
        device.setOsName("Android");
        device.setOsVersion(String.format(Locale.ENGLISH, "%d.%d.%d", (random.nextInt(5) + 1), random.nextInt(10), random.nextInt(100)));
        device.setOsApiLevel(random.nextInt(9) + 15);
        device.setLocale("en_US");
        device.setTimeZoneOffset(random.nextInt(52) * 30 - 720);
        device.setScreenSize(String.format(Locale.ENGLISH, "%dx%d", (random.nextInt(4) + 1) * 1000, (random.nextInt(10) + 1) * 100));
        device.setAppVersion(String.format(Locale.ENGLISH, "%d.%d.%d", (random.nextInt(5) + 1), random.nextInt(10), random.nextInt(100)));
        device.setAppBuild(Integer.toString(random.nextInt(1000) + 1));
        device.setAppNamespace("com.microsoft.unittest");

        MockLog log = new MockLog();
        log.setSid(UUIDUtils.randomUUID());
        log.setDevice(device);
        return log;
    }

}
