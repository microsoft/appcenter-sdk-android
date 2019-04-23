/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Tokens response.
 */
public class TokensResponse {

    /**
     * A list of token result for request partitions.
     */
    @SuppressWarnings("unused")
    @SerializedName("tokens")
    private List<TokenResult> mTokens;

    /**
     * Get a list of token result for request partitions.
     *
     * @return the tokens value
     */
    public List<TokenResult> getTokens() {
        return mTokens;
    }

}