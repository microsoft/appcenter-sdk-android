/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models;

import com.microsoft.appcenter.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * Device characteristic log.
 */
public class Device extends WrapperSdk {

    private static final String SDK_NAME = "sdkName";

    private static final String SDK_VERSION = "sdkVersion";

    private static final String MODEL = "model";

    private static final String OEM_NAME = "oemName";

    private static final String OS_NAME = "osName";

    private static final String OS_VERSION = "osVersion";

    private static final String OS_BUILD = "osBuild";

    private static final String OS_API_LEVEL = "osApiLevel";

    private static final String LOCALE = "locale";

    private static final String TIME_ZONE_OFFSET = "timeZoneOffset";

    private static final String SCREEN_SIZE = "screenSize";

    private static final String APP_VERSION = "appVersion";

    private static final String CARRIER_NAME = "carrierName";

    private static final String CARRIER_COUNTRY = "carrierCountry";

    private static final String APP_BUILD = "appBuild";

    private static final String APP_NAMESPACE = "appNamespace";

    /**
     * Name of the SDK.
     */
    private String sdkName;

    /**
     * Version of the SDK.
     */
    private String sdkVersion;

    /**
     * Device model (example: iPad2,3).
     */
    private String model;

    /**
     * Device manufacturer (example: HTC).
     */
    private String oemName;

    /**
     * OS name (example: iOS).
     */
    private String osName;

    /**
     * OS version (example: 9.3.0).
     */
    private String osVersion;

    /**
     * OS build code (example: LMY47X).
     */
    private String osBuild;

    /**
     * API level when applicable like in Android (example: 15).
     */
    private Integer osApiLevel;

    /**
     * Language code (example: en_US).
     */
    private String locale;

    /**
     * The offset in minutes from UTC for the device time zone, including
     * daylight savings time.
     */
    private Integer timeZoneOffset;

    /**
     * Screen size of the device in pixels (example: 640x480).
     */
    private String screenSize;

    /**
     * Application version name.
     */
    private String appVersion;

    /**
     * Carrier name (for mobile devices).
     */
    private String carrierName;

    /**
     * Carrier country code (for mobile devices).
     */
    private String carrierCountry;

    /**
     * The app's build number, e.g. 42.
     */
    private String appBuild;

    /**
     * The bundle identifier, package identifier, or namespace, depending on
     * what the individual platforms use,  .e.g com.microsoft.example.
     */
    private String appNamespace;

    /**
     * Get the sdkName value.
     *
     * @return the sdkName value
     */
    public String getSdkName() {
        return sdkName;
    }

    /**
     * Set the sdkName value.
     *
     * @param sdkName the sdkName value to set
     */
    public void setSdkName(String sdkName) {
        this.sdkName = sdkName;
    }

    /**
     * Get the sdkVersion value.
     *
     * @return the sdkVersion value
     */
    public String getSdkVersion() {
        return this.sdkVersion;
    }

    /**
     * Set the sdkVersion value.
     *
     * @param sdkVersion the sdkVersion value to set
     */
    public void setSdkVersion(String sdkVersion) {
        this.sdkVersion = sdkVersion;
    }

    /**
     * Get the model value.
     *
     * @return the model value
     */
    public String getModel() {
        return this.model;
    }

    /**
     * Set the model value.
     *
     * @param model the model value to set
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Get the oemName value.
     *
     * @return the oemName value
     */
    public String getOemName() {
        return this.oemName;
    }

    /**
     * Set the oemName value.
     *
     * @param oemName the oemName value to set
     */
    public void setOemName(String oemName) {
        this.oemName = oemName;
    }

    /**
     * Get the osName value.
     *
     * @return the osName value
     */
    public String getOsName() {
        return this.osName;
    }

    /**
     * Set the osName value.
     *
     * @param osName the osName value to set
     */
    public void setOsName(String osName) {
        this.osName = osName;
    }

    /**
     * Get the osVersion value.
     *
     * @return the osVersion value
     */
    public String getOsVersion() {
        return this.osVersion;
    }

    /**
     * Set the osVersion value.
     *
     * @param osVersion the osVersion value to set
     */
    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    /**
     * Get the osBuild value.
     *
     * @return the osBuild value
     */
    public String getOsBuild() {
        return this.osBuild;
    }

