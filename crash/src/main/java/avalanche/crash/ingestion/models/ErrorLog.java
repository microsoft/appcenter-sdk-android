package avalanche.crash.ingestion.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.List;

import avalanche.base.ingestion.models.LogWithProperties;
import avalanche.base.ingestion.models.json.JSONUtils;
import avalanche.base.ingestion.models.utils.LogUtils;
import avalanche.crash.ingestion.models.json.BinaryFactory;
import avalanche.crash.ingestion.models.json.ExceptionFactory;
import avalanche.crash.ingestion.models.json.ThreadFactory;

import static avalanche.base.ingestion.models.CommonProperties.ID;

/**
 * Error log.
 */
public class ErrorLog extends LogWithProperties {

    private static final String TYPE = "error";

    private static final String PROCESS = "process";

    private static final String PROCESS_ID = "processId";

    private static final String PARENT_PROCESS = "parentProcess";

    private static final String PARENT_PROCESS_ID = "parentProcessId";

    private static final String CRASH_THREAD = "crashThread";

    private static final String APPLICATION_PATH = "applicationPath";

    private static final String APP_LAUNCH_T_OFFSET = "appLaunchTOffset";

    private static final String EXCEPTION_TYPE = "exceptionType";

    private static final String EXCEPTION_CODE = "exceptionCode";

    private static final String EXCEPTION_ADDRESS = "exceptionAddress";

    private static final String EXCEPTION_REASON = "exceptionReason";

    private static final String FATAL = "fatal";

    private static final String THREADS = "threads";

    private static final String EXCEPTIONS = "exceptions";

    private static final String BINARIES = "binaries";

    /**
     * Crash identifier.
     */
    private String id;

    /**
     * Name of the process that crashes.
     */
    private String process;

    /**
     * Process identifier.
     */
    private Integer processId;

    /**
     * Name of the parent's process.
     */
    private String parentProcess;

    /**
     * Parent's process identifier.
     */
    private Integer parentProcessId;

    /**
     * Id of the thread that crashes.
     */
    private Integer crashThread;

    /**
     * Path to the application.
     */
    private String applicationPath;

    /**
     * Corresponds to the number of milliseconds elapsed between the time the
     * app was launched and the log was sent.
     */
    private Long appLaunchTOffset;

    /**
     * Exception type.
     */
    private String exceptionType;

    /**
     * Exception code.
     */
    private String exceptionCode;

    /**
     * Exception address.
     */
    private String exceptionAddress;

    /**
     * Exception reason.
     */
    private String exceptionReason;

    /**
     * Crash or handled exception.
     */
    private Boolean fatal;

    /**
     * Thread stacktraces associated to the crash.
     */
    private List<Thread> threads;

    /**
     * Exception stacktraces associated to the crash.
     */
    private List<Exception> exceptions;

    /**
     * Binaries associated to the crash with their associated addresses (used
     * only on iOS to symbolicate the stacktrace).
     */
    private List<Binary> binaries;

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Get the id value.
     *
     * @return the id value
     */
    public String getId() {
        return this.id;
    }

    /**
     * Set the id value.
     *
     * @param id the id value to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the process value.
     *
     * @return the process value
     */
    public String getProcess() {
        return this.process;
    }

