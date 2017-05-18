package com.microsoft.azure.mobile.sasquatch.utils;

import android.support.v7.app.AppCompatActivity;

import com.microsoft.azure.mobile.crashes.CrashesListener;

/**
 * CrashesListener provider for jcenterDependency flavour.
 */
public class CrashesListenerProvider {
    public static CrashesListener provideCrashesListener(AppCompatActivity activity) {
        return new SasquatchCrashesListener(activity);
    }
}