    /**
     * Set the osBuild value.
     *
     * @param osBuild the osBuild value to set
     */
    public void setOsBuild(String osBuild) {
        this.osBuild = osBuild;
    }

    /**
     * Get the osApiLevel value.
     *
     * @return the osApiLevel value
     */
    public Integer getOsApiLevel() {
        return this.osApiLevel;
    }

    /**
     * Set the osApiLevel value.
     *
     * @param osApiLevel the osApiLevel value to set
     */
    public void setOsApiLevel(Integer osApiLevel) {
        this.osApiLevel = osApiLevel;
    }

    /**
     * Get the locale value.
     *
     * @return the locale value
     */
    public String getLocale() {
        return this.locale;
    }

    /**
     * Set the locale value.
     *
     * @param locale the locale value to set
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /**
     * Get the timeZoneOffset value.
     *
     * @return the timeZoneOffset value
     */
    public Integer getTimeZoneOffset() {
        return this.timeZoneOffset;
    }

    /**
     * Set the timeZoneOffset value.
     *
     * @param timeZoneOffset the timeZoneOffset value to set
     */
    public void setTimeZoneOffset(Integer timeZoneOffset) {
        this.timeZoneOffset = timeZoneOffset;
    }

    /**
     * Get the screenSize value.
     *
     * @return the screenSize value
     */
    public String getScreenSize() {
        return this.screenSize;
    }

    /**
     * Set the screenSize value.
     *
     * @param screenSize the screenSize value to set
     */
    public void setScreenSize(String screenSize) {
        this.screenSize = screenSize;
    }

    /**
     * Get the appVersion value.
     *
     * @return the appVersion value
     */
    public String getAppVersion() {
        return this.appVersion;
    }

    /**
     * Set the appVersion value.
     *
     * @param appVersion the appVersion value to set
     */
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    /**
     * Get the carrierName value.
     *
     * @return the carrierName value
     */
    public String getCarrierName() {
        return this.carrierName;
    }

    /**
     * Set the carrierName value.
     *
     * @param carrierName the carrierName value to set
     */
    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    /**
     * Get the carrierCountry value.
     *
     * @return the carrierCountry value
     */
    public String getCarrierCountry() {
        return this.carrierCountry;
    }

    /**
     * Set the carrierCountry value.
     *
     * @param carrierCountry the carrierCountry value to set
     */
    public void setCarrierCountry(String carrierCountry) {
        this.carrierCountry = carrierCountry;
    }

    /**
     * Get the appBuild value.
     *
     * @return the appBuild value
     */
    public String getAppBuild() {
        return this.appBuild;
    }

    /**
     * Set the appBuild value.
     *
     * @param appBuild the appBuild value to set
     */
    public void setAppBuild(String appBuild) {
        this.appBuild = appBuild;
    }

    /**
     * Get the appNamespace value.
     *
     * @return the appNamespace value
     */
    public String getAppNamespace() {
        return this.appNamespace;
    }

    /**
     * Set the appNamespace value.
     *
     * @param appNamespace the appNamespace value to set
     */
    public void setAppNamespace(String appNamespace) {
        this.appNamespace = appNamespace;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        super.read(object);
        setSdkName(object.getString(SDK_NAME));
        setSdkVersion(object.getString(SDK_VERSION));
        setModel(object.getString(MODEL));
        setOemName(object.getString(OEM_NAME));
        setOsName(object.getString(OS_NAME));
        setOsVersion(object.getString(OS_VERSION));
        setOsBuild(object.optString(OS_BUILD, null));
        setOsApiLevel(JSONUtils.readInteger(object, OS_API_LEVEL));
        setLocale(object.getString(LOCALE));
        setTimeZoneOffset(object.getInt(TIME_ZONE_OFFSET));
        setScreenSize(object.getString(SCREEN_SIZE));
        setAppVersion(object.getString(APP_VERSION));
        setCarrierName(object.optString(CARRIER_NAME, null));
        setCarrierCountry(object.optString(CARRIER_COUNTRY, null));
        setAppBuild(object.getString(APP_BUILD));
        setAppNamespace(object.optString(APP_NAMESPACE, null));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        super.write(writer);
        writer.key(SDK_NAME).value(getSdkName());
        writer.key(SDK_VERSION).value(getSdkVersion());
        writer.key(MODEL).value(getModel());
        writer.key(OEM_NAME).value(getOemName());
        writer.key(OS_NAME).value(getOsName());
        writer.key(OS_VERSION).value(getOsVersion());
        JSONUtils.write(writer, OS_BUILD, getOsBuild());
        JSONUtils.write(writer, OS_API_LEVEL, getOsApiLevel());
        writer.key(LOCALE).value(getLocale());
        writer.key(TIME_ZONE_OFFSET).value(getTimeZoneOffset());
        writer.key(SCREEN_SIZE).value(getScreenSize());
        writer.key(APP_VERSION).value(getAppVersion());
        JSONUtils.write(writer, CARRIER_NAME, getCarrierName());
        JSONUtils.write(writer, CARRIER_COUNTRY, getCarrierCountry());
        writer.key(APP_BUILD).value(getAppBuild());
        JSONUtils.write(writer, APP_NAMESPACE, getAppNamespace());
    }

