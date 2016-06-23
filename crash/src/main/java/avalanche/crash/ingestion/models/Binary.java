package avalanche.crash.ingestion.models;

public class Binary {

    private String startAddress = null;

    private String endAddress = null;

    private String name = null;

    private String cpuType = null;

    private String cpuSubType = null;

    private String uuid = null;

    private String path = null;


    public String getStartAddress() {
        return startAddress;
    }

    public void setStartAddress(String startAddress) {
        this.startAddress = startAddress;
    }


    public String getEndAddress() {
        return endAddress;
    }

    public void setEndAddress(String endAddress) {
        this.endAddress = endAddress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getCpuType() {
        return cpuType;
    }

    public void setCpuType(String cpuType) {
        this.cpuType = cpuType;
    }


    public String getCpuSubType() {
        return cpuSubType;
    }

    public void setCpuSubType(String cpuSubType) {
        this.cpuSubType = cpuSubType;
    }


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     **/
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
