package com.microsoft.sonoma.core.utils;

import android.os.SystemClock;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("unused")
public class AndroidTimeSourceTest {

    @Test
    public void test() {
        AndroidTimeSource timeSource = new AndroidTimeSource();
        Assert.assertTrue(Math.abs(timeSource.currentTimeMillis() - System.currentTimeMillis()) <= 1);
        Assert.assertTrue(Math.abs(timeSource.elapsedRealtime() - SystemClock.elapsedRealtime()) <= 1);
    }
}