    @Override
    @SuppressWarnings({"SimplifiableIfStatement", "EqualsReplaceableByObjectsCall"})
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Device device = (Device) o;
        if (sdkName != null ? !sdkName.equals(device.sdkName) : device.sdkName != null) {
            return false;
        }
        if (sdkVersion != null ? !sdkVersion.equals(device.sdkVersion) : device.sdkVersion != null) {
            return false;
        }
        if (model != null ? !model.equals(device.model) : device.model != null) {
            return false;
        }
        if (oemName != null ? !oemName.equals(device.oemName) : device.oemName != null) {
            return false;
        }
        if (osName != null ? !osName.equals(device.osName) : device.osName != null) {
            return false;
        }
        if (osVersion != null ? !osVersion.equals(device.osVersion) : device.osVersion != null) {
            return false;
        }
        if (osBuild != null ? !osBuild.equals(device.osBuild) : device.osBuild != null) {
            return false;
        }
        if (osApiLevel != null ? !osApiLevel.equals(device.osApiLevel) : device.osApiLevel != null) {
            return false;
        }
        if (locale != null ? !locale.equals(device.locale) : device.locale != null) {
            return false;
        }
        if (timeZoneOffset != null ? !timeZoneOffset.equals(device.timeZoneOffset) : device.timeZoneOffset != null) {
            return false;
        }
        if (screenSize != null ? !screenSize.equals(device.screenSize) : device.screenSize != null) {
            return false;
        }
        if (appVersion != null ? !appVersion.equals(device.appVersion) : device.appVersion != null) {
            return false;
        }
        if (carrierName != null ? !carrierName.equals(device.carrierName) : device.carrierName != null) {
            return false;
        }
        if (carrierCountry != null ? !carrierCountry.equals(device.carrierCountry) : device.carrierCountry != null) {
            return false;
        }
        if (appBuild != null ? !appBuild.equals(device.appBuild) : device.appBuild != null) {
            return false;
        }
        return appNamespace != null ? appNamespace.equals(device.appNamespace) : device.appNamespace == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (sdkName != null ? sdkName.hashCode() : 0);
        result = 31 * result + (sdkVersion != null ? sdkVersion.hashCode() : 0);
        result = 31 * result + (model != null ? model.hashCode() : 0);
        result = 31 * result + (oemName != null ? oemName.hashCode() : 0);
        result = 31 * result + (osName != null ? osName.hashCode() : 0);
        result = 31 * result + (osVersion != null ? osVersion.hashCode() : 0);
        result = 31 * result + (osBuild != null ? osBuild.hashCode() : 0);
        result = 31 * result + (osApiLevel != null ? osApiLevel.hashCode() : 0);
        result = 31 * result + (locale != null ? locale.hashCode() : 0);
        result = 31 * result + (timeZoneOffset != null ? timeZoneOffset.hashCode() : 0);
        result = 31 * result + (screenSize != null ? screenSize.hashCode() : 0);
        result = 31 * result + (appVersion != null ? appVersion.hashCode() : 0);
        result = 31 * result + (carrierName != null ? carrierName.hashCode() : 0);
        result = 31 * result + (carrierCountry != null ? carrierCountry.hashCode() : 0);
        result = 31 * result + (appBuild != null ? appBuild.hashCode() : 0);
        result = 31 * result + (appNamespace != null ? appNamespace.hashCode() : 0);
        return result;
    }
}
