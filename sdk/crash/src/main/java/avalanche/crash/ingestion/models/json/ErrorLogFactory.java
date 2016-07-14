package avalanche.crash.ingestion.models.json;

import avalanche.core.ingestion.models.json.LogFactory;
import avalanche.crash.ingestion.models.ErrorLog;

public class ErrorLogFactory implements LogFactory {

    @Override
    public ErrorLog create() {
        return new ErrorLog();
    }
}
