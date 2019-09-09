/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JwtClaimsTest {

    private static final String JWT_FORMAT = "mockHeaders.%s.mockSignature";

    @Test
    public void getValidJwt() throws JSONException {
        JSONObject claims = new JSONObject();
        String userId = "some_random_user_id";
        int expiration = 1426420800;
        claims.put("sub", userId);
        claims.put("exp", expiration);
        String base64Claims = Base64.encodeToString(claims.toString().getBytes(), Base64.DEFAULT);
        String token = String.format(JWT_FORMAT, base64Claims);
        JwtClaims jwtClaims = JwtClaims.parse(token);
        assertNotNull(jwtClaims);
        assertEquals(userId, jwtClaims.getSubject());
        assertEquals(expiration, jwtClaims.getExpirationDate().getTime() / 1000);
    }

    @Test
    public void expirationClaimMissing() throws JSONException {
        JSONObject claims = new JSONObject();
        String userId = "some_random_user_id";
        claims.put("sub", userId);
        String base64Claims = Base64.encodeToString(claims.toString().getBytes(), Base64.DEFAULT);
        String token = String.format(JWT_FORMAT, base64Claims);
        JwtClaims jwtClaims = JwtClaims.parse(token);
        assertNull(jwtClaims);
    }

    @Test
    public void subjectClaimMissing() throws JSONException {
        JSONObject claims = new JSONObject();
        int expiration = 1426420800;
        claims.put("exp", expiration);
        String base64Claims = Base64.encodeToString(claims.toString().getBytes(), Base64.DEFAULT);
        String token = String.format(JWT_FORMAT, base64Claims);
        JwtClaims jwtClaims = JwtClaims.parse(token);
        assertNull(jwtClaims);
    }

    @Test
    public void expirationClaimInvalid() throws JSONException {
        JSONObject claims = new JSONObject();
        String expiration = "invalid";
        claims.put("exp", expiration);
        String base64Claims = Base64.encodeToString(claims.toString().getBytes(), Base64.DEFAULT);
        String token = String.format(JWT_FORMAT, base64Claims);
        JwtClaims jwtClaims = JwtClaims.parse(token);
        assertNull(jwtClaims);
    }

    @Test
    public void invalidBase64Token() {
        String base64Claims = "invalidBase64";
        String token = String.format(JWT_FORMAT, base64Claims);
        JwtClaims jwtClaims = JwtClaims.parse(token);
        assertNull(jwtClaims);
    }

    @Test
    public void missingParts() {
        String token = "invalid JWT";
        JwtClaims jwtClaims = JwtClaims.parse(token);
        assertNull(jwtClaims);
    }
}
