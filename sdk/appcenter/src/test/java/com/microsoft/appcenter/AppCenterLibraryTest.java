package com.microsoft.appcenter;

import android.content.Context;

import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.utils.AppCenterLog;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.appcenter.AppCenter.CORE_GROUP;
import static com.microsoft.appcenter.AppCenter.LOG_TAG;
import static com.microsoft.appcenter.AppCenter.PAIR_DELIMITER;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

public class AppCenterLibraryTest extends AbstractAppCenterTest {

    @Test
    public void nullApplicationTest() {
        AppCenter.startFromLibrary(null, DummyService.class);
        verify(DummyService.getInstance(), never()).onStarted(any(Context.class), any(Channel.class), anyString(), anyString(), anyBoolean());
        verifyStatic();
        AppCenterLog.error(eq(LOG_TAG), anyString());
    }

    @Test
    public void nullVarargArrayStartFromAppThenLibrary() {
        //noinspection ConfusingArgumentToVarargsMethod
        AppCenter.start(mApplication, DUMMY_APP_SECRET, (Class<? extends AppCenterService>[]) null);
        AppCenter.start((Class<? extends AppCenterService>) null);
        //noinspection ConfusingArgumentToVarargsMethod
        AppCenter.start((Class<? extends AppCenterService>[]) null);
        AppCenter.startFromLibrary(mApplication, (Class<? extends AppCenterService>[]) null);

        /* Verify that no services have been auto-loaded since none are configured for this */
        assertTrue(AppCenter.isConfigured());
        assertEquals(0, AppCenter.getInstance().getServices().size());
        assertEquals(mApplication, AppCenter.getInstance().getApplication());
    }

    @Test
    public void nullVarargArrayStartFromLibraryThenApp() {
        AppCenter.startFromLibrary(mApplication, (Class<? extends AppCenterService>[]) null);

        /* Verify that no services have been auto-loaded since none are configured for this */
        assertTrue(AppCenter.isConfigured());
        assertEquals(0, AppCenter.getInstance().getServices().size());
        assertEquals(mApplication, AppCenter.getInstance().getApplication());
        //noinspection ConfusingArgumentToVarargsMethod
        AppCenter.start(mApplication, DUMMY_APP_SECRET, (Class<? extends AppCenterService>[]) null);
        AppCenter.start((Class<? extends AppCenterService>) null);
        //noinspection ConfusingArgumentToVarargsMethod
        AppCenter.start((Class<? extends AppCenterService>[]) null);

        /* Verify again. */
        assertTrue(AppCenter.isConfigured());
        assertEquals(0, AppCenter.getInstance().getServices().size());
        assertEquals(mApplication, AppCenter.getInstance().getApplication());
    }

