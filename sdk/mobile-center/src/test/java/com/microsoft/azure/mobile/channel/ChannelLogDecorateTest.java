package com.microsoft.azure.mobile.channel;

import android.content.Context;
import android.os.Handler;

import com.microsoft.azure.mobile.ingestion.Ingestion;
import com.microsoft.azure.mobile.ingestion.models.Device;
import com.microsoft.azure.mobile.ingestion.models.Log;
import com.microsoft.azure.mobile.persistence.Persistence;
import com.microsoft.azure.mobile.utils.DeviceInfoHelper;
import com.microsoft.azure.mobile.utils.IdHelper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest({DeviceInfoHelper.class, IdHelper.class})
public class ChannelLogDecorateTest {

    @Test
    public void checkLogAttributes() throws DeviceInfoHelper.DeviceInfoException {
        mockStatic(DeviceInfoHelper.class);
        Device device = mock(Device.class);
        when(DeviceInfoHelper.getDeviceInfo(any(Context.class))).thenReturn(device);
        mockStatic(IdHelper.class);
        Channel channel = new DefaultChannel(mock(Context.class), UUID.randomUUID().toString(), mock(Persistence.class), mock(Ingestion.class), mock(Handler.class));
        channel.addGroup("", 0, 0, 0, null);

        /* Test a log that should be decorated. */
        for (int i = 0; i < 3; i++) {
            Log log = mock(Log.class);
            channel.enqueue(log, "");
            verify(log).setDevice(device);
            verify(log).setToffset(anyLong());
        }

        /* Check cache was used, meaning only 1 call to generate a device. */
        verifyStatic();
        DeviceInfoHelper.getDeviceInfo(any(Context.class));

        /* Test a log that is already decorated. */
        Log log2 = mock(Log.class);
        when(log2.getDevice()).thenReturn(device);
        when(log2.getToffset()).thenReturn(123L);
        channel.enqueue(log2, "");
        verify(log2, never()).setDevice(any(Device.class));
        verify(log2, never()).setToffset(anyLong());

        /* Simulate update to wrapper SDK. */
        Device device2 = mock(Device.class);
        when(DeviceInfoHelper.getDeviceInfo(any(Context.class))).thenReturn(device2);
        channel.invalidateDeviceCache();

        /* Generate some logs to verify device properties have been updated. */
        for (int i = 0; i < 3; i++) {
            Log log3 = mock(Log.class);
            channel.enqueue(log3, "");
            verify(log3).setDevice(device2);
            verify(log3).setToffset(anyLong());
        }

        /* Check only 1 device has been generated after cache invalidate. */
        verifyStatic(times(2));
        DeviceInfoHelper.getDeviceInfo(any(Context.class));
    }
}
