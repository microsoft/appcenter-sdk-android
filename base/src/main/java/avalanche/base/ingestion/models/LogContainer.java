package avalanche.base.ingestion.models;

import java.util.List;

/**
 * The LogContainer model.
 */
public class LogContainer {

    /**
     * Unique install identifier.
     */
    private String installId;

    /**
     * Application identifier.
     */
    private String appId;

    /**
     * The list of logs.
     */
    private List<Log> logs;

    /**
     * Get the installId value.
     *
     * @return the installId value
     */
    public String getInstallId() {
        return this.installId;
    }

    /**
     * Set the installId value.
     *
     * @param installId the installId value to set
     */
    public void setInstallId(String installId) {
        this.installId = installId;
    }

    /**
     * Get the appId value.
     *
     * @return the appId value
     */
    public String getAppId() {
        return this.appId;
    }

    /**
     * Set the appId value.
     *
     * @param appId the appId value to set
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * Get the logs value.
     *
     * @return the logs value
     */
    public List<Log> getLogs() {
        return this.logs;
    }

    /**
     * Set the logs value.
     *
     * @param logs the logs value to set
     */
    public void setLogs(List<Log> logs) {
        this.logs = logs;
    }
}
