/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.json;

import org.json.JSONException;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("unused")
public class JSONDateUtilsAndroidTest {

    @Test
    public void utilsCoverage() {
        new JSONDateUtils();
    }

    @Test
    public void formatAndParseDate() throws JSONException {
        Date date = new Date();
        String dateString = JSONDateUtils.toString(date);
        Date dateParsed = JSONDateUtils.toDate(dateString);

        /* Using equals will also check for millisecond accuracy. */
        assertEquals(date, dateParsed);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test(expected = JSONException.class)
    public void formatNullDate() throws JSONException {
        JSONDateUtils.toString(null);
    }

    @Test(expected = JSONException.class)
    public void parseNullDate() throws JSONException {
        JSONDateUtils.toDate(null);
    }

    @Test(expected = JSONException.class)
    public void parseInvalidDate() throws JSONException {
        JSONDateUtils.toDate("Fri Jul 07 17:43:56 PDT 2017");
    }
}
