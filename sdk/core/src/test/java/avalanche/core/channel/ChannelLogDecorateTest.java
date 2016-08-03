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
import avalanche.core.ingestion.models.json.LogSerializer;
import avalanche.core.persistence.AvalanchePersistence;
import avalanche.core.utils.DeviceInfoHelper;
import avalanche.core.utils.IdHelper;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest({System.class, DeviceInfoHelper.class, IdHelper.class, DefaultAvalancheChannel.class})
public class ChannelLogDecorateTest {

    @Test
    public void checkLogAttributes() throws DeviceInfoHelper.DeviceInfoException {
        mockStatic(System.class);
        when(System.currentTimeMillis()).thenReturn(123L);
        mockStatic(DeviceInfoHelper.class);
        Device device = mock(Device.class);
        when(DeviceInfoHelper.getDeviceInfo(any(Context.class))).thenReturn(device).thenReturn(null);
        mockStatic(IdHelper.class);
        Log log = mock(Log.class);
        AvalancheChannel channel = new DefaultAvalancheChannel(mock(Context.class), UUID.randomUUID(), mock(AvalancheIngestion.class), mock(AvalanchePersistence.class), mock(LogSerializer.class));
        channel.addGroup("", 0, 0, 0, null);
        int logCount = 2;
        for (int i = 0; i < logCount; i++)
            channel.enqueue(log, "");
        verify(log, times(logCount)).setDevice(device);
        verify(log, times(logCount)).setToffset(123L);
    }
}