    @Test
    public void startFromLibraryThenFromApp() {

        /* Start two services from different libraries. */
        AppCenter.startFromLibrary(mApplication, DummyService.class, AnotherDummyService.class);
        AppCenter.startFromLibrary(mApplication, AnotherDummyService.class, DummyService.class);

        /* Verify App Center is configured and enabled. */
        assertTrue(AppCenter.isConfigured());
        assertTrue(AppCenter.isEnabled().get());

        /* Verify only dummy service has been started once, as the other one doesn't support start from library. */
        assertEquals(1, AppCenter.getInstance().getServices().size());
        assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
        verify(DummyService.getInstance()).getLogFactories();
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), isNull(String.class), isNull(String.class), eq(false));
        verify(mApplication).registerActivityLifecycleCallbacks(DummyService.getInstance());
        verify(mChannel, never()).enqueue(eq(mStartServiceLog), eq(CORE_GROUP));
        verify(mChannel, never()).setAppSecret(anyString());

        /* Verify state. */
        assertTrue(DummyService.isEnabled().get());
        assertFalse(AnotherDummyService.isEnabled().get());

        /* Start two services from app. */
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class, AnotherDummyService.class);

        /* Verify that the right amount of services have been loaded and configured. */
        assertEquals(2, AppCenter.getInstance().getServices().size());

        /* Verify first service updated. */
        verify(DummyService.getInstance()).onConfigurationUpdated(DUMMY_APP_SECRET, null);

        /* Verify previous behaviors happened only once, thus not again. */
        verify(DummyService.getInstance()).getLogFactories();
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), isNull(String.class), isNull(String.class), eq(false));
        verify(mApplication).registerActivityLifecycleCallbacks(DummyService.getInstance());

        /* Verify second service started. */
        assertTrue(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));
        verify(AnotherDummyService.getInstance()).getLogFactories();
        verify(AnotherDummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(AnotherDummyService.getInstance());

        /* And enabled. */
        assertTrue(AnotherDummyService.isEnabled().get());

        /* Verify start service log is sent. */
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP));
        List<String> services = new ArrayList<>();
        services.add(DummyService.getInstance().getServiceName());
        services.add(AnotherDummyService.getInstance().getServiceName());
        verify(mStartServiceLog).setServices(eq(services));

        /* Verify channel updated with app secret. */
        verify(mChannel).setAppSecret(DUMMY_APP_SECRET);
    }

    @Test
    public void startFromLibraryThenFromAppWithTargetToken() {

        /* Start two services from library. */
        AppCenter.startFromLibrary(mApplication, DummyService.class, AnotherDummyService.class);

        /* Verify only dummy service has been started once, as the other one doesn't support start from library. */
        assertEquals(1, AppCenter.getInstance().getServices().size());
        assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
        verify(DummyService.getInstance()).getLogFactories();
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), isNull(String.class), isNull(String.class), eq(false));
        verify(mApplication).registerActivityLifecycleCallbacks(DummyService.getInstance());
        verify(mChannel, never()).enqueue(eq(mStartServiceLog), eq(CORE_GROUP));
        verify(mChannel, never()).setAppSecret(anyString());

        /* Start two services from app. */
        AppCenter.start(mApplication, DUMMY_TARGET_TOKEN_STRING, DummyService.class, AnotherDummyService.class);

        /* Verify that the right amount of services have been loaded and configured. */
        assertEquals(1, AppCenter.getInstance().getServices().size());

        /* Verify first service updated. */
        verify(DummyService.getInstance()).onConfigurationUpdated(null, DUMMY_TRANSMISSION_TARGET_TOKEN);

        /* Verify previous behaviors happened only once, thus not again. */
        verify(DummyService.getInstance()).getLogFactories();
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), isNull(String.class), isNull(String.class), eq(false));
        verify(mApplication).registerActivityLifecycleCallbacks(DummyService.getInstance());

        /* Verify second service started. */
        assertFalse(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));
        verify(AnotherDummyService.getInstance(), never()).getLogFactories();
        verify(AnotherDummyService.getInstance(), never()).onStarted(any(Context.class), any(Channel.class), isNull(String.class), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
        verify(mApplication, never()).registerActivityLifecycleCallbacks(AnotherDummyService.getInstance());

        /* Verify start service log is sent. */
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP));
        List<String> services = new ArrayList<>();
        services.add(DummyService.getInstance().getServiceName());
        verify(mStartServiceLog).setServices(eq(services));

        /* Verify channel is not updated with app secret. */
        verify(mChannel, never()).setAppSecret(anyString());
    }

    @Test
    public void startFromLibraryThenFromAppWithBothSecrets() {

        /* Start two services from different libraries. */
        AppCenter.startFromLibrary(mApplication, DummyService.class, AnotherDummyService.class);
        AppCenter.startFromLibrary(mApplication, AnotherDummyService.class, DummyService.class);

        /* Verify only dummy service has been started once, as the other one doesn't support start from library. */
        assertEquals(1, AppCenter.getInstance().getServices().size());
        assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
        verify(DummyService.getInstance()).getLogFactories();
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), isNull(String.class), isNull(String.class), eq(false));
        verify(mApplication).registerActivityLifecycleCallbacks(DummyService.getInstance());
        verify(mChannel, never()).enqueue(eq(mStartServiceLog), eq(CORE_GROUP));
        verify(mChannel, never()).setAppSecret(anyString());

        /* Start two services from app with app secret and transmission target. */
        String secret = DUMMY_TARGET_TOKEN_STRING + PAIR_DELIMITER + DUMMY_APP_SECRET;
        AppCenter.start(mApplication, secret, DummyService.class, AnotherDummyService.class);

        /* Verify that the right amount of services have been loaded and configured. */
        assertEquals(2, AppCenter.getInstance().getServices().size());

        /* Verify first service updated. */
        verify(DummyService.getInstance()).onConfigurationUpdated(DUMMY_APP_SECRET, DUMMY_TRANSMISSION_TARGET_TOKEN);

        /* Verify previous behaviors happened only once, thus not again. */
        verify(DummyService.getInstance()).getLogFactories();
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), isNull(String.class), isNull(String.class), eq(false));
        verify(mApplication).registerActivityLifecycleCallbacks(DummyService.getInstance());

        /* Verify second service started. */
        assertTrue(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));
        verify(AnotherDummyService.getInstance()).getLogFactories();
        verify(AnotherDummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(AnotherDummyService.getInstance());

        /* Verify start service log is sent. */
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP));
        List<String> services = new ArrayList<>();
        services.add(DummyService.getInstance().getServiceName());
        services.add(AnotherDummyService.getInstance().getServiceName());
        verify(mStartServiceLog).setServices(eq(services));

        /* Verify channel updated with app secret. */
        verify(mChannel).setAppSecret(DUMMY_APP_SECRET);
    }

    @Test
    public void startFromAppThenFromLibrary() {

        /* Start two services from app. */
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class, AnotherDummyService.class);

        /* Verify that the right amount of services have been loaded and configured. */
        assertEquals(2, AppCenter.getInstance().getServices().size());

        /* Verify first service started. */
        assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
        verify(DummyService.getInstance()).getLogFactories();
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(DummyService.getInstance());

        /* Verify second service started. */
        assertTrue(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));
        verify(AnotherDummyService.getInstance()).getLogFactories();
        verify(AnotherDummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(AnotherDummyService.getInstance());

        /* Verify start service log is sent. */
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP));
        List<String> services = new ArrayList<>();
        services.add(DummyService.getInstance().getServiceName());
        services.add(AnotherDummyService.getInstance().getServiceName());
        verify(mStartServiceLog).setServices(eq(services));

        /* Start two services from 2 libraries. */
        AppCenter.startFromLibrary(mApplication, DummyService.class, AnotherDummyService.class);
        AppCenter.startFromLibrary(mApplication, AnotherDummyService.class, DummyService.class);

        /* We get no warnings as app started those. */
        verifyStatic(never());
        AppCenterLog.warn(anyString(), anyString());

        /* Check nothing changes as everything was already initialized by app start. */
        assertEquals(2, AppCenter.getInstance().getServices().size());
        assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
        verify(DummyService.getInstance()).getLogFactories();
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(DummyService.getInstance());
        assertTrue(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));
        verify(AnotherDummyService.getInstance()).getLogFactories();
        verify(AnotherDummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(AnotherDummyService.getInstance());
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP));
        services = new ArrayList<>();
        services.add(DummyService.getInstance().getServiceName());
        services.add(AnotherDummyService.getInstance().getServiceName());
        verify(mStartServiceLog).setServices(eq(services));

        /* Verify we didn't update app secret on channel. */
        verify(mChannel, never()).setAppSecret(anyString());
    }

    @Test
    public void startWithTargetTokenThenFromLibrary() {

        /* Start two services from app with a target token. */
        AppCenter.start(mApplication, DUMMY_TARGET_TOKEN_STRING, DummyService.class, AnotherDummyService.class);

        /* Verify that only first service started as the second service requires app secret. */
        assertEquals(1, AppCenter.getInstance().getServices().size());

        /* Verify first service started. */
        assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
        verify(DummyService.getInstance()).getLogFactories();
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), isNull(String.class), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(DummyService.getInstance());

        /* Verify second service not started. */
        assertFalse(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));
        verify(AnotherDummyService.getInstance(), never()).getLogFactories();
        verify(AnotherDummyService.getInstance(), never()).onStarted(any(Context.class), any(Channel.class), isNull(String.class), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
        verify(mApplication, never()).registerActivityLifecycleCallbacks(AnotherDummyService.getInstance());

        /* Verify start service log is sent with only first service. */
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP));
        List<String> services = new ArrayList<>();
        services.add(DummyService.getInstance().getServiceName());
        verify(mStartServiceLog).setServices(eq(services));

        /* Start two services from 2 libraries. */
        AppCenter.startFromLibrary(mApplication, DummyService.class, AnotherDummyService.class);
        AppCenter.startFromLibrary(mApplication, AnotherDummyService.class, DummyService.class);

        /* Check nothing changes as everything was already initialized by app start. */
        assertEquals(1, AppCenter.getInstance().getServices().size());
        assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
        verify(DummyService.getInstance()).getLogFactories();
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), isNull(String.class), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(DummyService.getInstance());
        assertFalse(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));
        verify(AnotherDummyService.getInstance(), never()).getLogFactories();
        verify(AnotherDummyService.getInstance(), never()).onStarted(any(Context.class), any(Channel.class), isNull(String.class), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
        verify(mApplication, never()).registerActivityLifecycleCallbacks(AnotherDummyService.getInstance());
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP));
        services = new ArrayList<>();
        services.add(DummyService.getInstance().getServiceName());
        verify(mStartServiceLog).setServices(eq(services));

        /* Verify we didn't update app secret on channel. */
        verify(mChannel, never()).setAppSecret(anyString());
    }

    @Test
    public void startFromLibraryDoesNotStartFromApp() {

        /* Start one service from app with both secrets. */
        String secret = DUMMY_TARGET_TOKEN_STRING + PAIR_DELIMITER + DUMMY_APP_SECRET;
        AppCenter.start(mApplication, secret, AnotherDummyService.class);

        /* Start another from library. */
        AppCenter.startFromLibrary(mApplication, DummyService.class);

        /* Verify second service started without secrets with library flag. */
        verify(DummyService.getInstance()).onStarted(mApplication, mChannel, null, null, false);

        /* Now start from app. */
        AppCenter.start(DummyService.class);

        /* It should update. */
        verify(DummyService.getInstance()).onConfigurationUpdated(DUMMY_APP_SECRET, DUMMY_TRANSMISSION_TARGET_TOKEN);

        /* And not call onStarted again (verify 1 total call). */
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), anyString(), anyString(), anyBoolean());
    }

    @Test
    public void startFromAppDoesNotEnableStartingUnsupportedServicesFromLibrary() {

        /* Start one service from app with both secrets. */
        String secret = DUMMY_TARGET_TOKEN_STRING + PAIR_DELIMITER + DUMMY_APP_SECRET;
        AppCenter.start(mApplication, secret, DummyService.class);

        /* Start another from library that does not support that mode. */
        AppCenter.startFromLibrary(mApplication, AnotherDummyService.class);

        /* Verify second service not started. */
        verify(AnotherDummyService.getInstance(), never()).onStarted(any(Context.class), any(Channel.class), anyString(), anyString(), anyBoolean());

        /* Now start from app instead. */
        AppCenter.start(AnotherDummyService.class);

        /* It should work now. */
        verify(AnotherDummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
    }
}
