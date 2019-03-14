/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models;

import com.microsoft.appcenter.test.TestUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.junit.Test;

import java.util.Date;
import java.util.UUID;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
public class AbstractLogTest {

    @Test
    public void utilsCoverage() {
        new CommonProperties();
    }

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new MockLog());
        MockLogWithProperties mockLogWithProperties = new MockLogWithProperties();
        TestUtils.compareSelfNullClass(mockLogWithProperties);
        mockLogWithProperties.setTimestamp(new Date(1L));
        checkNotEquals(mockLogWithProperties, new MockLogWithProperties());
    }

    @Test
    public void compare() {

        /* Empty objects. */
        AbstractLog a = new MockLog();
        AbstractLog b = new MockLog();
        checkEquals(a, b);

        /* Transmission targets. */
        a.addTransmissionTarget("a");
        checkNotEquals(a, b);
        b.addTransmissionTarget("a");
        checkEquals(a, b);

        /* Timestamp. */
        a.setTimestamp(new Date(1));
        checkNotEquals(a, b);
        b.setTimestamp(new Date(1));
        checkEquals(a, b);

        /* Sid. */
        UUID sid1 = UUID.randomUUID();
        UUID sid2 = UUID.randomUUID();
        a.setSid(sid1);
        checkNotEquals(a, b);
        b.setSid(sid2);
        checkNotEquals(a, b);
        b.setSid(sid1);
        checkEquals(a, b);

        /* Distribution group ID. */
        String distributionGroupId1 = UUID.randomUUID().toString();
        String distributionGroupId2 = UUID.randomUUID().toString();
        a.setDistributionGroupId(distributionGroupId1);
        checkNotEquals(a, b);
        b.setDistributionGroupId(distributionGroupId2);
        checkNotEquals(a, b);
        b.setDistributionGroupId(distributionGroupId1);
        checkEquals(a, b);

        /* User ID. */
        String userId1 = "alice";
        String userId2 = "bob";
        a.setUserId(userId1);
        checkNotEquals(a, b);
        b.setUserId(userId2);
        checkNotEquals(a, b);
        b.setUserId(userId1);
        checkEquals(a, b);

        /* Device. */
        Device d1 = new Device();
        d1.setLocale("a");
        Device d2 = new Device();
        d2.setSdkVersion("a");
        a.setDevice(d1);
        checkNotEquals(a, b);
        b.setDevice(d2);
        checkNotEquals(a, b);
        b.setDevice(d1);
        checkEquals(a, b);

        /* Tag. */
        a.setTag(new Object());
        checkNotEquals(a, b);
        b.setTag(a.getTag());
        checkEquals(a, b);
    }

    @Test
    public void testTransmissionTargets() {
        final String transmissionTargetToken = UUID.randomUUID().toString();
        final AbstractLog log = new MockLog();
        assertEquals(0, log.getTransmissionTargetTokens().size());

        /* Add first transmission target. */
        log.addTransmissionTarget(transmissionTargetToken);
        assertTrue(log.getTransmissionTargetTokens().contains(transmissionTargetToken));
        assertEquals(1, log.getTransmissionTargetTokens().size());

        /* Ignore duplicate transmission targets. */
        log.addTransmissionTarget(transmissionTargetToken);
        assertEquals(1, log.getTransmissionTargetTokens().size());
    }

    @Test(expected = JSONException.class)
    public void readDifferentTypeTest() throws JSONException {
        JSONObject mockJsonObject = mock(JSONObject.class);
        when(mockJsonObject.getString(CommonProperties.TYPE)).thenReturn("type");
        AbstractLog mockLog = new MockLog();
        mockLog.read(mockJsonObject);
    }

    @Test
    public void writeNullDeviceTest() throws JSONException {
        JSONStringer mockJsonStringer = mock(JSONStringer.class);
        when(mockJsonStringer.key(anyString())).thenReturn(mockJsonStringer);
        when(mockJsonStringer.value(anyString())).thenReturn(mockJsonStringer);

        AbstractLog mockLog = new MockLog();
        mockLog.setTimestamp(new Date());
        mockLog.write(mockJsonStringer);

        verify(mockJsonStringer, never()).key(AbstractLog.DEVICE);
    }

    private static class MockLog extends AbstractLog {

        @Override
        public String getType() {
            return null;
        }
    }

    private static class MockLogWithProperties extends LogWithProperties {

        @Override
        public String getType() {
            return null;
        }
    }

    private static class MockLogWithType extends MockLog {

        @Override
        public String getType() {
            return "mockType";
        }
    }
}
