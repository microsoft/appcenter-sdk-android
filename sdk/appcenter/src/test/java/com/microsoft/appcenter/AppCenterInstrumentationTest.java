package com.microsoft.appcenter;

import com.microsoft.appcenter.utils.InstrumentationRegistryHelper;

import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.doThrow;

public class AppCenterInstrumentationTest extends AbstractAppCenterTest {

    @Test
    public void disableServicesViaInstrumentation() {

        /* Verify that when variable is not set, services are started. */
        addArgumentToRegistry(ServiceInstrumentationUtils.DISABLE_SERVICES, null);
        AppCenter.start(mApplication, "app-secret", DummyService.class, AnotherDummyService.class);
        assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
        assertTrue(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));

        /* Verify that when "All" is set, no service is started. */
        addArgumentToRegistry(ServiceInstrumentationUtils.DISABLE_SERVICES, ServiceInstrumentationUtils.DISABLE_ALL_SERVICES);
        AppCenter.unsetInstance();
        AppCenter.start(mApplication, "app-secret", DummyService.class, AnotherDummyService.class);
        assertFalse(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
        assertFalse(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));

        /* Verify single service can be disabled. */
        AppCenter.unsetInstance();
        addArgumentToRegistry(ServiceInstrumentationUtils.DISABLE_SERVICES, DummyService.getInstance().getServiceName());
        AppCenter.start(mApplication, "app-secret", DummyService.class, AnotherDummyService.class);
        assertFalse(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
        assertTrue(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));

        /* Verify that multiple services can be disabled. */
        addArgumentToRegistry(ServiceInstrumentationUtils.DISABLE_SERVICES, DummyService.getInstance().getServiceName()
                + ",anotherService," + AnotherDummyService.getInstance().getServiceName());
        AppCenter.unsetInstance();
        AppCenter.start(mApplication, "app-secret", DummyService.class, AnotherDummyService.class);
        assertFalse(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
        assertFalse(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));

