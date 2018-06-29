package com.microsoft.appcenter;

import android.content.Context;

import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.channel.OneCollectorChannelListener;
import com.microsoft.appcenter.ingestion.models.CustomPropertiesLog;
import com.microsoft.appcenter.ingestion.models.StartServiceLog;
import com.microsoft.appcenter.ingestion.models.WrapperSdk;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.ShutdownHelper;
import com.microsoft.appcenter.utils.storage.StorageHelper;

import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.microsoft.appcenter.AppCenter.APP_SECRET_KEY;
import static com.microsoft.appcenter.AppCenter.CORE_GROUP;
import static com.microsoft.appcenter.AppCenter.KEY_VALUE_DELIMITER;
import static com.microsoft.appcenter.AppCenter.LOG_TAG;
import static com.microsoft.appcenter.AppCenter.PAIR_DELIMITER;
import static com.microsoft.appcenter.utils.PrefStorageConstants.KEY_ENABLED;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

public class AppCenterTest extends AbstractAppCenterTest {

    @Test
    public void singleton() {
        assertNotNull(AppCenter.getInstance());
        assertSame(AppCenter.getInstance(), AppCenter.getInstance());
    }

    @Test
    public void nullVarargClass() {
        AppCenter.start(mApplication, DUMMY_APP_SECRET, (Class<? extends AppCenterService>) null);

        /* Verify that no services have been auto-loaded since none are configured for this */
        assertTrue(AppCenter.isConfigured());
        assertEquals(0, AppCenter.getInstance().getServices().size());
        assertEquals(mApplication, AppCenter.getInstance().getApplication());
    }

    @Test
    public void startServiceBeforeConfigure() {
        AppCenter.start(DummyService.class);
        assertFalse(AppCenter.isConfigured());
        assertNull(AppCenter.getInstance().getServices());
    }

    @Test
    public void useDummyServiceTest() {
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);

