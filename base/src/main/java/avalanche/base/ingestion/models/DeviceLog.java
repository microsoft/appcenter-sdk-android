package avalanche.base.ingestion.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import avalanche.base.ingestion.models.json.JSONUtils;
import avalanche.base.ingestion.models.utils.LogUtils;

/**
 * Device characteristic log.
 */
public class DeviceLog extends AbstractLog {

    /**
     * The log type.
     */
    public static final String TYPE = "device";

    private static final String SDK_VERSION = "sdkVersion";

    private static final String MODEL = "model";

    private static final String OEM_NAME = "oemName";

    private static final String OS_NAME = "osName";

    private static final String OS_VERSION = "osVersion";

    private static final String OS_API_LEVEL = "osApiLevel";

    private static final String LOCALE = "locale";

    private static final String TIME_ZONE_OFFSET = "timeZoneOffset";

    private static final String SCREEN_SIZE = "screenSize";

    private static final String APP_VERSION = "appVersion";

    private static final String CARRIER_NAME = "carrierName";

    private static final String CARRIER_COUNTRY = "carrierCountry";

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

    @Override
    public String getType() {
        return TYPE;
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

    @Override
    public void read(JSONObject object) throws JSONException {
        super.read(object);
        setSdkVersion(object.getString(SDK_VERSION));
        setModel(object.getString(MODEL));
        setOemName(object.getString(OEM_NAME));
        setOsName(object.getString(OS_NAME));
        setOsVersion(object.getString(OS_VERSION));
        if (object.has(OS_API_LEVEL))
            setOsApiLevel(object.getInt(OS_API_LEVEL));
        setLocale(object.getString(LOCALE));
        setTimeZoneOffset(object.getInt(TIME_ZONE_OFFSET));
        setScreenSize(object.getString(SCREEN_SIZE));
        setAppVersion(object.getString(APP_VERSION));
        setCarrierName(object.optString(CARRIER_NAME, null));
        setCarrierCountry(object.optString(CARRIER_COUNTRY, null));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        super.write(writer);
        writer.key(SDK_VERSION).value(getSdkVersion());
        writer.key(MODEL).value(getModel());
        writer.key(OEM_NAME).value(getOemName());
        writer.key(OS_NAME).value(getOsName());
        writer.key(OS_VERSION).value(getOsVersion());
        JSONUtils.write(writer, OS_API_LEVEL, getOsApiLevel(), false);
        writer.key(LOCALE).value(getLocale());
        writer.key(TIME_ZONE_OFFSET).value(getTimeZoneOffset());
        writer.key(SCREEN_SIZE).value(getScreenSize());
        writer.key(APP_VERSION).value(getAppVersion());
        JSONUtils.write(writer, CARRIER_NAME, getCarrierName(), false);
        JSONUtils.write(writer, CARRIER_COUNTRY, getCarrierCountry(), false);
    }

    @Override
    public void validate() throws IllegalArgumentException {
        super.validate();
        LogUtils.checkNotNull(SDK_VERSION, getSdkVersion());
        LogUtils.checkNotNull(MODEL, getModel());
        LogUtils.checkNotNull(OEM_NAME, getOemName());
        LogUtils.checkNotNull(OS_NAME, getOsName());
        LogUtils.checkNotNull(OS_VERSION, getOsVersion());
        LogUtils.checkNotNull(LOCALE, getLocale());
        LogUtils.checkNotNull(TIME_ZONE_OFFSET, getTimeZoneOffset());
        LogUtils.checkNotNull(SCREEN_SIZE, getScreenSize());
        LogUtils.checkNotNull(APP_VERSION, getAppVersion());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        DeviceLog deviceLog = (DeviceLog) o;

        if (sdkVersion != null ? !sdkVersion.equals(deviceLog.sdkVersion) : deviceLog.sdkVersion != null)
            return false;
        if (model != null ? !model.equals(deviceLog.model) : deviceLog.model != null) return false;
        if (oemName != null ? !oemName.equals(deviceLog.oemName) : deviceLog.oemName != null)
            return false;
        if (osName != null ? !osName.equals(deviceLog.osName) : deviceLog.osName != null)
            return false;
        if (osVersion != null ? !osVersion.equals(deviceLog.osVersion) : deviceLog.osVersion != null)
            return false;
        if (osApiLevel != null ? !osApiLevel.equals(deviceLog.osApiLevel) : deviceLog.osApiLevel != null)
            return false;
        if (locale != null ? !locale.equals(deviceLog.locale) : deviceLog.locale != null)
            return false;
        if (timeZoneOffset != null ? !timeZoneOffset.equals(deviceLog.timeZoneOffset) : deviceLog.timeZoneOffset != null)
            return false;
        if (screenSize != null ? !screenSize.equals(deviceLog.screenSize) : deviceLog.screenSize != null)
            return false;
        if (appVersion != null ? !appVersion.equals(deviceLog.appVersion) : deviceLog.appVersion != null)
            return false;
        if (carrierName != null ? !carrierName.equals(deviceLog.carrierName) : deviceLog.carrierName != null)
            return false;
        return carrierCountry != null ? carrierCountry.equals(deviceLog.carrierCountry) : deviceLog.carrierCountry == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (sdkVersion != null ? sdkVersion.hashCode() : 0);
        result = 31 * result + (model != null ? model.hashCode() : 0);
        result = 31 * result + (oemName != null ? oemName.hashCode() : 0);
        result = 31 * result + (osName != null ? osName.hashCode() : 0);
        result = 31 * result + (osVersion != null ? osVersion.hashCode() : 0);
        result = 31 * result + (osApiLevel != null ? osApiLevel.hashCode() : 0);
        result = 31 * result + (locale != null ? locale.hashCode() : 0);
        result = 31 * result + (timeZoneOffset != null ? timeZoneOffset.hashCode() : 0);
        result = 31 * result + (screenSize != null ? screenSize.hashCode() : 0);
        result = 31 * result + (appVersion != null ? appVersion.hashCode() : 0);
        result = 31 * result + (carrierName != null ? carrierName.hashCode() : 0);
        result = 31 * result + (carrierCountry != null ? carrierCountry.hashCode() : 0);
        return result;
    }
}
