package com.microsoft.appcenter.distribute;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class DistributeUtilsTest {

    @Test
    public void init() {
        assertNotNull(new DistributeUtils());
        assertNotNull(new DistributeConstants());
    }
}
