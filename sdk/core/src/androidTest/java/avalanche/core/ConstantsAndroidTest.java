package avalanche.core;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
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
    public void loadFilesPathNullContext() {
        Constants.loadFromContext(null);
        Assert.assertNull(Constants.FILES_PATH);
    }

    @Test
    public void loadFilesPathError() {
        Context mockContext = mock(Context.class);
        when(mockContext.getFilesDir()).thenReturn(null);

        Constants.loadFromContext(mockContext);

        /* Should return null, not throw an exception. */
        Assert.assertNull(Constants.FILES_PATH);
    }
}
