package com.microsoft.azure.mobile.updates;

import junit.framework.Assert;

import org.junit.Test;

public class UpdatesTest extends AbstractUpdatesTest {

    @Test
    public void singleton() {
        Assert.assertSame(Updates.getInstance(), Updates.getInstance());
    }
}
