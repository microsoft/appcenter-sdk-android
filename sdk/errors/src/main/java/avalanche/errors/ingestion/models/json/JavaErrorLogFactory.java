package avalanche.errors.ingestion.models.json;

import avalanche.core.ingestion.models.json.LogFactory;
import avalanche.errors.ingestion.models.JavaErrorLog;

public class JavaErrorLogFactory implements LogFactory {

    private static final JavaErrorLogFactory sInstance = new JavaErrorLogFactory();

    private JavaErrorLogFactory() {
    }

    public static JavaErrorLogFactory getInstance() {
        return sInstance;
    }

    @Override
    public JavaErrorLog create() {
        return new JavaErrorLog();
    }
}
