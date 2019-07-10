/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.json.DefaultLogSerializer;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("unused")
public class CommonSchemaLogSerializerTest {

    @Test
    public void serializeAndDeserialize() throws JSONException {

        /* Init serializer for Common Schema common fields. */
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(MockCommonSchemaLog.TYPE, new MockCommonSchemaLogFactory());

        /* Prepare a log. */
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        assertJsonFailure(serializer, log);
        log.setVer("3.0");
        assertJsonFailure(serializer, log);
        log.setName("test");
        assertJsonFailure(serializer, log);
        log.setTimestamp(new Date());

        /* All required fields are now set, check we can serialize/deserialize. */
        checkSerialization(serializer, log);

        /* Keep adding top level fields and test. */
        log.setPopSample(3.1415);
        checkSerialization(serializer, log);
        log.setIKey(UUID.randomUUID().toString());
        checkSerialization(serializer, log);
        log.setFlags(5L);
        checkSerialization(serializer, log);
        log.setCV("awXwfegr");

        /* Empty extensions. */
        log.setExt(new Extensions());
        checkSerialization(serializer, log);

        /* Add extension and fields 1 by 1. */

        /* Metadata extension. */
        log.getExt().setMetadata(new MetadataExtension());
        checkSerialization(serializer, log);
        log.getExt().getMetadata().getMetadata().put("f", new JSONObject());
        checkSerialization(serializer, log);

        /* Protocol extension. */
        log.getExt().setProtocol(new ProtocolExtension());
        checkSerialization(serializer, log);
        log.getExt().getProtocol().setTicketKeys(Arrays.asList("First", "Second"));
        checkSerialization(serializer, log);
        log.getExt().getProtocol().setDevMake("Samsung");
        checkSerialization(serializer, log);
        log.getExt().getProtocol().setDevModel("S5");
        checkSerialization(serializer, log);

        /* User extension. */
        log.getExt().setUser(new UserExtension());
        checkSerialization(serializer, log);
        log.getExt().getUser().setLocalId("d:1234");
        checkSerialization(serializer, log);
        log.getExt().getUser().setLocale("en-US");
        checkSerialization(serializer, log);

        /* Device extension. */
        log.getExt().setDevice(new DeviceExtension());
        checkSerialization(serializer, log);
        log.getExt().getDevice().setLocalId("5DE1C5B8433DF3EE");
        checkSerialization(serializer, log);

        /* OS extension. */
        log.getExt().setOs(new OsExtension());
        checkSerialization(serializer, log);
        log.getExt().getOs().setName("Android");
        checkSerialization(serializer, log);
        log.getExt().getOs().setVer("8.1.0");
        checkSerialization(serializer, log);

        /* App extension. */
        log.getExt().setApp(new AppExtension());
        checkSerialization(serializer, log);
        log.getExt().getApp().setId("com.contoso.app");
        checkSerialization(serializer, log);
        log.getExt().getApp().setVer("1.2.3");
        checkSerialization(serializer, log);
        log.getExt().getApp().setLocale("fr-FR");
        checkSerialization(serializer, log);
        log.getExt().getApp().setUserId("c:charlie");
        checkSerialization(serializer, log);

        /* Net extension. */
        log.getExt().setNet(new NetExtension());
        checkSerialization(serializer, log);
        log.getExt().getNet().setProvider("AT&T");
        checkSerialization(serializer, log);

        /* SDK extension. */
        log.getExt().setSdk(new SdkExtension());
        checkSerialization(serializer, log);
        log.getExt().getSdk().setLibVer("appcenter.android-1.6.0");
        checkSerialization(serializer, log);
        log.getExt().getSdk().setEpoch(UUID.randomUUID().toString());
        checkSerialization(serializer, log);
        log.getExt().getSdk().setSeq(21L);
        checkSerialization(serializer, log);
        log.getExt().getSdk().setInstallId(UUID.randomUUID());
        checkSerialization(serializer, log);

        /* Loc extension. */
        log.getExt().setLoc(new LocExtension());
        checkSerialization(serializer, log);
        log.getExt().getLoc().setTz("-08:00");
        checkSerialization(serializer, log);

        /* Data. */
        log.setData(new Data());
        checkSerialization(serializer, log);
        log.getData().getProperties().put("baseType", "custom");
        log.getData().getProperties().put("baseData", new JSONObject());
        log.getData().getProperties().put("a", "b");
        Log copy = serializer.deserializeLog(serializer.serializeLog(log), MockCommonSchemaLog.TYPE);

        /*
         * equals method not defined in JSONObject so it's comparing strings, which is order sensitive.
         * Since now Part B is serialized first, it's order sensitive for those 2 keys.
         * Need to compare Part B, then remove Part B then compare the rest with equals...
         */
        assertTrue(copy instanceof CommonSchemaLog);
        CommonSchemaLog csCopy = (CommonSchemaLog) copy;
        assertEquals(log.getData().getProperties().remove("baseType"), csCopy.getData().getProperties().remove("baseType"));
        assertEquals(log.getData().getProperties().remove("baseData").toString(), csCopy.getData().getProperties().remove("baseData").toString());
        assertEquals(log, copy);

        /* Check deserialize fails without data type */
        try {
            serializer.deserializeLog(serializer.serializeLog(log), null);
            fail("Was supposed to fail with JSONException");
        } catch (JSONException ignore) {

            /* Expected. */
        }
    }

    /**
     * Verify JSON error as long as required fields (required as per SDK) are missing.
     */
    private void assertJsonFailure(LogSerializer serializer, MockCommonSchemaLog log) {
        try {
            serializer.serializeLog(log);
            fail("Was supposed to fail with JSONException");
        } catch (JSONException ignore) {

            /* Expected. */
        }
    }

    private void checkSerialization(LogSerializer serializer, MockCommonSchemaLog log) throws JSONException {
        Log copy = serializer.deserializeLog(serializer.serializeLog(log), MockCommonSchemaLog.TYPE);
        assertEquals(log, copy);
    }
}