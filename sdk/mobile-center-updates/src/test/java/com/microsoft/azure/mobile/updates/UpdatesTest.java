package com.microsoft.azure.mobile.updates;

import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import junit.framework.Assert;

import org.junit.Test;

import static com.microsoft.azure.mobile.updates.UpdateConstants.PREFERENCE_KEY_REQUEST_ID;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

public class UpdatesTest extends AbstractUpdatesTest {

    @Test
    public void singleton() {
        Assert.assertSame(Updates.getInstance(), Updates.getInstance());
    }

    @Test
    public void storeTokenBeforeStart() {
        when(StorageHelper.PreferencesStorage.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn("r");
        Updates.getInstance().storeUpdateToken("some token", "r");
        verifyStatic(never());
        StorageHelper.PreferencesStorage.putString(anyString(), anyString());
    }
}
