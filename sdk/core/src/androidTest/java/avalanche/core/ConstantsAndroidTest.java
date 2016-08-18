package avalanche.core;

import android.support.test.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ConstantsAndroidTest {

    @Before
    public void setUp() {
        Constants.FILES_PATH = null;
    }

    @Test
    public void loadFilesPath() {
        Constants.loadFromContext(InstrumentationRegistry.getContext());
        Assert.assertNotNull(Constants.FILES_PATH);
    }

    @Test
    public void loadFilesPathError() {
        Constants.loadFromContext(null);

        /* Should return null, not throw an exception. */
        Assert.assertNull(Constants.FILES_PATH);
    }
}
