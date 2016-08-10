package avalanche.errors.ingestion.models.json;

import avalanche.core.ingestion.models.json.LogFactory;
import avalanche.errors.ingestion.models.ErrorLog;

public class ErrorLogFactory implements LogFactory {

    private static final ErrorLogFactory sInstance = new ErrorLogFactory();

    private ErrorLogFactory() {
    }

    public static ErrorLogFactory getInstance() {
        return sInstance;
    }

    @Override
    public ErrorLog create() {
        return new ErrorLog();
    }
}
