/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.test.TestUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

public class ExtensionsTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new Extensions());
    }

    @Test
    public void equalsHashCode() throws JSONException {

        /* Empty objects. */
        Extensions a = new Extensions();
        Extensions b = new Extensions();
        checkEquals(a, b);

        /* Metadata. */
        MetadataExtension metadata = new MetadataExtension();
        metadata.getMetadata().put("f", new JSONObject());
        a.setMetadata(metadata);
        checkNotEquals(a, b);
        b.setMetadata(new MetadataExtension());
        checkNotEquals(a, b);
        b.getMetadata().getMetadata().put("f", new JSONObject());
        checkEquals(a, b);

        /* Protocol. */
        ProtocolExtension protocol = new ProtocolExtension();
        protocol.setDevModel("model");
        a.setProtocol(protocol);
        checkNotEquals(a, b);
        b.setProtocol(new ProtocolExtension());
        checkNotEquals(a, b);
        b.setProtocol(a.getProtocol());
        checkEquals(a, b);

        /* User. */
        UserExtension user = new UserExtension();
        user.setLocale("fr-FR");
        a.setUser(user);
        checkNotEquals(a, b);
        b.setUser(new UserExtension());
        checkNotEquals(a, b);
        b.setUser(a.getUser());
        checkEquals(a, b);

        /* Device. */
        DeviceExtension device = new DeviceExtension();
        device.setLocalId("123");
        a.setDevice(device);
        checkNotEquals(a, b);
        b.setDevice(new DeviceExtension());
        checkNotEquals(a, b);
        b.setDevice(a.getDevice());
        checkEquals(a, b);

        /* OS. */
        OsExtension os = new OsExtension();
        os.setVer("7.0.0");
        a.setOs(os);
        checkNotEquals(a, b);
        b.setOs(new OsExtension());
        checkNotEquals(a, b);
        b.setOs(a.getOs());
        checkEquals(a, b);

        /* App. */
        AppExtension app = new AppExtension();
        app.setVer("1.2.3");
        a.setApp(app);
        checkNotEquals(a, b);
        b.setApp(new AppExtension());
        checkNotEquals(a, b);
        b.setApp(a.getApp());
        checkEquals(a, b);

        /* Net. */
        NetExtension net = new NetExtension();
        net.setProvider("T-Mobile");
        a.setNet(net);
        checkNotEquals(a, b);
        b.setNet(new NetExtension());
        checkNotEquals(a, b);
        b.setNet(a.getNet());
        checkEquals(a, b);

        /* SDK. */
        SdkExtension sdk = new SdkExtension();
        sdk.setSeq(1L);
        a.setSdk(sdk);
        checkNotEquals(a, b);
        b.setSdk(new SdkExtension());
        checkNotEquals(a, b);
        b.setSdk(a.getSdk());
        checkEquals(a, b);

        /* Loc. */
        LocExtension loc = new LocExtension();
        loc.setTz("+02:00");
        a.setLoc(loc);
        checkNotEquals(a, b);
        b.setLoc(new LocExtension());
        checkNotEquals(a, b);
        b.setLoc(a.getLoc());
        checkEquals(a, b);
    }
}
