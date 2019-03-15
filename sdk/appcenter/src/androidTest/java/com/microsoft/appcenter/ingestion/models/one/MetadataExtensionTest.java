/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.test.TestUtils;

import org.json.JSONException;
import org.junit.Test;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

public class MetadataExtensionTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new MetadataExtension());
    }

    @Test
    public void equalsHashCode() throws JSONException {

        /* Empty objects. */
        MetadataExtension a = new MetadataExtension();
        MetadataExtension b = new MetadataExtension();
        checkEquals(a, b);

        /* Properties. */
        a.getMetadata().put("a", "b");
        checkNotEquals(a, b);
        b.getMetadata().put("a", "b");
        checkEquals(a, b);
    }
}
