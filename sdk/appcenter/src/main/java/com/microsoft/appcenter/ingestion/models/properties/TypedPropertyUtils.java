package com.microsoft.appcenter.ingestion.models.properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.appcenter.ingestion.models.CommonProperties.TYPE;
import static com.microsoft.appcenter.ingestion.models.CommonProperties.TYPED_PROPERTIES;

public class TypedPropertyUtils {

    public static TypedProperty create(String type) throws JSONException {
        switch (type) {
            case BooleanTypedProperty.TYPE:
                return new BooleanTypedProperty();

            case DateTimeTypedProperty.TYPE:
                return new DateTimeTypedProperty();

            case DoubleTypedProperty.TYPE:
                return new DoubleTypedProperty();

            case LongTypedProperty.TYPE:
                return new LongTypedProperty();

            case StringTypedProperty.TYPE:
                return new StringTypedProperty();

            default:
                throw new JSONException("Unsupported type: " + type);
        }
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
