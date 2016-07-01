package avalanche.sasquatch;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import avalanche.base.ingestion.models.DeviceLog;
import avalanche.base.ingestion.models.json.DefaultLogSerializer;
import avalanche.base.utils.DeviceInfoHelper;

import static org.junit.Assert.assertEquals;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentationTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        DeviceLog deviceLog = DeviceInfoHelper.getDeviceLog(appContext);
        deviceLog.setSid(UUID.randomUUID());
        String payload = new DefaultLogSerializer().serializeLog(deviceLog);
        Log.i("Avalanche", "technicals=" + payload);
        assertEquals("avalanche.sasquatch", appContext.getPackageName());
    }
}