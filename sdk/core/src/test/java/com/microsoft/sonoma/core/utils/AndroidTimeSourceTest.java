package com.microsoft.sonoma.core.utils;

import android.os.SystemClock;

import org.junit.Rule;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings("unused")
@PrepareForTest({AndroidTimeSource.class, SystemClock.class})
public class AndroidTimeSourceTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Test
    public void test() {
        mockStatic(System.class);
        mockStatic(SystemClock.class);
        when(System.currentTimeMillis()).thenReturn(1L);
        when(SystemClock.elapsedRealtime()).thenReturn(2L);
        AndroidTimeSource timeSource = new AndroidTimeSource();
        assertEquals(1L, timeSource.currentTimeMillis());
        assertEquals(2L, timeSource.elapsedRealtime());
    }
}