        /* Repeat last test with whitespace. */
        addArgumentToRegistry(ServiceInstrumentationUtils.DISABLE_SERVICES, " " + DummyService.getInstance().getServiceName()
                + " , anotherService, " + AnotherDummyService.getInstance().getServiceName() + " ");
        AppCenter.unsetInstance();
        AppCenter.start(mApplication, "app-secret", DummyService.class, AnotherDummyService.class);
        assertFalse(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
        assertFalse(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));
    }

    @Test
    public void doNotDisableServicesInNonTestEnvironment() {

        /* Throw IllegalAccessError. */
        doThrow(new IllegalAccessError()).when(InstrumentationRegistryHelper.class);
        InstrumentationRegistryHelper.getArguments();
        AppCenter.start(mApplication, "app-secret", DummyService.class, AnotherDummyService.class);
        assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
        assertTrue(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));

        /* Throw NoClassDefFoundError. */
        AppCenter.unsetInstance();
        doThrow(new NoClassDefFoundError()).when(InstrumentationRegistryHelper.class);
        InstrumentationRegistryHelper.getArguments();
        AppCenter.start(mApplication, "app-secret", DummyService.class, AnotherDummyService.class);
        assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
        assertTrue(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));

        /* Throw NoSuchMethodError. */
        AppCenter.unsetInstance();
        doThrow(new NoSuchMethodError()).when(InstrumentationRegistryHelper.class);
        InstrumentationRegistryHelper.getArguments();
        AppCenter.start(mApplication, "app-secret", DummyService.class, AnotherDummyService.class);
        assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
        assertTrue(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));

        /* Throw LinkageError. */
        AppCenter.unsetInstance();
        doThrow(new LinkageError()).when(InstrumentationRegistryHelper.class);
        InstrumentationRegistryHelper.getArguments();
        AppCenter.start(mApplication, "app-secret", DummyService.class, AnotherDummyService.class);
        assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
        assertTrue(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));

        /* Throw IllegalStateException. */
        AppCenter.unsetInstance();
        doThrow(new IllegalStateException()).when(InstrumentationRegistryHelper.class);
        InstrumentationRegistryHelper.getArguments();
        AppCenter.start(mApplication, "app-secret", DummyService.class, AnotherDummyService.class);
        assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
        assertTrue(AppCenter.getInstance().getServices().contains(AnotherDummyService.getInstance()));
    }

    @Test
    public void disableServicesFromLibraryViaInstrumentation() {

        /* Verify that when variable is not set, services are started. */
        addArgumentToRegistry(ServiceInstrumentationUtils.DISABLE_SERVICES, null);
        AppCenter.startFromLibrary(mApplication, DummyService.class);
        assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));

        /* Verify that when "All" is set, no service is started. */
        addArgumentToRegistry(ServiceInstrumentationUtils.DISABLE_SERVICES, ServiceInstrumentationUtils.DISABLE_ALL_SERVICES);
        AppCenter.unsetInstance();
        AppCenter.startFromLibrary(mApplication, DummyService.class);
        assertFalse(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));

        /* Verify single service can be disabled. */
        AppCenter.unsetInstance();
        addArgumentToRegistry(ServiceInstrumentationUtils.DISABLE_SERVICES, DummyService.getInstance().getServiceName());
        AppCenter.startFromLibrary(mApplication, DummyService.class);
        assertFalse(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));

        /* Verify that multiple services can be disabled. */
        addArgumentToRegistry(ServiceInstrumentationUtils.DISABLE_SERVICES, DummyService.getInstance().getServiceName()
                + ",anotherService," + AnotherDummyService.getInstance().getServiceName());
        AppCenter.unsetInstance();
        AppCenter.startFromLibrary(mApplication, DummyService.class);
        assertFalse(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));

        /* Repeat last test with whitespace. */
        addArgumentToRegistry(ServiceInstrumentationUtils.DISABLE_SERVICES, " " + DummyService.getInstance().getServiceName()
                + " , anotherService, " + AnotherDummyService.getInstance().getServiceName() + " ");
        AppCenter.unsetInstance();
        AppCenter.startFromLibrary(mApplication, DummyService.class);
        assertFalse(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
    }

    @Test
    public void doNotDisableServicesFromLibraryInNonTestEnvironment() {

        /* Throw IllegalAccessError. */
        doThrow(new IllegalAccessError()).when(InstrumentationRegistryHelper.class);
        InstrumentationRegistryHelper.getArguments();
        AppCenter.startFromLibrary(mApplication, DummyService.class);
        assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));

        /* Throw NoClassDefFoundError. */
        AppCenter.unsetInstance();
        doThrow(new NoClassDefFoundError()).when(InstrumentationRegistryHelper.class);
        InstrumentationRegistryHelper.getArguments();
        AppCenter.startFromLibrary(mApplication, DummyService.class);
        assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));

        /* Throw NoSuchMethodError. */
        AppCenter.unsetInstance();
        doThrow(new NoSuchMethodError()).when(InstrumentationRegistryHelper.class);
        InstrumentationRegistryHelper.getArguments();
        AppCenter.startFromLibrary(mApplication, DummyService.class);
        assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));

        /* Throw LinkageError. */
        AppCenter.unsetInstance();
        doThrow(new LinkageError()).when(InstrumentationRegistryHelper.class);
        InstrumentationRegistryHelper.getArguments();
        AppCenter.startFromLibrary(mApplication, DummyService.class);
        assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));

        /* Throw IllegalStateException. */
        AppCenter.unsetInstance();
        doThrow(new IllegalStateException()).when(InstrumentationRegistryHelper.class);
        InstrumentationRegistryHelper.getArguments();
        AppCenter.startFromLibrary(mApplication, DummyService.class);
        assertTrue(AppCenter.getInstance().getServices().contains(DummyService.getInstance()));
    }
}
