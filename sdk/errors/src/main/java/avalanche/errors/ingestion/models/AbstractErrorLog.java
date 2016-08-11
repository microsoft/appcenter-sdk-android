package avalanche.errors.ingestion.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.UUID;

import avalanche.core.ingestion.models.AbstractLog;
import avalanche.core.ingestion.models.json.JSONUtils;

import static avalanche.core.ingestion.models.CommonProperties.ID;

/**
 * Abstract error log.
 */
public abstract class AbstractErrorLog extends AbstractLog {

    private static final String PROCESS_ID = "processId";

    private static final String PROCESS_NAME = "processName";

    private static final String PARENT_PROCESS_ID = "parentProcessId";

    private static final String PARENT_PROCESS_NAME = "parentProcessName";

    private static final String CPU_TYPE = "cpuType";

    private static final String CPU_SUB_TYPE = "cpuSubType";

    private static final String ERROR_THREAD_ID = "errorThreadId";

    private static final String ERROR_THREAD_NAME = "errorThreadName";

    private static final String FATAL = "fatal";

    private static final String APP_LAUNCH_T_OFFSET = "appLaunchTOffset";

    /**
     * Error identifier.
     */
    private UUID id;

    /**
     * Process identifier.
     */
    private Integer processId;

    /**
     * Process name.
     */
    private String processName;

    /**
     * Parent's process identifier.
     */
    private Integer parentProcessId;

    /**
     * Parent's process name.
     */
    private String parentProcessName;

    /**
     * The cpuType property.
     */
    private Integer cpuType;

    /**
     * The cpuSubType property.
     */
    private Integer cpuSubType;

    /**
     * Error thread identifier.
     */
    private Long errorThreadId;

    /**
     * Error thread name.
     */
    private String errorThreadName;

    /**
     * If true, this error report is an application crash.
     */
    private Boolean fatal;

    /**
     * Corresponds to the number of milliseconds elapsed between the time the
     * error occurred and the app was launched.
     */
    private Long appLaunchTOffset;

    /**
     * Get the id value.
     *
     * @return the id value
     */
    public UUID getId() {
        return this.id;
    }

    /**
     * Set the id value.
     *
     * @param id the id value to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Get the processId value.
     *
     * @return the processId value
     */
    public Integer getProcessId() {
        return this.processId;
    }

    /**
     * Set the processId value.
     *
     * @param processId the processId value to set
     */
    public void setProcessId(Integer processId) {
        this.processId = processId;
    }

    /**
     * Get the processName value.
     *
     * @return the processName value
     */
    public String getProcessName() {
        return this.processName;
    }

    /**
     * Set the processName value.
     *
     * @param processName the processName value to set
     */
    public void setProcessName(String processName) {
        this.processName = processName;
    }

    /**
     * Get the parentProcessId value.
     *
     * @return the parentProcessId value
     */
    public Integer getParentProcessId() {
        return this.parentProcessId;
    }

    /**
     * Set the parentProcessId value.
     *
     * @param parentProcessId the parentProcessId value to set
     */
    public void setParentProcessId(Integer parentProcessId) {
        this.parentProcessId = parentProcessId;
    }

    /**
     * Get the parentProcessName value.
     *
     * @return the parentProcessName value
     */
    public String getParentProcessName() {
        return this.parentProcessName;
    }

    /**
     * Set the parentProcessName value.
     *
     * @param parentProcessName the parentProcessName value to set
     */
    public void setParentProcessName(String parentProcessName) {
        this.parentProcessName = parentProcessName;
    }

    /**
     * Get the cpuType value.
     *
     * @return the cpuType value
     */
    public Integer getCpuType() {
        return this.cpuType;
    }

    /**
     * Set the cpuType value.
     *
     * @param cpuType the cpuType value to set
     */
    public void setCpuType(Integer cpuType) {
        this.cpuType = cpuType;
    }

    /**
     * Get the cpuSubType value.
     *
     * @return the cpuSubType value
     */
    public Integer getCpuSubType() {
        return this.cpuSubType;
    }

    /**
     * Set the cpuSubType value.
     *
     * @param cpuSubType the cpuSubType value to set
     */
    public void setCpuSubType(Integer cpuSubType) {
        this.cpuSubType = cpuSubType;
    }

    /**
     * Get the errorThreadId value.
     *
     * @return the errorThreadId value
     */
    public Long getErrorThreadId() {
        return this.errorThreadId;
    }

    /**
     * Set the errorThreadId value.
     *
     * @param errorThreadId the errorThreadId value to set
     */
    public void setErrorThreadId(Long errorThreadId) {
        this.errorThreadId = errorThreadId;
    }

    /**
     * Get the errorThreadName value.
     *
     * @return the errorThreadName value
     */
    public String getErrorThreadName() {
        return this.errorThreadName;
    }

