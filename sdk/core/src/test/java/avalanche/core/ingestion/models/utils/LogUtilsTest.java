package avalanche.core.ingestion.models.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import avalanche.core.ingestion.models.Device;

@RunWith(PowerMockRunner.class)
public class LogUtilsTest {

    @Test(expected = IllegalArgumentException.class)
    public void checkNotNullTest() {
        LogUtils.checkNotNull("", null);
    }

    @Test
    public void validateTest() {
        List<Device> devices = new ArrayList<>();

        /* Mock device logs. */
        for (int i = 0; i < 5; i++)
            devices.add(Mockito.mock(Device.class));

        LogUtils.validateArray(devices);

        /* Verify. */
        for (Device device : devices)
            Mockito.verify(device).validate();
    }
}
