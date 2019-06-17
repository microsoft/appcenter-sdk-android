/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models;


import com.microsoft.appcenter.test.TestUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

@SuppressWarnings("unused")
public class StartServiceLogTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new StartServiceLog());
    }

    @Test
    public void compare() {

        /* Empty objects. */
        StartServiceLog a = new StartServiceLog();
        StartServiceLog b = new StartServiceLog();
        checkEquals(a, b);
        checkEquals(a.getType(), StartServiceLog.TYPE);

        UUID sid = UUID.randomUUID();
        a.setSid(sid);
        checkNotEquals(a, b);
        b.setSid(sid);
        checkEquals(a, b);

        /* Services. */
        List<String> services = new ArrayList<>();
        services.add("FIRST");
        services.add("SECOND");
        a.setServices(services);
        checkEquals(a.getServices(), services);
        checkNotEquals(a, b);
        b.setServices(new ArrayList<String>());
        checkNotEquals(a, b);
        b.setServices(services);
        checkEquals(a, b);
    }
}