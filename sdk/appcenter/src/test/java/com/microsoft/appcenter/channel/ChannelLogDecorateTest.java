/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.channel;

import android.content.Context;
import android.os.Handler;

import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.ingestion.models.Device;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.persistence.Persistence;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.IdHelper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;
import java.util.UUID;

import static com.microsoft.appcenter.Flags.DEFAULTS;
import static org.mockito.ArgumentMatchers.any;
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
    public void checkLogAttributes() throws Exception {
        mockStatic(DeviceInfoHelper.class);
        Device device = mock(Device.class);
        when(DeviceInfoHelper.getDeviceInfo(any(Context.class))).thenReturn(device);
        mockStatic(IdHelper.class);
        String mockToken = UUID.randomUUID().toString();
        Channel channel = new DefaultChannel(mock(Context.class), UUID.randomUUID().toString(), mock(Persistence.class), mock(Ingestion.class), mock(Handler.class));
        channel.addGroup("", 0, 0, 0, null, null);

        /* Test a log that should be decorated. */
        for (int i = 0; i < 3; i++) {
            Log log = mock(Log.class);
            channel.enqueue(log, "", DEFAULTS);
            verify(log).setDevice(device);
            verify(log).setTimestamp(any(Date.class));
        }

        /* Check cache was used, meaning only 1 call to generate a device. */
        verifyStatic(DeviceInfoHelper.class);
        DeviceInfoHelper.getDeviceInfo(any(Context.class));

        /* Test a log that is already decorated. */
        Log log2 = mock(Log.class);
        when(log2.getDevice()).thenReturn(device);
        when(log2.getTimestamp()).thenReturn(new Date(123L));
        when(log2.getDataResidencyRegion()).thenReturn("region");
        channel.enqueue(log2, "", DEFAULTS);
        verify(log2, never()).setDevice(any(Device.class));
        verify(log2, never()).setTimestamp(any(Date.class));

        /* Simulate update to wrapper SDK. */
        Device device2 = mock(Device.class);
        when(DeviceInfoHelper.getDeviceInfo(any(Context.class))).thenReturn(device2);
        channel.invalidateDeviceCache();

        /* Generate some logs to verify device properties have been updated. */
        for (int i = 0; i < 3; i++) {
            Log log3 = mock(Log.class);
            channel.enqueue(log3, "", DEFAULTS);
            verify(log3).setDevice(device2);
            verify(log3).setTimestamp(any(Date.class));
        }

        /* Check only 1 device has been generated after cache invalidate. */
        verifyStatic(DeviceInfoHelper.class, times(2));
        DeviceInfoHelper.getDeviceInfo(any(Context.class));
    }
}
