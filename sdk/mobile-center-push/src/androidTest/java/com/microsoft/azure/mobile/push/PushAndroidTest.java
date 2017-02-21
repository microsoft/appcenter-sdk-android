package com.microsoft.azure.mobile.push;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.microsoft.azure.mobile.Constants;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.junit.Before;
import org.junit.BeforeClass;

import static com.microsoft.azure.mobile.test.TestUtils.TAG;

@SuppressWarnings("unused")
public class PushAndroidTest {

    @SuppressLint("StaticFieldLeak")
    private static Context sContext;

    @BeforeClass
    public static void setUpClass() {
        MobileCenterLog.setLogLevel(android.util.Log.VERBOSE);
        sContext = InstrumentationRegistry.getContext();
        Constants.loadFromContext(sContext);
        StorageHelper.initialize(sContext);
    }

    @Before
    public void cleanup() {
        android.util.Log.i(TAG, "Cleanup");
        Push.unsetInstance();
        StorageHelper.PreferencesStorage.clear();
    }
}
