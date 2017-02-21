package com.microsoft.azure.mobile.push.ingestion.models;

import com.microsoft.azure.mobile.test.TestUtils;

import org.junit.Test;

@SuppressWarnings("unused")
public class PushInstallationLogTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new PushInstallationLog());
    }
}