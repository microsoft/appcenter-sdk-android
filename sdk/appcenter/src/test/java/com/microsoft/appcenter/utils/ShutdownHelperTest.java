package com.microsoft.appcenter.utils;

import android.os.Process;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings("unused")
@PrepareForTest({ShutdownHelper.class, Process.class})
public class ShutdownHelperTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Before
    public void setUp() {
        mockStatic(Process.class);
        mockStatic(System.class);
    }

    @Test
    public void shutdown() {

        /* Dummy coverage */
        new ShutdownHelper();

        /* Mock process id */
        when(Process.myPid()).thenReturn(123);

        ShutdownHelper.shutdown(999);
        verifyStatic();
        Process.killProcess(123);
        verifyStatic();
        System.exit(999);
    }
}