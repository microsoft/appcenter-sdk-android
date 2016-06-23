package avalanche.base.ingestion.models;

/**
 * Device characteristic log.
 */
public class DeviceLog extends Log {

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
    private String timeZoneOffset;

    /**
     * Screen size of the device in pixels (example: 640x480).
     */
    private String screenSize;

    /**
     * Application version name.
     */
    private String appVersion;

    /**
     * Application version code.
     */
    private String appCode;

    /**
     * Carrier name (for mobile devices).
     */
    private String carrierName;

    /**
     * Carrier country code (for mobile devices).
     */
    private String carrierCountry;

    @Override
    public String getType() {
        return "device";
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
    public String getTimeZoneOffset() {
        return this.timeZoneOffset;
    }

    /**
     * Set the timeZoneOffset value.
     *
     * @param timeZoneOffset the timeZoneOffset value to set
     */
    public void setTimeZoneOffset(String timeZoneOffset) {
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
     * Get the appCode value.
     *
     * @return the appCode value
     */
    public String getAppCode() {
        return this.appCode;
    }

    /**
     * Set the appCode value.
     *
     * @param appCode the appCode value to set
     */
    public void setAppCode(String appCode) {
        this.appCode = appCode;
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
}
