package avalanche.errors.ingestion.models.json;

import avalanche.core.ingestion.models.json.LogFactory;
import avalanche.errors.ingestion.models.ErrorLog;

public class ErrorLogFactory implements LogFactory {

    @Override
    public ErrorLog create() {
        return new ErrorLog();
    }
}
