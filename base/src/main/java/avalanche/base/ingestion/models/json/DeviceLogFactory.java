package avalanche.base.ingestion.models.json;

import avalanche.base.ingestion.models.DeviceLog;

public class DeviceLogFactory implements LogFactory {

    @Override
    public DeviceLog create() {
        return new DeviceLog();
    }
}

