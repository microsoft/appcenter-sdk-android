/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.properties;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.appcenter.ingestion.models.CommonProperties.TYPE;
import static com.microsoft.appcenter.ingestion.models.CommonProperties.TYPED_PROPERTIES;

public class TypedPropertyUtils {

    @SuppressWarnings("IfCanBeSwitch")
    public static TypedProperty create(@NonNull String type) throws JSONException {
        if (BooleanTypedProperty.TYPE.equals(type)) {
            return new BooleanTypedProperty();
        } else if (DateTimeTypedProperty.TYPE.equals(type)) {
            return new DateTimeTypedProperty();
        } else if (DoubleTypedProperty.TYPE.equals(type)) {
            return new DoubleTypedProperty();
        } else if (LongTypedProperty.TYPE.equals(type)) {
            return new LongTypedProperty();
        } else if (StringTypedProperty.TYPE.equals(type)) {
            return new StringTypedProperty();
        }
        throw new JSONException("Unsupported type: " + type);
    }

    public static List<TypedProperty> read(JSONObject object) throws JSONException {
        JSONArray jArray = object.optJSONArray(TYPED_PROPERTIES);
        if (jArray != null) {
            List<TypedProperty> array = new ArrayList<>(jArray.length());
            for (int i = 0; i < jArray.length(); i++) {
                JSONObject jsonObject = jArray.getJSONObject(i);
                TypedProperty typedProperty = TypedPropertyUtils.create(jsonObject.getString(TYPE));
                typedProperty.read(jsonObject);
                array.add(typedProperty);
            }
            return array;
        }
        return null;
    }
}
