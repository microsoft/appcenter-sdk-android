/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.json;

import org.json.JSONException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Utility to convert date to string and vice versa to use in JSON payloads.
 * The date format is using ISO 8601 and includes date and time to milliseconds accuracy.
 * It also always uses UTC timezone.
 */
public final class JSONDateUtils {

    /**
     * Date formatter.
     */
    private static final ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {

        @Override
        protected DateFormat initialValue() {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return dateFormat;
        }
    };

    /**
     * Check date parameter is null.
     *
     * @param date date parameter.
     * @throws JSONException if parameter is null.
     */
    private static void checkNull(Object date) throws JSONException {
        if (date == null) {
            throw new JSONException("date cannot be null");
        }
    }

    /**
     * Convert date to string.
     *
     * @param date date.
     * @return string.
     * @throws JSONException if the date is null.
     */
    public static String toString(Date date) throws JSONException {
        checkNull(date);
        return DATE_FORMAT.get().format(date);
    }

    /**
     * Convert string to date.
     *
     * @param date date.
     * @return string.
     * @throws JSONException if string has a wrong format or is null.
     */
    public static Date toDate(String date) throws JSONException {
        checkNull(date);
        try {
            return DATE_FORMAT.get().parse(date);
        } catch (ParseException e) {
            throw new JSONException(e.getMessage());
        }
    }
}
