package com.microsoft.sonoma.core.ingestion.models;

import com.microsoft.sonoma.core.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class WrapperSdk implements Model {

    private static final String WRAPPER_SDK_VERSION = "wrapper_sdk_version";

    private static final String WRAPPER_SDK_NAME = "wrapper_sdk_name";

    /**
     * Version of the wrapper SDK in semver format. When the SDK is embedding another base SDK (for example Xamarin.Android wraps Android),
     * the Xamarin specific version is populated into this field while sdkVersion refers to the original Android SDK.
     */
    private String wrapperSdkVersion;

    /**
     * Name of the wrapper SDK. Consists of the name of the SDK and the wrapper platform, e.g. "avalanchesdk.xamarin", "hockeysdk.cordova".
     */
    private String wrapperSdkName;

    /**
     * Get the wrapperSdkVersion value.
     *
     * @return the wrapperSdkVersion value
     */
    public String getWrapperSdkVersion() {
        return this.wrapperSdkVersion;
    }

    /**
     * Set the wrapperSdkVersion value.
     *
     * @param wrapperSdkVersion the wrapperSdkVersion value to set
     */
    public void setWrapperSdkVersion(String wrapperSdkVersion) {
        this.wrapperSdkVersion = wrapperSdkVersion;
    }

    /**
     * Get the wrapperSdkName value.
     *
     * @return the wrapperSdkName value
     */
    public String getWrapperSdkName() {
        return this.wrapperSdkName;
    }

    /**
     * Set the wrapperSdkName value.
     *
     * @param wrapperSdkName the wrapperSdkName value to set
     */
    public void setWrapperSdkName(String wrapperSdkName) {
        this.wrapperSdkName = wrapperSdkName;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        setWrapperSdkVersion(object.optString(WRAPPER_SDK_VERSION, null));
        setWrapperSdkName(object.optString(WRAPPER_SDK_NAME, null));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, WRAPPER_SDK_VERSION, getWrapperSdkVersion());
        JSONUtils.write(writer, WRAPPER_SDK_NAME, getWrapperSdkName());
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WrapperSdk that = (WrapperSdk) o;

        if (wrapperSdkVersion != null ? !wrapperSdkVersion.equals(that.wrapperSdkVersion) : that.wrapperSdkVersion != null)
            return false;
        return wrapperSdkName != null ? wrapperSdkName.equals(that.wrapperSdkName) : that.wrapperSdkName == null;
    }

    @Override
    public int hashCode() {
        int result = wrapperSdkVersion != null ? wrapperSdkVersion.hashCode() : 0;
        result = 31 * result + (wrapperSdkName != null ? wrapperSdkName.hashCode() : 0);
        return result;
    }
}
