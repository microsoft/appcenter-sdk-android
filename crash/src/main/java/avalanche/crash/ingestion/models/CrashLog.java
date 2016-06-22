package avalanche.crash.ingestion.models;

import avalanche.base.ingestion.models.InSessionLog;

import java.util.List;
import java.util.Map;


public class CrashLog extends InSessionLog {

    private String type = null;
    private Map<String, String> properties = null;
    private String sid = null;
    private String id = null;
    private String process = null;
    private Integer processId = null;
    private String parentProcess = null;
    private Integer parentProcessId = null;
    private Integer crashThread = null;
    private String applicationPath = null;
    private String exceptionType = null;
    private String exceptionCode = null;
    private String exceptionAddress = null;
    private String exceptionReason = null;
    private List<Thread> threads = null;
    private List<Binary> binaries = null;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }


    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    public Integer getProcessId() {
        return processId;
    }

    public void setProcessId(Integer processId) {
        this.processId = processId;
    }

    public String getParentProcess() {
        return parentProcess;
    }

    public void setParentProcess(String parentProcess) {
        this.parentProcess = parentProcess;
    }

    public Integer getParentProcessId() {
        return parentProcessId;
    }

    public void setParentProcessId(Integer parentProcessId) {
        this.parentProcessId = parentProcessId;
    }

    public Integer getCrashThread() {
        return crashThread;
    }

    public void setCrashThread(Integer crashThread) {
        this.crashThread = crashThread;
    }


    public String getApplicationPath() {
        return applicationPath;
    }

    public void setApplicationPath(String applicationPath) {
        this.applicationPath = applicationPath;
    }


    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }


    public String getExceptionCode() {
        return exceptionCode;
    }

    public void setExceptionCode(String exceptionCode) {
        this.exceptionCode = exceptionCode;
    }


    public String getExceptionAddress() {
        return exceptionAddress;
    }

    public void setExceptionAddress(String exceptionAddress) {
        this.exceptionAddress = exceptionAddress;
    }


    public String getExceptionReason() {
        return exceptionReason;
    }

    public void setExceptionReason(String exceptionReason) {
        this.exceptionReason = exceptionReason;
    }


    public List<Thread> getThreads() {
        return threads;
    }

    public void setThreads(List<Thread> threads) {
        this.threads = threads;
    }


    public List<Binary> getBinaries() {
        return binaries;
    }

    public void setBinaries(List<Binary> binaries) {
        this.binaries = binaries;
    }
}
