package com.microsoft.azure.mobile.push;

import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest({ StorageHelper.PreferencesStorage.class})
public class PushTest {
    @Test
    public void getInstance() throws Exception {
        Assert.assertSame(Push.getInstance(), Push.getInstance());
    }

}