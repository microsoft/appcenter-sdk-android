/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.util.Log;

import com.microsoft.appcenter.AppCenter;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.verification.VerificationMode;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest(Log.class)
public class AppCenterLogTest {

    private static void callLogs() {
        AppCenterLog.logAssert("my-tag", "error with my-tag");
        AppCenterLog.logAssert("my-tag", "error with my-tag with exception", new Exception());
        AppCenterLog.error("my-tag", "error with my-tag");
        AppCenterLog.error("my-tag", "error with my-tag with exception", new Exception());
        AppCenterLog.warn("my-tag", "warn with my-tag");
        AppCenterLog.warn("my-tag", "warn with my-tag with exception", new Exception());
        AppCenterLog.info("my-tag", "info with my-tag");
        AppCenterLog.info("my-tag", "info with my-tag with exception", new Exception());
        AppCenterLog.debug("my-tag", "debug with my-tag");
        AppCenterLog.debug("my-tag", "debug with my-tag with exception", new Exception());
        AppCenterLog.verbose("my-tag", "verbose with my-tag");
        AppCenterLog.verbose("my-tag", "verbose with my-tag with exception", new Exception());
    }

    private static void verifyAssert(VerificationMode verificationMode) {
        verifyStatic(verificationMode);
        Log.println(Log.ASSERT, "my-tag", "error with my-tag");
        verifyStatic(verificationMode);
        //noinspection WrongConstant
        Log.println(eq(Log.ASSERT), eq("my-tag"), eq("error with my-tag with exception\nmock stack trace"));
    }

    private static void verifyError(VerificationMode verificationMode) {
        verifyStatic(verificationMode);
        Log.e("my-tag", "error with my-tag");
        verifyStatic(verificationMode);
        Log.e(eq("my-tag"), eq("error with my-tag with exception"), any(Exception.class));
    }

    private static void verifyWarn(VerificationMode verificationMode) {
        verifyStatic(verificationMode);
        Log.w("my-tag", "warn with my-tag");
        verifyStatic(verificationMode);
        Log.w(eq("my-tag"), eq("warn with my-tag with exception"), any(Exception.class));
    }

    private static void verifyInfo(VerificationMode verificationMode) {
        verifyStatic(verificationMode);
        Log.i("my-tag", "info with my-tag");
        verifyStatic(verificationMode);
        Log.i(eq("my-tag"), eq("info with my-tag with exception"), any(Exception.class));
    }

    private static void verifyDebug(VerificationMode verificationMode) {
        verifyStatic(verificationMode);
        Log.d("my-tag", "debug with my-tag");
        verifyStatic(verificationMode);
        Log.d(eq("my-tag"), eq("debug with my-tag with exception"), any(Exception.class));
    }

    private static void verifyVerbose(VerificationMode verificationMode) {
        verifyStatic(verificationMode);
        Log.v("my-tag", "verbose with my-tag");
        verifyStatic(verificationMode);
        Log.v(eq("my-tag"), eq("verbose with my-tag with exception"), any(Exception.class));
    }

    @BeforeClass
    public static void setUpBeforeClass() {

        /* Default initial state can be tested only once in the entire test suite... */
        assertEquals(Log.ASSERT, AppCenter.getLogLevel());
        assertEquals(AppCenter.getLogLevel(), AppCenterLog.getLogLevel());
    }

    @Before
    public void setUp() {
        mockStatic(Log.class);
        when(Log.getStackTraceString(any(Throwable.class))).thenReturn("mock stack trace");
    }

    @Test
    public void none() {
        AppCenter.setLogLevel(AppCenterLog.NONE);
        assertEquals(AppCenter.getLogLevel(), AppCenterLog.getLogLevel());
        callLogs();
        verifyVerbose(never());
        verifyDebug(never());
        verifyInfo(never());
        verifyWarn(never());
        verifyError(never());
        verifyAssert(never());
    }

    @Test
    public void assertLevel() {
        AppCenter.setLogLevel(Log.ASSERT);
        assertEquals(AppCenter.getLogLevel(), AppCenterLog.getLogLevel());
        callLogs();
        verifyVerbose(never());
        verifyDebug(never());
        verifyInfo(never());
        verifyWarn(never());
        verifyError(never());
        verifyAssert(times(1));
    }

    @Test
    public void error() {
        AppCenter.setLogLevel(Log.ERROR);
        assertEquals(AppCenter.getLogLevel(), AppCenterLog.getLogLevel());
        callLogs();
        verifyVerbose(never());
        verifyDebug(never());
        verifyInfo(never());
        verifyWarn(never());
        verifyError(times(1));
        verifyAssert(times(1));
    }

    @Test
    public void warn() {
        AppCenter.setLogLevel(Log.WARN);
        assertEquals(AppCenter.getLogLevel(), AppCenterLog.getLogLevel());
        callLogs();
        verifyVerbose(never());
        verifyDebug(never());
        verifyInfo(never());
        verifyWarn(times(1));
        verifyError(times(1));
        verifyAssert(times(1));
    }

    @Test
    public void info() {
        AppCenter.setLogLevel(Log.INFO);
        assertEquals(AppCenter.getLogLevel(), AppCenterLog.getLogLevel());
        callLogs();
        verifyVerbose(never());
        verifyDebug(never());
        verifyInfo(times(1));
        verifyWarn(times(1));
        verifyError(times(1));
        verifyAssert(times(1));
    }

    @Test
    public void debug() {
        AppCenter.setLogLevel(Log.DEBUG);
        assertEquals(AppCenter.getLogLevel(), AppCenterLog.getLogLevel());
        callLogs();
        verifyVerbose(never());
        verifyDebug(times(1));
        verifyInfo(times(1));
        verifyWarn(times(1));
        verifyError(times(1));
        verifyAssert(times(1));
    }

    @Test
    public void verbose() {
        AppCenter.setLogLevel(Log.VERBOSE);
        assertEquals(AppCenter.getLogLevel(), AppCenterLog.getLogLevel());
        callLogs();
        verifyVerbose(times(1));
        verifyDebug(times(1));
        verifyInfo(times(1));
        verifyWarn(times(1));
        verifyError(times(1));
        verifyAssert(times(1));
    }
}
