/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import static com.microsoft.appcenter.AppCenter.APP_SECRET_KEY;
import static com.microsoft.appcenter.AppCenter.CORE_GROUP;
import static com.microsoft.appcenter.AppCenter.KEY_VALUE_DELIMITER;
import static com.microsoft.appcenter.AppCenter.LOG_TAG;
import static com.microsoft.appcenter.AppCenter.PAIR_DELIMITER;
import static com.microsoft.appcenter.Flags.DEFAULTS;
import static com.microsoft.appcenter.utils.PrefStorageConstants.KEY_ENABLED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import android.app.Activity;
import android.content.Context;

import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.channel.OneCollectorChannelListener;
import com.microsoft.appcenter.ingestion.models.StartServiceLog;
import com.microsoft.appcenter.ingestion.models.WrapperSdk;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.ApplicationLifecycleListener;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.PrefStorageConstants;
import com.microsoft.appcenter.utils.ShutdownHelper;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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
        verify(service).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(service);
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP), eq(DEFAULTS));
        verify(mChannel).setMaxStorageSize(AppCenter.DEFAULT_MAX_STORAGE_SIZE_IN_BYTES);
        List<String> services = new ArrayList<>();
        services.add(service.getServiceName());
        verify(mStartServiceLog).setServices(eq(services));
        verify(mStartServiceLog).oneCollectorEnabled(eq(false));
    }

    @Test
    public void useDummyServiceWhenDisablePersisted() {
        when(SharedPreferencesManager.getBoolean(KEY_ENABLED, true)).thenReturn(false);
        AppCenter appCenter = AppCenter.getInstance();
        DummyService service = DummyService.getInstance();
        AnotherDummyService anotherService = AnotherDummyService.getInstance();

        /* Start. */
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        assertFalse(AppCenter.isEnabled().get());
        assertEquals(1, AppCenter.getInstance().getServices().size());
        assertTrue(appCenter.getServices().contains(service));
        verify(mChannel, never()).enqueue(eq(mStartServiceLog), eq(CORE_GROUP), anyInt());

        /* Start another service. */
        AppCenter.start(AnotherDummyService.class);
        assertFalse(AppCenter.isEnabled().get());
        assertEquals(2, AppCenter.getInstance().getServices().size());
        assertTrue(appCenter.getServices().contains(anotherService));
        verify(mChannel, never()).enqueue(eq(mStartServiceLog), eq(CORE_GROUP), anyInt());

        /* Enable. */
        AppCenter.setEnabled(true);
        assertTrue(AppCenter.isEnabled().get());
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP), eq(DEFAULTS));
        List<String> services = new ArrayList<>();
        services.add(service.getServiceName());
        services.add(anotherService.getServiceName());
        verify(mStartServiceLog).setServices(eq(services));
        verify(mStartServiceLog).oneCollectorEnabled(eq(false));
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
        verify(service).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(service);
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP), eq(DEFAULTS));
        List<String> services = new ArrayList<>();
        services.add(service.getServiceName());
        verify(mStartServiceLog).setServices(eq(services));
        verify(mStartServiceLog).oneCollectorEnabled(eq(false));
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
        verify(service).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
        verify(service, never()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET + "a"), isNull(), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(service);
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP), eq(DEFAULTS));
        List<String> services = new ArrayList<>();
        services.add(service.getServiceName());
        verify(mStartServiceLog).setServices(eq(services));
        verify(mStartServiceLog).oneCollectorEnabled(eq(false));
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
        verify(service).onStarted(eq(mContext), any(Channel.class), isNull(), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
        verify(service, never()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(service);
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP), eq(DEFAULTS));
        List<String> services = new ArrayList<>();
        services.add(service.getServiceName());
        verify(mStartServiceLog).setServices(eq(services));
        verify(mStartServiceLog).oneCollectorEnabled(eq(true));
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
        verify(service, never()).onStarted(any(Context.class), any(Channel.class), isNull(), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
        verify(service).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(service);
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP), eq(DEFAULTS));
        List<String> services = new ArrayList<>();
        services.add(service.getServiceName());
        verify(mStartServiceLog).setServices(eq(services));
        verify(mStartServiceLog).oneCollectorEnabled(eq(false));
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
        verify(service).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
        verify(service, never()).onStarted(any(Context.class), any(Channel.class), eq(DUMMY_APP_SECRET + "a"), isNull(), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(service);
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP), eq(DEFAULTS));
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
        verify(DummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(DummyService.getInstance());

        /* Verify second service started. */
        assertTrue(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));
        verify(AnotherDummyService.getInstance()).getLogFactories();
        verify(AnotherDummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(AnotherDummyService.getInstance());

        /* Verify start service log is sent. */
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP), eq(DEFAULTS));
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
            verify(DummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
            verify(mApplication).registerActivityLifecycleCallbacks(DummyService.getInstance());
        }
        {
            assertTrue(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));
            verify(AnotherDummyService.getInstance()).getLogFactories();
            verify(AnotherDummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
            verify(mApplication).registerActivityLifecycleCallbacks(AnotherDummyService.getInstance());
        }
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP), eq(DEFAULTS));
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
            verify(DummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
            verify(mApplication).registerActivityLifecycleCallbacks(DummyService.getInstance());
        }
        {
            assertTrue(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));
            verify(AnotherDummyService.getInstance()).getLogFactories();
            verify(AnotherDummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
            verify(mApplication).registerActivityLifecycleCallbacks(AnotherDummyService.getInstance());
        }
        verify(mChannel, times(2)).enqueue(any(StartServiceLog.class), eq(CORE_GROUP), eq(DEFAULTS));
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
            verify(DummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
            verify(mApplication).registerActivityLifecycleCallbacks(DummyService.getInstance());
        }
        {
            assertTrue(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));
            verify(AnotherDummyService.getInstance()).getLogFactories();
            verify(AnotherDummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
            verify(mApplication).registerActivityLifecycleCallbacks(AnotherDummyService.getInstance());
        }
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP), eq(DEFAULTS));
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
            verify(DummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
            verify(mApplication).registerActivityLifecycleCallbacks(DummyService.getInstance());
        }
        {
            assertTrue(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));
            verify(AnotherDummyService.getInstance()).getLogFactories();
            verify(AnotherDummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
            verify(mApplication).registerActivityLifecycleCallbacks(AnotherDummyService.getInstance());
        }
        verify(mChannel, times(2)).enqueue(any(StartServiceLog.class), eq(CORE_GROUP), eq(DEFAULTS));
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
        verify(service).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(service);

        /* Start twice, this call is ignored. */
        AppCenter.start(DummyService.class);

        /* Verify that single service has been loaded and configured (only once interaction). */
        assertEquals(1, AppCenter.getInstance().getServices().size());
        verify(service).getLogFactories();
        verify(service).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
        verify(mApplication).registerActivityLifecycleCallbacks(service);
        verify(mChannel).enqueue(eq(mStartServiceLog), eq(CORE_GROUP), eq(DEFAULTS));
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
        verifyStatic(AppCenterLog.class);
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
        verify(dummyService).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
        verify(anotherDummyService).getLogFactories();
        verify(anotherDummyService).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
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
        verifyStatic(AppCenterLog.class, times(3));
        AppCenterLog.error(eq(LOG_TAG), anyString());
    }

    @Test
    public void disablePersisted() {
        when(SharedPreferencesManager.getBoolean(KEY_ENABLED, true)).thenReturn(false);
        when(SharedPreferencesManager.getBoolean(AnotherDummyService.getInstance().getEnabledPreferenceKey(), true)).thenReturn(false);
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
        when(SharedPreferencesManager.getBoolean(KEY_ENABLED, true)).thenReturn(true);

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
        when(SharedPreferencesManager.getBoolean(KEY_ENABLED, true)).thenReturn(false);
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
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(LOG_TAG), anyString(), any(NoSuchMethodException.class));
    }

    @Test
    public void nullApplicationTest() {

        /* First verify start from application. */
        AppCenter.start(null, DUMMY_APP_SECRET, DummyService.class);
        verify(DummyService.getInstance(), never()).onStarted(any(Context.class), any(Channel.class), anyString(), anyString(), anyBoolean());
        verifyStatic(AppCenterLog.class);
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
        verify(DummyService.getInstance(), never()).onStarted(any(Context.class), any(Channel.class), isNull(), isNull(), eq(true));
    }

    @Test
    public void startWithoutAppSecretTest() {

        /* Start App Center without an app secret. */
        AppCenter.start(mApplication, DummyService.class);
        checkStartedWithoutAppSecret();
    }

    @Test
    public void configureWithDeviceProtectedStorage() {
        when(ApplicationContextUtils.isDeviceProtectedStorage(mContext)).thenReturn(true);

        /* Configure App Center. */
        AppCenter.configure(mApplication);

        /* App Center is configured that way. */
        assertTrue(AppCenter.isConfigured());

        /* Verify warning call. */
        verifyStatic(AppCenterLog.class);
        AppCenterLog.warn(eq(LOG_TAG), anyString());
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
        verify(DummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), isNull(), isNull(), eq(true));

        /* We must not be able to reconfigure app secret from null/empty state. */
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class, AnotherDummyService.class);
        AppCenter.start(mApplication, (String) null, DummyService.class, AnotherDummyService.class);
        AppCenter.start(mApplication, "", DummyService.class, AnotherDummyService.class);

        /* Verify start not called again (1 total call). */
        verify(DummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), any(), any(), anyBoolean());

        /* And not updated either. */
        verify(DummyService.getInstance(), never()).onConfigurationUpdated(anyString(), anyString());

        /* Verify the second service was not started as was part of second secret configuration. */
        verify(AnotherDummyService.getInstance(), never()).onStarted(any(Context.class), any(Channel.class), anyString(), anyString(), anyBoolean());
    }

    @Test
    public void appSecretKeyWithoutAppSecretTest() {
        String secret = APP_SECRET_KEY + KEY_VALUE_DELIMITER;
        AppCenter.start(mApplication, secret, DummyService.class);
        verify(DummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), isNull(), isNull(), eq(true));
    }

    @Test
    public void appSecretWithKeyValueDelimiter() {
        String secret = KEY_VALUE_DELIMITER + DUMMY_APP_SECRET;
        AppCenter.start(mApplication, secret, DummyService.class);
        verify(DummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), isNull(), isNull(), eq(true));
    }

    @Test
    public void appSecretWithPairDelimiterAfter() {
        String secret = DUMMY_APP_SECRET + PAIR_DELIMITER;
        AppCenter.start(mApplication, secret, DummyService.class);
        verify(DummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
    }

    @Test
    public void appSecretWithPairDelimiterBefore() {
        String secret = PAIR_DELIMITER + DUMMY_APP_SECRET;
        AppCenter.start(mApplication, secret, DummyService.class);
        verify(DummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
    }

    @Test
    public void appSecretWithTargetTokenTest() {
        String secret = DUMMY_APP_SECRET + PAIR_DELIMITER + DUMMY_TARGET_TOKEN_STRING;
        AppCenter.start(mApplication, secret, DummyService.class);
        verify(DummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
    }

    @Test
    public void keyedAppSecretTest() {
        String secret = APP_SECRET_KEY + KEY_VALUE_DELIMITER + DUMMY_APP_SECRET;
        AppCenter.start(mApplication, secret, DummyService.class);
        verify(DummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
    }

    @Test
    public void keyedAppSecretWithDelimiterTest() {
        String secret = APP_SECRET_KEY + KEY_VALUE_DELIMITER + DUMMY_APP_SECRET + PAIR_DELIMITER;
        AppCenter.start(mApplication, secret, DummyService.class);
        verify(DummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
    }

    @Test
    public void keyedAppSecretWithTargetTokenTest() {
        String secret = APP_SECRET_KEY + KEY_VALUE_DELIMITER + DUMMY_APP_SECRET + PAIR_DELIMITER +
                DUMMY_TARGET_TOKEN_STRING;
        AppCenter.start(mApplication, secret, DummyService.class);
        verify(DummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
    }

    @Test
    public void targetTokenTest() {
        AppCenter.start(mApplication, DUMMY_TARGET_TOKEN_STRING, DummyService.class, AnotherDummyService.class);
        verify(DummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), isNull(), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
        verify(AnotherDummyService.getInstance(), never()).onStarted(eq(mContext), any(Channel.class), isNull(), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
    }

    @Test
    public void targetTokenWithAppSecretTest() {
        String secret = DUMMY_TARGET_TOKEN_STRING + PAIR_DELIMITER +
                DUMMY_APP_SECRET;
        AppCenter.start(mApplication, secret, DummyService.class);
        verify(DummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
    }

    @Test
    public void targetTokenWithUnKeyedAppSecretTest() {
        String secret = DUMMY_TARGET_TOKEN_STRING + PAIR_DELIMITER +
                APP_SECRET_KEY + KEY_VALUE_DELIMITER + DUMMY_APP_SECRET;
        AppCenter.start(mApplication, secret, DummyService.class);
        verify(DummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
    }

    @Test
    public void unknownKeyTest() {
        String secret = DUMMY_APP_SECRET + PAIR_DELIMITER +
                DUMMY_TARGET_TOKEN_STRING + PAIR_DELIMITER +
                "unknown" + KEY_VALUE_DELIMITER + "value";
        AppCenter.start(mApplication, secret, DummyService.class);
        assertTrue(AppCenter.isEnabled().get());
        verify(DummyService.getInstance()).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), eq(DUMMY_TRANSMISSION_TARGET_TOKEN), eq(true));
    }

    @Test
    public void checkStartHandlerWhenDesableRunnableIsNull() {
        String secret = DUMMY_APP_SECRET + PAIR_DELIMITER +
                DUMMY_TARGET_TOKEN_STRING + PAIR_DELIMITER +
                "unknown" + KEY_VALUE_DELIMITER + "value";

        // Start App Center.
        AppCenter.start(mApplication, secret, DummyService.class);
        assertTrue(AppCenter.isEnabled().get());

        // Disable App Center.
        AppCenter.setEnabled(false);
        assertFalse(AppCenter.isEnabled().get());

        // Call runnable with disabledRunnable is null.
        Runnable mockRunnable = mock(Runnable.class);
        AppCenter.getInstance().getAppCenterHandler().post(mockRunnable, null);
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(LOG_TAG), eq("App Center SDK is disabled."));

        // Call App Center start with null application and verify than anything happening.
        AppCenter.getInstance().resetApplication();
        AppCenter.getInstance().getAppCenterHandler().post(mockRunnable, null);
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(LOG_TAG), eq("App Center hasn't been configured. You need to call AppCenter.start with appSecret or AppCenter.configure first."));
    }

    @Test
    public void duplicateServiceTest() {
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class, DummyService.class);

        /* Verify that only one service has been loaded and configured. */
        verify(DummyService.getInstance()).onStarted(notNull(), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));
        assertEquals(1, AppCenter.getInstance().getServices().size());
    }

    @Test
    public void setWrapperSdkTest() {

        /* Call method. */
        WrapperSdk wrapperSdk = new WrapperSdk();
        AppCenter.setWrapperSdk(wrapperSdk);

        /* Check propagation. */
        verifyStatic(DeviceInfoHelper.class);
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
    public void setCountryCode() {

        /* Set country code. */
        String expectedCountryCode = "aa";
        AppCenter.setCountryCode(expectedCountryCode);

        /* Check that method was called. */
        verifyStatic(DeviceInfoHelper.class);
        DeviceInfoHelper.setCountryCode(eq(expectedCountryCode));
    }

    @Test
    public void setDataResidencyRegion() {

        /* Set country code. */
        String expectedDataResidencyRegion = "rg";
        AppCenter.setDataResidencyRegion(expectedDataResidencyRegion);

        assertEquals(AppCenter.getDataResidencyRegion(), expectedDataResidencyRegion);
    }

    @Test
    public void setDefaultLogLevelRelease() {
        mApplicationInfo.flags = 0;
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        verifyStatic(AppCenterLog.class, never());
        AppCenterLog.setLogLevel(anyInt());
    }

    @Test
    public void setDefaultLogLevelDebug() {
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        verifyStatic(AppCenterLog.class);
        AppCenterLog.setLogLevel(android.util.Log.WARN);
    }

    @Test
    public void doNotSetDefaultLogLevel() {
        AppCenter.setLogLevel(android.util.Log.VERBOSE);
        verifyStatic(AppCenterLog.class);
        AppCenterLog.setLogLevel(android.util.Log.VERBOSE);
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        verifyStatic(AppCenterLog.class, never());
        AppCenterLog.setLogLevel(android.util.Log.WARN);
    }

    @Test
    public void setLogUrl() throws Exception {
        OneCollectorChannelListener listener = mock(OneCollectorChannelListener.class);
        whenNew(OneCollectorChannelListener.class).withAnyArguments().thenReturn(listener);

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
        verify(listener, never()).setLogUrl(anyString());
    }

    @Test
    public void setOneCollectorUrlWhenTargetTokenUsed() throws Exception {
        OneCollectorChannelListener listener = mock(OneCollectorChannelListener.class);
        whenNew(OneCollectorChannelListener.class).withAnyArguments().thenReturn(listener);

        /* Change log URL before start. */
        String logUrl = "http://mock";
        AppCenter.setLogUrl(logUrl);

        /* No effect for now. */
        verify(listener, never()).setLogUrl(logUrl);

        /* Start should propagate the log URL without App Secret. */
        AppCenter.start(mApplication, DUMMY_TARGET_TOKEN_STRING, DummyService.class);
        verify(listener).setLogUrl(logUrl);

        /* Change it after, should work immediately. */
        logUrl = "http://mock2";
        AppCenter.setLogUrl(logUrl);
        verify(listener).setLogUrl(logUrl);
        verify(mChannel, never()).setLogUrl(anyString());
    }

    @Test
    public void setOneCollectorUrlWhenNoSecretUsed() throws Exception {
        OneCollectorChannelListener listener = mock(OneCollectorChannelListener.class);
        whenNew(OneCollectorChannelListener.class).withAnyArguments().thenReturn(listener);

        /* Change log URL before start. */
        String logUrl = "http://mock";
        AppCenter.setLogUrl(logUrl);

        /* No effect for now. */
        verify(listener, never()).setLogUrl(logUrl);

        /* Start should propagate the log URL without App Secret. */
        AppCenter.start(mApplication, DummyService.class);
        verify(listener).setLogUrl(logUrl);

        /* Change it after, should work immediately. */
        logUrl = "http://mock2";
        AppCenter.setLogUrl(logUrl);
        verify(listener).setLogUrl(logUrl);
        verify(mChannel, never()).setLogUrl(anyString());
    }

    @Test
    public void getSdkVersionTest() {
        assertEquals(BuildConfig.VERSION_NAME, AppCenter.getSdkVersion());
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
        verifyStatic(Thread.class);
        Thread.setDefaultUncaughtExceptionHandler(eq(handler));

        /* Crash an verify. */
        Thread thread = mock(Thread.class);
        Throwable exception = mock(Throwable.class);
        handler.uncaughtException(thread, exception);
        verify(mChannel).shutdown();
        verify(defaultUncaughtExceptionHandler).uncaughtException(eq(thread), eq(exception));

        /* But we don't do it if App Center is disabled. */
        AppCenter.setEnabled(false);
        verifyStatic(Thread.class);
        Thread.setDefaultUncaughtExceptionHandler(eq(defaultUncaughtExceptionHandler));
        handler.uncaughtException(thread, exception);
        verify(mChannel, times(1)).shutdown();

        /* Try enabled without default thread handler: should shut down process. */
        when(Thread.getDefaultUncaughtExceptionHandler()).thenReturn(null);
        AppCenter.setEnabled(true);
        assertNull(handler.getDefaultUncaughtExceptionHandler());
        handler.uncaughtException(thread, exception);
        verifyStatic(ShutdownHelper.class);
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
        verifyStatic(Thread.class);
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
        verifyStatic(AppCenterLog.class);
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
        verifyStatic(Thread.class);
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
        verifyStatic(AppCenterLog.class);
        AppCenterLog.warn(eq(LOG_TAG), anyString(), isA(InterruptedException.class));
    }

    @Test
    public void addOneCollectorListenerOnStart() {
        AppCenter.start(mApplication, DUMMY_TARGET_TOKEN_STRING, DummyService.class);
        verify(mChannel).addListener(isA(OneCollectorChannelListener.class));
    }

    @Test
    public void useApplicationLifecycleListener() {

        /* Capture ApplicationLifecycleListener. */
        ArgumentCaptor<ApplicationLifecycleListener> lifecycleListenerCaptor = ArgumentCaptor.forClass(ApplicationLifecycleListener.class);
        doNothing().when(mApplication).registerActivityLifecycleCallbacks(lifecycleListenerCaptor.capture());

        /* Start App Center. */
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);

        /* Verify that the service started. */
        DummyService service = DummyService.getInstance();
        verify(service).onStarted(eq(mContext), any(Channel.class), eq(DUMMY_APP_SECRET), isNull(), eq(true));

        /* Verify that the listener is subscribed to application events. */
        verify(mApplication).registerActivityLifecycleCallbacks(isA(ApplicationLifecycleListener.class));
        ApplicationLifecycleListener lifecycleListener = lifecycleListenerCaptor.getAllValues().get(0);

        /* Check enter foreground. */
        Activity mockActivity = mock(Activity.class);
        lifecycleListener.onActivityStarted(mockActivity);
        verify(service).onApplicationEnterForeground();

        /* Check enter background. */
        lifecycleListener.onActivityStopped(mockActivity);
        verify(service).onApplicationEnterBackground();
    }

    @Test
    public void setNetworkRequestValueWhenChannelEnableOrDisable() {

        /* Configure App Center. */
        AppCenter.configure(mApplication);
        AppCenter.getInstance().setChannel(mChannel);

        /* Verify that channel was enabled. */
        verify(mChannel).setEnabled(true);

        /* Verify that network requests are allowed. */
        assertTrue(AppCenter.isNetworkRequestsAllowed());

        /* Disable App Center. */
        AppCenter.setEnabled(false);

        /* Verify that channel was disable with disabled App Center and allowed network requests. */
        verify(mChannel).setEnabled(false);

        /* Disallow network request value. */
        AppCenter.setNetworkRequestsAllowed(false);

        /* Verify that channel still disable with disabled App Center and disallowed network requests. */
        verify(mChannel).setEnabled(false);

        /* Enable App Center. */
        AppCenter.setEnabled(true);

        /* Verify that channel still disable with enabled App Center and disallowed network requests. */
        verify(mChannel).setEnabled(false);

        /* Allow network request value. */
        AppCenter.setNetworkRequestsAllowed(true);

        /* Verify that channel was enabled with enabled App Center and allowed network requests. */
        verify(mChannel, times(2)).setEnabled(true);
    }

    @Test
    public void setNetworkRequestValue() {

        /* Configure App Center. */
        AppCenter.configure(mApplication);

        /* Check that this value wasn't saved to SharedPreferences. */
        verifyStatic(SharedPreferencesManager.class, never());
        SharedPreferencesManager.putBoolean(eq(PrefStorageConstants.ALLOWED_NETWORK_REQUEST), anyBoolean());

        /* Set channel. */
        AppCenter.getInstance().setChannel(mChannel);

        /* Disallow network request value. */
        AppCenter.setNetworkRequestsAllowed(false);

        /* Verify that value was passed to channel. */
        verify(mChannel).setNetworkRequests(false);

        /* Allow network request value. */
        AppCenter.setNetworkRequestsAllowed(true);

        /* Verify that value was passed to channel. */
        verify(mChannel).setNetworkRequests(true);
    }

    @Test
    public void setSameNetworkRequestsAllowedValue() {

        /* Configure App Center. */
        AppCenter.configure(mApplication);
        AppCenter.getInstance().setChannel(null);
        verifyStatic(SharedPreferencesManager.class, never());
        SharedPreferencesManager.putBoolean(eq(PrefStorageConstants.ALLOWED_NETWORK_REQUEST), anyBoolean());

        /* Verify that default value is true. */
        assertTrue(AppCenter.isNetworkRequestsAllowed());

        /* Allow network request again. */
        AppCenter.setNetworkRequestsAllowed(true);

        /* Verify that value wasn't saved. */
        verifyStatic(SharedPreferencesManager.class, never());
        SharedPreferencesManager.putBoolean(eq(PrefStorageConstants.ALLOWED_NETWORK_REQUEST), anyBoolean());

        /* Disallow network requests. */
        AppCenter.setNetworkRequestsAllowed(false);
        assertFalse(AppCenter.isNetworkRequestsAllowed());
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.putBoolean(eq(PrefStorageConstants.ALLOWED_NETWORK_REQUEST), anyBoolean());

        /* Disallow network again requests. */
        AppCenter.setNetworkRequestsAllowed(false);
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.putBoolean(eq(PrefStorageConstants.ALLOWED_NETWORK_REQUEST), anyBoolean());
    }

    @Test
    public void setNetworkRequestsAllowedValueWhenAppCenterIsNotConfigured() {

        /* Verify that network requests are allowed by default.  */
        Assert.assertTrue(AppCenter.isNetworkRequestsAllowed());

        /* Disallow network requests. */
        AppCenter.setNetworkRequestsAllowed(false);

        /* Verify that network saved previous value. */
        Assert.assertFalse(AppCenter.isNetworkRequestsAllowed());

        /* Check that this value wasn't saved to SharedPreferences. */
        verifyStatic(SharedPreferencesManager.class, never());
        SharedPreferencesManager.putBoolean(eq(PrefStorageConstants.ALLOWED_NETWORK_REQUEST), eq(false));

        /* Verify that network requests value was saved to local variable. */
        Assert.assertFalse(AppCenter.isNetworkRequestsAllowed());
        verifyStatic(SharedPreferencesManager.class, never());
        SharedPreferencesManager.getBoolean(eq(PrefStorageConstants.ALLOWED_NETWORK_REQUEST), eq(true));

        /* Configure App Center. */
        AppCenter.configure(mApplication);

        /* Verify that network requests value was saved to SharedPreferences. */
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.putBoolean(eq(PrefStorageConstants.ALLOWED_NETWORK_REQUEST), eq(false));

        /* Verify that network requests value was saved to SharedPreferences. */
        Assert.assertFalse(AppCenter.isNetworkRequestsAllowed());
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.getBoolean(eq(PrefStorageConstants.ALLOWED_NETWORK_REQUEST), eq(false));
    }
}
