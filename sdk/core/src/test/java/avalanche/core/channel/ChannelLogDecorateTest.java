package avalanche.core.channel;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.UUID;

import avalanche.core.ingestion.AvalancheIngestion;
import avalanche.core.ingestion.models.Device;
import avalanche.core.ingestion.models.Log;
import avalanche.core.persistence.AvalanchePersistence;
import avalanche.core.utils.DeviceInfoHelper;
import avalanche.core.utils.IdHelper;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest({DeviceInfoHelper.class, IdHelper.class})
public class ChannelLogDecorateTest {

    @Test
    public void checkLogAttributes() throws DeviceInfoHelper.DeviceInfoException {
        mockStatic(DeviceInfoHelper.class);
        Device device = mock(Device.class);
        when(DeviceInfoHelper.getDeviceInfo(any(Context.class))).thenReturn(device).thenReturn(null);
        mockStatic(IdHelper.class);
        AvalancheChannel channel = new DefaultAvalancheChannel(mock(Context.class), UUID.randomUUID(), mock(AvalanchePersistence.class), mock(AvalancheIngestion.class));
        channel.addGroup("", 0, 0, 0, null);

        /* Test a log that should be decorated. */
        Log log = mock(Log.class);
        channel.enqueue(log, "");
        verify(log).setDevice(device);
        verify(log).setToffset(anyLong());

        /* Test a log that is already decorated. */
        Log log2 = mock(Log.class);
        when(log2.getDevice()).thenReturn(device);
        when(log2.getToffset()).thenReturn(123L);
        channel.enqueue(log2, "");
        verify(log2, never()).setDevice(any(Device.class));
        verify(log2, never()).setToffset(anyLong());
    }
}
