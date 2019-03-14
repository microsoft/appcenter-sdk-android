/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.test.TestUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

public class ProtocolExtensionTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new ProtocolExtension());
    }

    @Test
    public void equalsHashCode() {

        /* Empty objects. */
        ProtocolExtension a = new ProtocolExtension();
        ProtocolExtension b = new ProtocolExtension();
        checkEquals(a, b);

        /* Ticket Keys */
        List<String> ticketKeys = new ArrayList<>();
        ticketKeys.add("FIRST");
        ticketKeys.add("SECOND");
        a.setTicketKeys(ticketKeys);
        checkEquals(a.getTicketKeys(), ticketKeys);
        checkNotEquals(a, b);
        b.setTicketKeys(new ArrayList<String>());
        checkNotEquals(a, b);
        b.setTicketKeys(ticketKeys);
        checkEquals(a, b);

        /* Dev make. */
        a.setDevMake("a1");
        checkNotEquals(a, b);
        b.setDevMake("b1");
        checkNotEquals(a, b);
        b.setDevMake("a1");
        checkEquals(a, b);

        /* Dev model. */
        a.setDevModel("a2");
        checkNotEquals(a, b);
        b.setDevModel("b2");
        checkNotEquals(a, b);
        b.setDevModel("a2");
        checkEquals(a, b);
    }
}