    /**
     * Set the errorThreadName value.
     *
     * @param errorThreadName the errorThreadName value to set
     */
    public void setErrorThreadName(String errorThreadName) {
        this.errorThreadName = errorThreadName;
    }

    /**
     * Get the fatal value.
     *
     * @return the fatal value
     */
    public Boolean getFatal() {
        return this.fatal;
    }

    /**
     * Set the fatal value.
     *
     * @param fatal the fatal value to set
     */
    public void setFatal(Boolean fatal) {
        this.fatal = fatal;
    }

    /**
     * Get the appLaunchTOffset value.
     *
     * @return the appLaunchTOffset value
     */
    public Long getAppLaunchTOffset() {
        return this.appLaunchTOffset;
    }

    /**
     * Set the appLaunchTOffset value.
     *
     * @param appLaunchTOffset the appLaunchTOffset value to set
     */
    public void setAppLaunchTOffset(Long appLaunchTOffset) {
        this.appLaunchTOffset = appLaunchTOffset;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        super.read(object);
        setId(UUID.fromString(object.getString(ID)));
        setProcessId(JSONUtils.readInteger(object, PROCESS_ID));
        setProcessName(object.optString(PROCESS_NAME, null));
        setParentProcessId(JSONUtils.readInteger(object, PARENT_PROCESS_ID));
        setParentProcessName(object.optString(PARENT_PROCESS_NAME, null));
        setCpuType(JSONUtils.readInteger(object, CPU_TYPE));
        setCpuSubType(JSONUtils.readInteger(object, CPU_SUB_TYPE));
        setErrorThreadId(JSONUtils.readLong(object, ERROR_THREAD_ID));
        setErrorThreadName(object.optString(ERROR_THREAD_NAME, null));
        setFatal(JSONUtils.readBoolean(object, FATAL));
        setAppLaunchTOffset(JSONUtils.readLong(object, APP_LAUNCH_T_OFFSET));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        super.write(writer);
        JSONUtils.write(writer, ID, getId());
        JSONUtils.write(writer, PROCESS_ID, getProcessId());
        JSONUtils.write(writer, PROCESS_NAME, getProcessName());
        JSONUtils.write(writer, PARENT_PROCESS_ID, getParentProcessId());
        JSONUtils.write(writer, PARENT_PROCESS_NAME, getParentProcessName());
        JSONUtils.write(writer, CPU_TYPE, getCpuType());
        JSONUtils.write(writer, CPU_SUB_TYPE, getCpuSubType());
        JSONUtils.write(writer, ERROR_THREAD_ID, getErrorThreadId());
        JSONUtils.write(writer, ERROR_THREAD_NAME, getErrorThreadName());
        JSONUtils.write(writer, FATAL, getFatal());
        JSONUtils.write(writer, APP_LAUNCH_T_OFFSET, getAppLaunchTOffset());
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        AbstractErrorLog that = (AbstractErrorLog) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (processId != null ? !processId.equals(that.processId) : that.processId != null)
            return false;
        if (processName != null ? !processName.equals(that.processName) : that.processName != null)
            return false;
        if (parentProcessId != null ? !parentProcessId.equals(that.parentProcessId) : that.parentProcessId != null)
            return false;
        if (parentProcessName != null ? !parentProcessName.equals(that.parentProcessName) : that.parentProcessName != null)
            return false;
        if (cpuType != null ? !cpuType.equals(that.cpuType) : that.cpuType != null) return false;
        if (cpuSubType != null ? !cpuSubType.equals(that.cpuSubType) : that.cpuSubType != null)
            return false;
        if (errorThreadId != null ? !errorThreadId.equals(that.errorThreadId) : that.errorThreadId != null)
            return false;
        if (errorThreadName != null ? !errorThreadName.equals(that.errorThreadName) : that.errorThreadName != null)
            return false;
        if (fatal != null ? !fatal.equals(that.fatal) : that.fatal != null) return false;
        return appLaunchTOffset != null ? appLaunchTOffset.equals(that.appLaunchTOffset) : that.appLaunchTOffset == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (processId != null ? processId.hashCode() : 0);
        result = 31 * result + (processName != null ? processName.hashCode() : 0);
        result = 31 * result + (parentProcessId != null ? parentProcessId.hashCode() : 0);
        result = 31 * result + (parentProcessName != null ? parentProcessName.hashCode() : 0);
        result = 31 * result + (cpuType != null ? cpuType.hashCode() : 0);
        result = 31 * result + (cpuSubType != null ? cpuSubType.hashCode() : 0);
        result = 31 * result + (errorThreadId != null ? errorThreadId.hashCode() : 0);
        result = 31 * result + (errorThreadName != null ? errorThreadName.hashCode() : 0);
        result = 31 * result + (fatal != null ? fatal.hashCode() : 0);
        result = 31 * result + (appLaunchTOffset != null ? appLaunchTOffset.hashCode() : 0);
        return result;
    }
}
