/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch;

import android.content.Context;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import org.json.JSONObject;

import java.text.ParseException;
import java.util.Map;

public class JwtUtils {

    public static JSONObject getParsedToken(Context context, String rawToken) {
        try {
            JWT parsedIdToken = JWTParser.parse(rawToken);
            Map<String, Object> claims = parsedIdToken.getJWTClaimsSet().getClaims();
            return new JSONObject(claims);
        } catch (ParseException ex) {
            AppCenterLog.error(AppCenterLog.LOG_TAG, context.getString(R.string.jwt_parse_error));
        }
        return null;
    }
}