        /* Verify that single service has been loaded and configured */
        assertEquals(1, AppCenter.getInstance().getServices().size());
        DummyService service = DummyService.getInstance();
        assertTrue(AppCenter.getInstance().getServices().contains(service));
        verify(service).getLogFactories();
        verify(service).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(service);
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP));
        List<String> services = new ArrayList<>();
        services.add(service.getServiceName());
        verify(mStartServiceLog).setServices(eq(services));
    }

    @Test
    public void useDummyServiceWhenDisablePersisted() {
        when(StorageHelper.PreferencesStorage.getBoolean(KEY_ENABLED, true)).thenReturn(false);
        AppCenter appCenter = AppCenter.getInstance();
        DummyService service = DummyService.getInstance();
        AnotherDummyService anotherService = AnotherDummyService.getInstance();

        /* Start. */
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        assertFalse(AppCenter.isEnabled().get());
        assertEquals(1, AppCenter.getInstance().getServices().size());
        assertTrue(appCenter.getServices().contains(service));
        verify(mChannel, never()).enqueue(eq(mStartServiceLog), eq(CORE_GROUP));

        /* Start another service. */
        AppCenter.start(AnotherDummyService.class);
        assertFalse(AppCenter.isEnabled().get());
        assertEquals(2, AppCenter.getInstance().getServices().size());
        assertTrue(appCenter.getServices().contains(anotherService));
        verify(mChannel, never()).enqueue(eq(mStartServiceLog), eq(CORE_GROUP));

        /* Enable. */
        AppCenter.setEnabled(true);
        assertTrue(AppCenter.isEnabled().get());
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP));
        List<String> services = new ArrayList<>();
        services.add(service.getServiceName());
        services.add(anotherService.getServiceName());
        verify(mStartServiceLog).setServices(eq(services));
    }

    @Test
    public void useDummyServiceTestSplitCall() {
        assertFalse(AppCenter.isConfigured());
        AppCenter.configure(mApplication, DUMMY_APP_SECRET);
        assertTrue(AppCenter.isConfigured());
        AppCenter.start(DummyService.class);

        /* Verify that single service has been loaded and configured */
        assertEquals(1, AppCenter.getInstance().getServices().size());
        DummyService service = DummyService.getInstance();
        assertTrue(AppCenter.getInstance().getServices().contains(service));
        verify(service).getLogFactories();
        verify(service).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(service);
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP));
        List<String> services = new ArrayList<>();
        services.add(service.getServiceName());
        verify(mStartServiceLog).setServices(eq(services));
    }

    @Test
    public void configureAndStartTwiceTest() {
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        AppCenter.start(mApplication, DUMMY_APP_SECRET + "a", AnotherDummyService.class); //ignored

        /* Verify that single service has been loaded and configured */
        assertEquals(1, AppCenter.getInstance().getServices().size());
        DummyService service = DummyService.getInstance();
        assertTrue(AppCenter.getInstance().getServices().contains(service));
        verify(service).getLogFactories();
        verify(service).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
        verify(service, never()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET + "a"), isNull(String.class), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(service);
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP));
        List<String> services = new ArrayList<>();
        services.add(service.getServiceName());
        verify(mStartServiceLog).setServices(eq(services));
    }

    @Test
    public void startTargetTokenThenStartWithAppSecretTest() {
        AppCenter.start(mApplication, DUMMY_TARGET_TOKEN_STRING, DummyService.class);
        AppCenter.start(mApplication, DUMMY_APP_SECRET, AnotherDummyService.class);

        /* Verify that single service has been loaded and configured */
        assertEquals(1, AppCenter.getInstance().getServices().size());
        DummyService service = DummyService.getInstance();
        assertTrue(AppCenter.getInstance().getServices().contains(service));
        verify(service).getLogFactories();
        verify(service).onStarted(any(Context.class), any(Channel.class), isNull(String.class), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
        verify(service, never()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(service);
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP));
        List<String> services = new ArrayList<>();
        services.add(service.getServiceName());
        verify(mStartServiceLog).setServices(eq(services));
    }

    @Test
    public void startAppSecretThenStartWithTargetTokenTest() {
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        AppCenter.start(mApplication, DUMMY_TARGET_TOKEN_STRING, AnotherDummyService.class);

        /* Verify that single service has been loaded and configured */
        assertEquals(1, AppCenter.getInstance().getServices().size());
        DummyService service = DummyService.getInstance();
        assertTrue(AppCenter.getInstance().getServices().contains(service));
        verify(service).getLogFactories();
        verify(service, never()).onStarted(any(Context.class), any(Channel.class), isNull(String.class), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
        verify(service).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(service);
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP));
        List<String> services = new ArrayList<>();
        services.add(service.getServiceName());
        verify(mStartServiceLog).setServices(eq(services));
    }


    @Test
    public void configureTwiceTest() {
        AppCenter.configure(mApplication, DUMMY_APP_SECRET);
        AppCenter.configure(mApplication, DUMMY_APP_SECRET + "a"); //ignored
        AppCenter.start(DummyService.class);

        /* Verify that single service has been loaded and configured */
        assertEquals(1, AppCenter.getInstance().getServices().size());
        DummyService service = DummyService.getInstance();
        assertTrue(AppCenter.getInstance().getServices().contains(service));
        verify(service).getLogFactories();
        verify(service).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
        verify(service, never()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET + "a"), isNull(String.class), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(service);
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP));
        List<String> services = new ArrayList<>();
        services.add(service.getServiceName());
        verify(mStartServiceLog).setServices(eq(services));

        /* We don't support updating app secret that way. */
        verify(service, never()).onConfigurationUpdated(anyString(), anyString());
    }

    @Test
    public void startTwoServicesTest() {
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
    }

    @Test
    public void startTwoServicesSplit() {
        AppCenter.configure(mApplication, DUMMY_APP_SECRET);
        AppCenter.start(DummyService.class, AnotherDummyService.class);

        /* Verify that the right amount of services have been loaded and configured */
        assertEquals(2, AppCenter.getInstance().getServices().size());
        {
            assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
            verify(DummyService.getInstance()).getLogFactories();
            verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
            verify(mApplication).registerActivityLifecycleCallbacks(DummyService.getInstance());
        }
        {
            assertTrue(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));
            verify(AnotherDummyService.getInstance()).getLogFactories();
            verify(AnotherDummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
            verify(mApplication).registerActivityLifecycleCallbacks(AnotherDummyService.getInstance());
        }
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP));
        List<String> services = new ArrayList<>();
        services.add(DummyService.getInstance().getServiceName());
        services.add(AnotherDummyService.getInstance().getServiceName());
        verify(mStartServiceLog).setServices(eq(services));
    }

    @Test
    public void startTwoServicesSplitEvenMore() {
        AppCenter.configure(mApplication, DUMMY_APP_SECRET);
        AppCenter.start(DummyService.class);
        AppCenter.start(AnotherDummyService.class);

        /* Verify that the right amount of services have been loaded and configured */
        assertEquals(2, AppCenter.getInstance().getServices().size());
        {
            assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
            verify(DummyService.getInstance()).getLogFactories();
            verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
            verify(mApplication).registerActivityLifecycleCallbacks(DummyService.getInstance());
        }
        {
            assertTrue(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));
            verify(AnotherDummyService.getInstance()).getLogFactories();
            verify(AnotherDummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
            verify(mApplication).registerActivityLifecycleCallbacks(AnotherDummyService.getInstance());
        }
        verify(mChannel, times(2)).enqueue(any(StartServiceLog.class), eq(CORE_GROUP));
        List<String> services1 = new ArrayList<>();
        services1.add(DummyService.getInstance().getServiceName());
        verify(mStartServiceLog).setServices(eq(services1));
        List<String> services2 = new ArrayList<>();
        services2.add(DummyService.getInstance().getServiceName());
        verify(mStartServiceLog).setServices(eq(services2));
    }

    @Test
    public void startTwoServicesWithSomeInvalidReferences() {
        AppCenter.start(mApplication, DUMMY_APP_SECRET, null, DummyService.class, null, InvalidService.class, AnotherDummyService.class, null);

        /* Verify that the right amount of services have been loaded and configured */
        assertEquals(2, AppCenter.getInstance().getServices().size());
        {
            assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
            verify(DummyService.getInstance()).getLogFactories();
            verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
            verify(mApplication).registerActivityLifecycleCallbacks(DummyService.getInstance());
        }
        {
            assertTrue(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));
            verify(AnotherDummyService.getInstance()).getLogFactories();
            verify(AnotherDummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
            verify(mApplication).registerActivityLifecycleCallbacks(AnotherDummyService.getInstance());
        }
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP));
        List<String> services = new ArrayList<>();
        services.add(DummyService.getInstance().getServiceName());
        services.add(AnotherDummyService.getInstance().getServiceName());
        verify(mStartServiceLog).setServices(eq(services));
    }

    @Test
    public void startTwoServicesWithSomeInvalidReferencesSplit() {
        AppCenter.configure(mApplication, DUMMY_APP_SECRET);
        AppCenter.start((Class<AppCenterService>) null, DummyService.class, null);
        AppCenter.start(InvalidService.class, AnotherDummyService.class, null);

        /* Verify that the right amount of services have been loaded and configured */
        assertEquals(2, AppCenter.getInstance().getServices().size());
        {
            assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
            verify(DummyService.getInstance()).getLogFactories();
            verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
            verify(mApplication).registerActivityLifecycleCallbacks(DummyService.getInstance());
        }
        {
            assertTrue(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));
            verify(AnotherDummyService.getInstance()).getLogFactories();
            verify(AnotherDummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
            verify(mApplication).registerActivityLifecycleCallbacks(AnotherDummyService.getInstance());
        }
        verify(mChannel, times(2)).enqueue(any(StartServiceLog.class), eq(CORE_GROUP));
        List<String> services1 = new ArrayList<>();
        services1.add(DummyService.getInstance().getServiceName());
        verify(mStartServiceLog).setServices(eq(services1));
        List<String> services2 = new ArrayList<>();
        services2.add(DummyService.getInstance().getServiceName());
        verify(mStartServiceLog).setServices(eq(services2));
    }

    @Test
    public void startServiceTwice() {

        /* Start once. */
        AppCenter.configure(mApplication, DUMMY_APP_SECRET);
        AppCenter.start(DummyService.class);

        /* Check. */
        assertEquals(1, AppCenter.getInstance().getServices().size());
        DummyService service = DummyService.getInstance();
        assertTrue(AppCenter.getInstance().getServices().contains(service));
        verify(service).getLogFactories();
        verify(service).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(service);

        /* Start twice, this call is ignored. */
        AppCenter.start(DummyService.class);

        /* Verify that single service has been loaded and configured (only once interaction). */
        assertEquals(1, AppCenter.getInstance().getServices().size());
        verify(service).getLogFactories();
        verify(service).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(service);
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP));
        List<String> services = new ArrayList<>();
        services.add(DummyService.getInstance().getServiceName());
        verify(mStartServiceLog).setServices(eq(services));
    }

    @Test
    public void enableTest() {

        /* Start App Center SDK */
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class, AnotherDummyService.class);
        AppCenter appCenter = AppCenter.getInstance();

        /* Verify services are enabled by default */
        Set<AppCenterService> services = appCenter.getServices();
        assertTrue(AppCenter.isEnabled().get());
        DummyService dummyService = DummyService.getInstance();
        AnotherDummyService anotherDummyService = AnotherDummyService.getInstance();
        for (AppCenterService service : services) {
            assertTrue(service.isInstanceEnabled());
        }

        /* Explicit set enabled should not change that */
        AppCenter.setEnabled(true);
        assertTrue(AppCenter.isEnabled().get());
        for (AppCenterService service : services) {
            assertTrue(service.isInstanceEnabled());
        }
        verify(dummyService, never()).setInstanceEnabled(anyBoolean());
        verify(anotherDummyService, never()).setInstanceEnabled(anyBoolean());
        verify(mChannel, times(2)).setEnabled(true);
        verify(mNetworkStateHelper, never()).close();
        verify(mNetworkStateHelper, never()).reopen();

        /* Verify disabling base disables all services */
        AppCenter.setEnabled(false);
        assertFalse(AppCenter.isEnabled().get());
        for (AppCenterService service : services) {
            assertFalse(service.isInstanceEnabled());
        }
        verify(dummyService).setInstanceEnabled(false);
        verify(anotherDummyService).setInstanceEnabled(false);
        verify(mChannel).setEnabled(false);
        verify(mNetworkStateHelper).close();
        verify(mNetworkStateHelper, never()).reopen();

        /* Verify re-enabling base re-enables all services */
        AppCenter.setEnabled(true);
        assertTrue(AppCenter.isEnabled().get());
        for (AppCenterService service : services) {
            assertTrue(service.isInstanceEnabled());
        }
        verify(dummyService).setInstanceEnabled(true);
        verify(anotherDummyService).setInstanceEnabled(true);
        verify(mApplication, times(1)).registerActivityLifecycleCallbacks(dummyService);
        verify(mApplication, times(1)).registerActivityLifecycleCallbacks(anotherDummyService);
        verify(mChannel, times(3)).setEnabled(true);
        verify(mNetworkStateHelper).reopen();

        /* Verify that disabling one service leaves base and other services enabled */
        dummyService.setInstanceEnabledAsync(false);
        assertFalse(dummyService.isInstanceEnabled());
        assertTrue(AppCenter.isEnabled().get());
        assertTrue(anotherDummyService.isInstanceEnabled());

        /* Enable back via main class. */
        AppCenter.setEnabled(true);
        assertTrue(AppCenter.isEnabled().get());
        for (AppCenterService service : services) {
            assertTrue(service.isInstanceEnabled());
        }
        verify(dummyService, times(2)).setInstanceEnabled(true);
        verify(anotherDummyService).setInstanceEnabled(true);
        verify(mChannel, times(4)).setEnabled(true);
        verify(mNetworkStateHelper, times(1)).reopen();

        /* Enable service after the SDK is disabled. */
        AppCenter.setEnabled(false);
        assertFalse(AppCenter.isEnabled().get());
        for (AppCenterService service : services) {
            assertFalse(service.isInstanceEnabled());
        }
        dummyService.setInstanceEnabledAsync(true);
        assertFalse(dummyService.isInstanceEnabledAsync().get());
        verifyStatic();
        AppCenterLog.error(eq(LOG_TAG), anyString());
        assertFalse(AppCenter.isEnabled().get());
        verify(mChannel, times(2)).setEnabled(false);
        verify(mNetworkStateHelper, times(2)).close();

        /* Disable back via main class. */
        AppCenter.setEnabled(false);
        assertFalse(AppCenter.isEnabled().get());
        for (AppCenterService service : services) {
            assertFalse(service.isInstanceEnabled());
        }
        verify(mChannel, times(3)).setEnabled(false);
        verify(mNetworkStateHelper, times(2)).close();

        /* Check factories / channel only once interactions. */
        verify(dummyService).getLogFactories();
        verify(dummyService).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
        verify(anotherDummyService).getLogFactories();
        verify(anotherDummyService).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
    }

    @Test
    public void disableBetweenStartCalls() {
        DummyService dummyService = DummyService.getInstance();
        AnotherDummyService anotherDummyService = AnotherDummyService.getInstance();

        /* Start App Center SDK with one service. */
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        assertTrue(AppCenter.isEnabled().get());
        assertTrue(dummyService.isInstanceEnabled());

        /* Disable. */
        AppCenter.setEnabled(false);
        assertFalse(AppCenter.isEnabled().get());
        assertFalse(dummyService.isInstanceEnabled());

        /* Start another one service. */
        AppCenter.start(AnotherDummyService.class);
        assertFalse(AppCenter.isEnabled().get());
        assertFalse(dummyService.isInstanceEnabled());
        assertFalse(anotherDummyService.isInstanceEnabled());
    }

    @Test
    public void enableBeforeConfiguredTest() {

        /* Test isEnabled and setEnabled before configure */
        assertFalse(AppCenter.isEnabled().get());
        AppCenter.setEnabled(true);
        assertFalse(AppCenter.isEnabled().get());
        verifyStatic(times(3));
        AppCenterLog.error(eq(LOG_TAG), anyString());
    }

    @Test
    public void disablePersisted() {
        when(StorageHelper.PreferencesStorage.getBoolean(KEY_ENABLED, true)).thenReturn(false);
        when(StorageHelper.PreferencesStorage.getBoolean(AnotherDummyService.getInstance().getEnabledPreferenceKey(), true)).thenReturn(false);
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class, AnotherDummyService.class);
        AppCenter appCenter = AppCenter.getInstance();

        /* Verify services are disabled by default if App Center is disabled. */
        assertFalse(AppCenter.isEnabled().get());
        for (AppCenterService service : appCenter.getServices()) {
            assertFalse(service.isInstanceEnabled());
            verify((AbstractAppCenterService) service).applyEnabledState(eq(false));
            verify((AbstractAppCenterService) service, never()).applyEnabledState(eq(true));
            verify(mApplication).registerActivityLifecycleCallbacks(service);
        }
        verify(mNetworkStateHelper).close();

        /* Verify we can enable back. */
        AppCenter.setEnabled(true);
        assertTrue(AppCenter.isEnabled().get());
        for (AppCenterService service : appCenter.getServices()) {
            assertTrue(service.isInstanceEnabled());
            verify((AbstractAppCenterService) service).applyEnabledState(eq(true));
        }
        verify(mNetworkStateHelper).reopen();
    }

    @Test
    public void disabledBeforeStart() {
        when(StorageHelper.PreferencesStorage.getBoolean(KEY_ENABLED, true)).thenReturn(true);

        /* Verify services are disabled if called before start (no access to storage). */
        assertFalse(AppCenter.isEnabled().get());
        assertFalse(DummyService.isEnabled().get());

        /* Verify we can not enable until start. */
        AppCenter.setEnabled(true);
        assertFalse(AppCenter.isEnabled().get());
        assertFalse(DummyService.isEnabled().get());
    }

    @Test
    public void disablePersistedAndDisable() {
        when(StorageHelper.PreferencesStorage.getBoolean(KEY_ENABLED, true)).thenReturn(false);
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class, AnotherDummyService.class);
        AppCenter appCenter = AppCenter.getInstance();

        /* Its already disabled so disable should have no effect on App Center but should disable services. */
        AppCenter.setEnabled(false);
        assertFalse(AppCenter.isEnabled().get());
        for (AppCenterService service : appCenter.getServices()) {
            assertFalse(service.isInstanceEnabled());
            verify(mApplication).registerActivityLifecycleCallbacks(service);
        }

        /* Verify we can enable App Center back, should have no effect on service except registering the mApplication life cycle callbacks. */
        AppCenter.setEnabled(true);
        assertTrue(AppCenter.isEnabled().get());
        for (AppCenterService service : appCenter.getServices()) {
            assertTrue(service.isInstanceEnabled());

            /* Happened only once. */
            verify(mApplication).registerActivityLifecycleCallbacks(service);
        }
    }

    @Test
    public void invalidServiceTest() {
        AppCenter.start(mApplication, DUMMY_APP_SECRET, InvalidService.class);
        verifyStatic();
        AppCenterLog.error(eq(LOG_TAG), anyString(), any(NoSuchMethodException.class));
    }

    @Test
    public void nullApplicationTest() {

        /* First verify start from application. */
        AppCenter.start(null, DUMMY_APP_SECRET, DummyService.class);
        verify(DummyService.getInstance(), never()).onStarted(any(Context.class), any(Channel.class), anyString(), anyString(), anyBoolean());
        verifyStatic();
        AppCenterLog.error(eq(LOG_TAG), anyString());
    }

    @Test
    public void startWithNullAppSecretTest() {
        AppCenter.start(mApplication, (String) null, DummyService.class);
        checkNullOrEmptySecretStringForbidden();
    }

    @Test
    public void configureWithNullAppSecretTest() {
        AppCenter.configure(mApplication, null);
        AppCenter.start(DummyService.class);
        checkNullOrEmptySecretStringForbidden();
    }

    @Test
    public void startWithEmptyAppSecretTest() {
        AppCenter.start(mApplication, "", DummyService.class);
        checkNullOrEmptySecretStringForbidden();
    }

    @Test
    public void configureWithEmptyAppSecretTest() {
        AppCenter.configure(mApplication, "");
        AppCenter.start(DummyService.class);
        checkNullOrEmptySecretStringForbidden();
    }

    private void checkNullOrEmptySecretStringForbidden() {

        /* App Center is not configured. */
        assertFalse(AppCenter.isConfigured());

        /* Verify service did not start with null secrets from application. */
        verify(DummyService.getInstance(), never()).onStarted(any(Context.class), any(Channel.class), isNull(String.class), isNull(String.class), eq(true));
    }

    @Test
    public void startWithoutAppSecretTest() {

        /* Start App Center without an app secret. */
        AppCenter.start(mApplication, DummyService.class);
        checkStartedWithoutAppSecret();
    }

    @Test
    public void configureWithoutAppSecretTest() {

        /* Configure and start App Center without an app secret. */
        AppCenter.configure(mApplication);
        AppCenter.start(DummyService.class);
        checkStartedWithoutAppSecret();
    }

    private void checkStartedWithoutAppSecret() {

        /* App Center is configured that way. */
        assertTrue(AppCenter.isConfigured());

        /* Verify service started with null secrets from application. */
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), isNull(String.class), isNull(String.class), eq(true));

        /* We must not be able to reconfigure app secret from null/empty state. */
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class, AnotherDummyService.class);
        AppCenter.start(mApplication, (String) null, DummyService.class, AnotherDummyService.class);
        AppCenter.start(mApplication, "", DummyService.class, AnotherDummyService.class);

        /* Verify start not called again (1 total call). */
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), anyString(), anyString(), anyBoolean());

        /* And not updated either. */
        verify(DummyService.getInstance(), never()).onConfigurationUpdated(anyString(), anyString());

        /* Verify the second service was not started as was part of second secret configuration. */
        verify(AnotherDummyService.getInstance(), never()).onStarted(any(Context.class), any(Channel.class), anyString(), anyString(), anyBoolean());
    }

    @Test
    public void appSecretKeyWithoutAppSecretTest() {
        String secret = APP_SECRET_KEY + KEY_VALUE_DELIMITER;
        AppCenter.start(mApplication, secret, DummyService.class);
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), isNull(String.class), isNull(String.class), eq(true));
    }

    @Test
    public void appSecretWithKeyValueDelimiter() {
        String secret = KEY_VALUE_DELIMITER + DUMMY_APP_SECRET;
        AppCenter.start(mApplication, secret, DummyService.class);
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), isNull(String.class), isNull(String.class), eq(true));
    }

    @Test
    public void appSecretWithPairDelimiterAfter() {
        String secret = DUMMY_APP_SECRET + PAIR_DELIMITER;
        AppCenter.start(mApplication, secret, DummyService.class);
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
    }

    @Test
    public void appSecretWithPairDelimiterBefore() {
        String secret = PAIR_DELIMITER + DUMMY_APP_SECRET;
        AppCenter.start(mApplication, secret, DummyService.class);
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
    }

    @Test
    public void appSecretWithTargetTokenTest() {
        String secret = DUMMY_APP_SECRET + PAIR_DELIMITER + DUMMY_TARGET_TOKEN_STRING;
        AppCenter.start(mApplication, secret, DummyService.class);
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
    }

    @Test
    public void keyedAppSecretTest() {
        String secret = APP_SECRET_KEY + KEY_VALUE_DELIMITER + DUMMY_APP_SECRET;
        AppCenter.start(mApplication, secret, DummyService.class);
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
    }

    @Test
    public void keyedAppSecretWithDelimiterTest() {
        String secret = APP_SECRET_KEY + KEY_VALUE_DELIMITER + DUMMY_APP_SECRET + PAIR_DELIMITER;
        AppCenter.start(mApplication, secret, DummyService.class);
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
    }

    @Test
    public void keyedAppSecretWithTargetTokenTest() {
        String secret = APP_SECRET_KEY + KEY_VALUE_DELIMITER + DUMMY_APP_SECRET + PAIR_DELIMITER +
                DUMMY_TARGET_TOKEN_STRING;
        AppCenter.start(mApplication, secret, DummyService.class);
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
    }

    @Test
    public void targetTokenTest() {
        AppCenter.start(mApplication, DUMMY_TARGET_TOKEN_STRING, DummyService.class, AnotherDummyService.class);
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), isNull(String.class), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
        verify(AnotherDummyService.getInstance(), never()).onStarted(any(Context.class), any(Channel.class), isNull(String.class), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
    }

    @Test
    public void targetTokenWithAppSecretTest() {
        String secret = DUMMY_TARGET_TOKEN_STRING + PAIR_DELIMITER +
                DUMMY_APP_SECRET;
        AppCenter.start(mApplication, secret, DummyService.class);
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
    }

    @Test
    public void targetTokenWithUnKeyedAppSecretTest() {
        String secret = DUMMY_TARGET_TOKEN_STRING + PAIR_DELIMITER +
                APP_SECRET_KEY + KEY_VALUE_DELIMITER + DUMMY_APP_SECRET;
        AppCenter.start(mApplication, secret, DummyService.class);
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
    }

    @Test
    public void unknownKeyTest() {
        String secret = DUMMY_APP_SECRET + PAIR_DELIMITER +
                DUMMY_TARGET_TOKEN_STRING + PAIR_DELIMITER +
                "unknown" + KEY_VALUE_DELIMITER + "value";
        AppCenter.start(mApplication, secret, DummyService.class);
        assertTrue(AppCenter.isEnabled().get());
        verify(DummyService.getInstance()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
    }

    @Test
    public void duplicateServiceTest() {
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class, DummyService.class);

        /* Verify that only one service has been loaded and configured. */
        verify(DummyService.getInstance()).onStarted(notNull(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(String.class), eq(true));
        assertEquals(1, AppCenter.getInstance().getServices().size());
    }

    @Test
    public void setWrapperSdkTest() {

        /* Call method. */
        WrapperSdk wrapperSdk = new WrapperSdk();
        AppCenter.setWrapperSdk(wrapperSdk);

        /* Check propagation. */
        verifyStatic();
        DeviceInfoHelper.setWrapperSdk(wrapperSdk);

        /* Since the channel was not created when setting wrapper, no need to refresh channel after start. */
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        verify(mChannel, never()).invalidateDeviceCache();

        /* Update wrapper SDK and check channel refreshed. */
        wrapperSdk = new WrapperSdk();
        AppCenter.setWrapperSdk(wrapperSdk);
        verify(mChannel).invalidateDeviceCache();
    }

    @Test
    public void setDefaultLogLevelRelease() {
        mApplicationInfo.flags = 0;
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        verifyStatic(never());
        AppCenterLog.setLogLevel(anyInt());
    }

    @Test
    public void setDefaultLogLevelDebug() {
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        verifyStatic();
        AppCenterLog.setLogLevel(android.util.Log.WARN);
    }

    @Test
    public void doNotSetDefaultLogLevel() {
        AppCenter.setLogLevel(android.util.Log.VERBOSE);
        verifyStatic();
        AppCenterLog.setLogLevel(android.util.Log.VERBOSE);
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        verifyStatic(never());
        AppCenterLog.setLogLevel(android.util.Log.WARN);
    }

    @Test
    public void setLogUrl() {

        /* Change log URL before start. */
        String logUrl = "http://mock";
        AppCenter.setLogUrl(logUrl);

        /* No effect for now. */
        verify(mChannel, never()).setLogUrl(logUrl);

        /* Start should propagate the log URL. */
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        verify(mChannel).setLogUrl(logUrl);

        /* Change it after, should work immediately. */
        logUrl = "http://mock2";
        AppCenter.setLogUrl(logUrl);
        verify(mChannel).setLogUrl(logUrl);
    }

    @Test
    public void getSdkVersionTest() {
        assertEquals(BuildConfig.VERSION_NAME, AppCenter.getSdkVersion());
    }

    @Test
    public void setCustomPropertiesTest() throws Exception {

        /* Configure mocking. */
        CustomPropertiesLog log = mock(CustomPropertiesLog.class);
        whenNew(CustomPropertiesLog.class).withAnyArguments().thenReturn(log);

        /* Call before start is forbidden. */
        AppCenter.setCustomProperties(new CustomProperties().clear("test"));
        verify(mChannel, never()).enqueue(eq(log), eq(CORE_GROUP));
        verifyStatic(times(1));
        AppCenterLog.error(eq(LOG_TAG), anyString());

        /* Start. */
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);

        /* Set null. */
        AppCenter.setCustomProperties(null);
        verify(mChannel, never()).enqueue(eq(log), eq(CORE_GROUP));
        verifyStatic(times(2));
        AppCenterLog.error(eq(LOG_TAG), anyString());

        /* Set empty. */
        CustomProperties empty = new CustomProperties();
        AppCenter.setCustomProperties(empty);
        verify(mChannel, never()).enqueue(eq(log), eq(CORE_GROUP));
        verifyStatic(times(3));
        AppCenterLog.error(eq(LOG_TAG), anyString());

        /* Set normal. */
        CustomProperties properties = new CustomProperties();
        properties.set("test", "test");
        AppCenter.setCustomProperties(properties);
        verify(log).setProperties(eq(properties.getProperties()));
        verify(mChannel).enqueue(eq(log), eq(CORE_GROUP));

        /* Call after disabled triggers an error. */
        AppCenter.setEnabled(false);
        AppCenter.setCustomProperties(properties);
        verifyStatic(times(4));
        AppCenterLog.error(eq(LOG_TAG), anyString());

        /* No more log enqueued. */
        verify(mChannel).enqueue(eq(log), eq(CORE_GROUP));
    }

    @Test
    public void uncaughtExceptionHandler() {

        /* Setup mock App Center. */
        Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
        when(Thread.getDefaultUncaughtExceptionHandler()).thenReturn(defaultUncaughtExceptionHandler);
        AppCenter.configure(mApplication, DUMMY_APP_SECRET);
        UncaughtExceptionHandler handler = AppCenter.getInstance().getUncaughtExceptionHandler();
        assertNotNull(handler);
        assertEquals(defaultUncaughtExceptionHandler, handler.getDefaultUncaughtExceptionHandler());
        verifyStatic();
        Thread.setDefaultUncaughtExceptionHandler(eq(handler));

        /* Crash an verify. */
        Thread thread = mock(Thread.class);
        Throwable exception = mock(Throwable.class);
        handler.uncaughtException(thread, exception);
        verify(mChannel).shutdown();
        verify(defaultUncaughtExceptionHandler).uncaughtException(eq(thread), eq(exception));

        /* But we don't do it if App Center is disabled. */
        AppCenter.setEnabled(false);
        verifyStatic();
        Thread.setDefaultUncaughtExceptionHandler(eq(defaultUncaughtExceptionHandler));
        handler.uncaughtException(thread, exception);
        verify(mChannel, times(1)).shutdown();

        /* Try enabled without default thread handler: should shut down process. */
        when(Thread.getDefaultUncaughtExceptionHandler()).thenReturn(null);
        AppCenter.setEnabled(true);
        assertNull(handler.getDefaultUncaughtExceptionHandler());
        handler.uncaughtException(thread, exception);
        verifyStatic();
        ShutdownHelper.shutdown(10);
    }

    @Test
    public void uncaughtExceptionHandlerTimeout() throws Exception {

        /* Mock semaphore to time out. */
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                System.out.println(invocation.getArguments()[1]);
                return null;
            }
        }).when(AppCenterLog.class);
        AppCenterLog.error(eq(LOG_TAG), anyString());
        Semaphore semaphore = mock(Semaphore.class);
        whenNew(Semaphore.class).withAnyArguments().thenReturn(semaphore);
        when(semaphore.tryAcquire(anyLong(), any(TimeUnit.class))).thenReturn(false);

        /* Setup mock App Center. */
        Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
        when(Thread.getDefaultUncaughtExceptionHandler()).thenReturn(defaultUncaughtExceptionHandler);
        AppCenter.configure(mApplication, DUMMY_APP_SECRET);
        UncaughtExceptionHandler handler = AppCenter.getInstance().getUncaughtExceptionHandler();
        assertNotNull(handler);
        assertEquals(defaultUncaughtExceptionHandler, handler.getDefaultUncaughtExceptionHandler());
        verifyStatic();
        Thread.setDefaultUncaughtExceptionHandler(eq(handler));

        /* Simulate crash to shutdown channel. */
        Thread thread = mock(Thread.class);
        Throwable exception = mock(Throwable.class);
        handler.uncaughtException(thread, exception);

        /* We let channel shutdown even if we gave up with timeout it can happen later while process still running. */
        verify(mChannel).shutdown();

        /* Verify we still chain exception handlers correctly. */
        verify(defaultUncaughtExceptionHandler).uncaughtException(eq(thread), eq(exception));

        /* Verify we log an error on timeout. */
        verifyStatic();
        AppCenterLog.error(eq(LOG_TAG), anyString());
    }

    @Test
    public void uncaughtExceptionHandlerInterrupted() throws Exception {

        /* Mock semaphore to be interrupted while waiting. */
        Semaphore semaphore = mock(Semaphore.class);
        whenNew(Semaphore.class).withAnyArguments().thenReturn(semaphore);
        when(semaphore.tryAcquire(anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException());

        /* Setup mock App Center. */
        Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
        when(Thread.getDefaultUncaughtExceptionHandler()).thenReturn(defaultUncaughtExceptionHandler);
        AppCenter.configure(mApplication, DUMMY_APP_SECRET);
        UncaughtExceptionHandler handler = AppCenter.getInstance().getUncaughtExceptionHandler();
        assertNotNull(handler);
        assertEquals(defaultUncaughtExceptionHandler, handler.getDefaultUncaughtExceptionHandler());
        verifyStatic();
        Thread.setDefaultUncaughtExceptionHandler(eq(handler));

        /* Simulate crash to shutdown channel. */
        Thread thread = mock(Thread.class);
        Throwable exception = mock(Throwable.class);
        handler.uncaughtException(thread, exception);

        /* Channel still shut down in the thread, the interruption is in the waiting one. */
        verify(mChannel).shutdown();

        /* Verify we still chain exception handlers correctly. */
        verify(defaultUncaughtExceptionHandler).uncaughtException(eq(thread), eq(exception));

        /* Verify we log a warning on interruption. */
        verifyStatic();
        AppCenterLog.warn(eq(LOG_TAG), anyString(), argThat(new ArgumentMatcher<Throwable>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof InterruptedException;
            }
        }));
    }

    @Test
    public void addOneCollectorListenerOnStart() {
        AppCenter.start(mApplication, DUMMY_TARGET_TOKEN_STRING, DummyService.class);
        verify(mChannel).addListener(argThat(new ArgumentMatcher<Channel.Listener>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof OneCollectorChannelListener;
            }
        }));
    }
}
