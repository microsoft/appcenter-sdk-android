package avalanche.core;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import avalanche.core.utils.StorageHelper;

import static avalanche.core.utils.PrefStorageConstants.KEY_ENABLED;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest(StorageHelper.PreferencesStorage.class)
public class AvalancheFeatureTest {

    @Before
    public void setUp() {

        /* First call to avalanche.isInstanceEnabled shall return true, initial state. */
        mockStatic(StorageHelper.PreferencesStorage.class);
        final String key = KEY_ENABLED + "_group_test";
        when(StorageHelper.PreferencesStorage.getBoolean(key, true)).thenReturn(true);

        /* Then simulate further changes to state. */
        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {

                /* Whenever the new state is persisted, make further calls return the new state. */
                boolean enabled = (Boolean) invocation.getArguments()[1];
                when(StorageHelper.PreferencesStorage.getBoolean(key, true)).thenReturn(enabled);
                return null;
            }
        }).when(StorageHelper.PreferencesStorage.class);
        StorageHelper.PreferencesStorage.putBoolean(eq(key), anyBoolean());
    }

    @Test
    public void featureEnabledTest() {

        AvalancheFeature feature = new TestAvalancheFeature();
        // Feature should be enabled by default
        assertTrue(feature.isInstanceEnabled());

        // Test disabling the feature
        feature.setInstanceEnabled(false);
        assertFalse(feature.isInstanceEnabled());

        // Test re-enabling the feature
        feature.setInstanceEnabled(true);
        assertTrue(feature.isInstanceEnabled());
    }

    static class TestAvalancheFeature extends AbstractAvalancheFeature {
        @Override
        protected String getGroupName() {
            return "group_test";
        }
    }
}
