package com.microsoft.appcenter.storage.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Tokens response.
 */
public class TokensResponse {
    /**
     * A list of token result for request partitions.
     */
    @SerializedName(value = "tokens")
    private List<TokenResult> tokens;

    /**
     * Get a list of token result for request partitions.
     *
     * @return the tokens value
     */
    public List<TokenResult> tokens() {
        return this.tokens;
    }

}