    /**
     * Set the process value.
     *
     * @param process the process value to set
     */
    public void setProcess(String process) {
        this.process = process;
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
     * Get the parentProcess value.
     *
     * @return the parentProcess value
     */
    public String getParentProcess() {
        return this.parentProcess;
    }

    /**
     * Set the parentProcess value.
     *
     * @param parentProcess the parentProcess value to set
     */
    public void setParentProcess(String parentProcess) {
        this.parentProcess = parentProcess;
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
     * Get the crashThread value.
     *
     * @return the crashThread value
     */
    public Integer getCrashThread() {
        return this.crashThread;
    }

    /**
     * Set the crashThread value.
     *
     * @param crashThread the crashThread value to set
     */
    public void setCrashThread(Integer crashThread) {
        this.crashThread = crashThread;
    }

    /**
     * Get the applicationPath value.
     *
     * @return the applicationPath value
     */
    public String getApplicationPath() {
        return this.applicationPath;
    }

    /**
     * Set the applicationPath value.
     *
     * @param applicationPath the applicationPath value to set
     */
    public void setApplicationPath(String applicationPath) {
        this.applicationPath = applicationPath;
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

    /**
     * Get the exceptionType value.
     *
     * @return the exceptionType value
     */
    public String getExceptionType() {
        return this.exceptionType;
    }

    /**
     * Set the exceptionType value.
     *
     * @param exceptionType the exceptionType value to set
     */
    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    /**
     * Get the exceptionCode value.
     *
     * @return the exceptionCode value
     */
    public String getExceptionCode() {
        return this.exceptionCode;
    }

    /**
     * Set the exceptionCode value.
     *
     * @param exceptionCode the exceptionCode value to set
     */
    public void setExceptionCode(String exceptionCode) {
        this.exceptionCode = exceptionCode;
    }

    /**
     * Get the exceptionAddress value.
     *
     * @return the exceptionAddress value
     */
    public String getExceptionAddress() {
        return this.exceptionAddress;
    }

    /**
     * Set the exceptionAddress value.
     *
     * @param exceptionAddress the exceptionAddress value to set
     */
    public void setExceptionAddress(String exceptionAddress) {
        this.exceptionAddress = exceptionAddress;
    }

    /**
     * Get the exceptionReason value.
     *
     * @return the exceptionReason value
     */
    public String getExceptionReason() {
        return this.exceptionReason;
    }

    /**
     * Set the exceptionReason value.
     *
     * @param exceptionReason the exceptionReason value to set
     */
    public void setExceptionReason(String exceptionReason) {
        this.exceptionReason = exceptionReason;
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
     * Get the threads value.
     *
     * @return the threads value
     */
    public List<Thread> getThreads() {
        return this.threads;
    }

    /**
     * Set the threads value.
     *
     * @param threads the threads value to set
     */
    public void setThreads(List<Thread> threads) {
        this.threads = threads;
    }

    /**
     * Get the exceptions value.
     *
     * @return the exceptions value
     */
    public List<Exception> getExceptions() {
        return this.exceptions;
    }

    /**
     * Set the exceptions value.
     *
     * @param exceptions the exceptions value to set
     */
    public void setExceptions(List<Exception> exceptions) {
        this.exceptions = exceptions;
    }

    /**
     * Get the binaries value.
     *
     * @return the binaries value
     */
    public List<Binary> getBinaries() {
        return this.binaries;
    }

    /**
     * Set the binaries value.
     *
     * @param binaries the binaries value to set
     */
    public void setBinaries(List<Binary> binaries) {
        this.binaries = binaries;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        super.read(object);
        setId(object.getString(ID));
        setProcess(object.optString(PROCESS, null));
        setProcessId(JSONUtils.readInteger(object, PROCESS_ID));
        setParentProcess(object.optString(PARENT_PROCESS, null));
        setParentProcessId(JSONUtils.readInteger(object, PARENT_PROCESS_ID));
        setCrashThread(JSONUtils.readInteger(object, CRASH_THREAD));
        setApplicationPath(object.optString(APPLICATION_PATH, null));
        setAppLaunchTOffset(JSONUtils.readLong(object, APP_LAUNCH_T_OFFSET));
        setExceptionType(object.getString(EXCEPTION_TYPE));
        setExceptionCode(object.optString(EXCEPTION_CODE, null));
        setExceptionAddress(object.optString(EXCEPTION_ADDRESS, null));
        setExceptionReason(object.getString(EXCEPTION_REASON));
        setFatal(JSONUtils.readBoolean(object, FATAL));
        setThreads(JSONUtils.readArray(object, THREADS, ThreadFactory.getInstance()));
        setExceptions(JSONUtils.readArray(object, EXCEPTIONS, ExceptionFactory.getInstance()));
        setBinaries(JSONUtils.readArray(object, BINARIES, BinaryFactory.getInstance()));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        super.write(writer);
        JSONUtils.write(writer, ID, getId(), true);
        JSONUtils.write(writer, PROCESS, getProcess(), false);
        JSONUtils.write(writer, PROCESS_ID, getProcessId(), false);
        JSONUtils.write(writer, PARENT_PROCESS, getParentProcess(), false);
        JSONUtils.write(writer, PARENT_PROCESS_ID, getParentProcessId(), false);
        JSONUtils.write(writer, CRASH_THREAD, getCrashThread(), false);
        JSONUtils.write(writer, APPLICATION_PATH, getApplicationPath(), false);
        JSONUtils.write(writer, APP_LAUNCH_T_OFFSET, getAppLaunchTOffset(), false);
        JSONUtils.write(writer, EXCEPTION_TYPE, getExceptionType(), true);
        JSONUtils.write(writer, EXCEPTION_CODE, getExceptionCode(), false);
        JSONUtils.write(writer, EXCEPTION_ADDRESS, getExceptionAddress(), false);
        JSONUtils.write(writer, EXCEPTION_REASON, getExceptionReason(), true);
        JSONUtils.write(writer, FATAL, getFatal(), false);
        JSONUtils.writeArray(writer, THREADS, getThreads());
        JSONUtils.writeArray(writer, EXCEPTIONS, getExceptions());
        JSONUtils.writeArray(writer, BINARIES, getBinaries());
    }

    @Override
    public void validate() throws IllegalArgumentException {
        super.validate();
        LogUtils.checkNotNull(ID, getId());
        LogUtils.checkNotNull(EXCEPTION_TYPE, getExceptionType());
        LogUtils.checkNotNull(EXCEPTION_REASON, getExceptionReason());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ErrorLog errorLog = (ErrorLog) o;

        if (id != null ? !id.equals(errorLog.id) : errorLog.id != null) return false;
        if (process != null ? !process.equals(errorLog.process) : errorLog.process != null)
            return false;
        if (processId != null ? !processId.equals(errorLog.processId) : errorLog.processId != null)
            return false;
        if (parentProcess != null ? !parentProcess.equals(errorLog.parentProcess) : errorLog.parentProcess != null)
            return false;
        if (parentProcessId != null ? !parentProcessId.equals(errorLog.parentProcessId) : errorLog.parentProcessId != null)
            return false;
        if (crashThread != null ? !crashThread.equals(errorLog.crashThread) : errorLog.crashThread != null)
            return false;
        if (applicationPath != null ? !applicationPath.equals(errorLog.applicationPath) : errorLog.applicationPath != null)
            return false;
        if (appLaunchTOffset != null ? !appLaunchTOffset.equals(errorLog.appLaunchTOffset) : errorLog.appLaunchTOffset != null)
            return false;
        if (exceptionType != null ? !exceptionType.equals(errorLog.exceptionType) : errorLog.exceptionType != null)
            return false;
        if (exceptionCode != null ? !exceptionCode.equals(errorLog.exceptionCode) : errorLog.exceptionCode != null)
            return false;
        if (exceptionAddress != null ? !exceptionAddress.equals(errorLog.exceptionAddress) : errorLog.exceptionAddress != null)
            return false;
        if (exceptionReason != null ? !exceptionReason.equals(errorLog.exceptionReason) : errorLog.exceptionReason != null)
            return false;
        if (fatal != null ? !fatal.equals(errorLog.fatal) : errorLog.fatal != null) return false;
        if (threads != null ? !threads.equals(errorLog.threads) : errorLog.threads != null)
            return false;
        if (exceptions != null ? !exceptions.equals(errorLog.exceptions) : errorLog.exceptions != null)
            return false;
        return binaries != null ? binaries.equals(errorLog.binaries) : errorLog.binaries == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (process != null ? process.hashCode() : 0);
        result = 31 * result + (processId != null ? processId.hashCode() : 0);
        result = 31 * result + (parentProcess != null ? parentProcess.hashCode() : 0);
        result = 31 * result + (parentProcessId != null ? parentProcessId.hashCode() : 0);
        result = 31 * result + (crashThread != null ? crashThread.hashCode() : 0);
        result = 31 * result + (applicationPath != null ? applicationPath.hashCode() : 0);
        result = 31 * result + (appLaunchTOffset != null ? appLaunchTOffset.hashCode() : 0);
        result = 31 * result + (exceptionType != null ? exceptionType.hashCode() : 0);
        result = 31 * result + (exceptionCode != null ? exceptionCode.hashCode() : 0);
        result = 31 * result + (exceptionAddress != null ? exceptionAddress.hashCode() : 0);
        result = 31 * result + (exceptionReason != null ? exceptionReason.hashCode() : 0);
        result = 31 * result + (fatal != null ? fatal.hashCode() : 0);
        result = 31 * result + (threads != null ? threads.hashCode() : 0);
        result = 31 * result + (exceptions != null ? exceptions.hashCode() : 0);
        result = 31 * result + (binaries != null ? binaries.hashCode() : 0);
        return result;
    }
}
