package avalanche.base.ingestion.models.json;

import avalanche.base.ingestion.models.DeviceLog;
import avalanche.base.ingestion.models.Log;

public class DeviceLogFactory implements LogFactory {

    @Override
    public Log create() {
        return new DeviceLog();
    }
}

