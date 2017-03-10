package com.microsoft.azure.mobile.ingestion.models;


import com.microsoft.azure.mobile.test.TestUtils;
import com.microsoft.azure.mobile.utils.UUIDUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.microsoft.azure.mobile.test.TestUtils.checkEquals;
import static com.microsoft.azure.mobile.test.TestUtils.checkNotEquals;

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

        UUID sid = UUIDUtils.randomUUID();
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