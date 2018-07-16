package com.microsoft.appcenter;

import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.InstrumentationRegistryHelper;

/**
 * Instrumentation utilities.
 */
class ServiceInstrumentationUtils {

    /**
     * Value to indicate that all services should be disabled.
     */
    @VisibleForTesting
    static final String DISABLE_ALL_SERVICES = "All";

    /**
     * Name of the variable used to indicate services that should be disabled (typically for test
     * cloud).
     */
    @VisibleForTesting
    static final String DISABLE_SERVICES = "APP_CENTER_DISABLE";

    /**
     * Check if a service is disabled via instrumentation arguments.
     *
     * @param serviceName service name.
     * @return true if service is disabled via instrumentation, false otherwise.
     */
    static boolean isServiceDisabledByInstrumentation(String serviceName) {
        try {
            Bundle arguments = InstrumentationRegistryHelper.getArguments();
            String disableServices = arguments.getString(DISABLE_SERVICES);
            if (disableServices == null) {
                return false;
            }
            String[] disableServicesList = disableServices.split(",");
            for (String service : disableServicesList) {
                service = service.trim();
                if (service.equals(DISABLE_ALL_SERVICES) || service.equals(serviceName)) {
                    return true;
                }
            }
            return false;
        } catch (LinkageError | IllegalStateException e) {
            AppCenterLog.debug(AppCenter.LOG_TAG, "Cannot read instrumentation variables in a non-test environment.");
            return false;
        }
    }
}
