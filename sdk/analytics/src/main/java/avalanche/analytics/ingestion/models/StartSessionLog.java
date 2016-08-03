package avalanche.analytics.ingestion.models;

import avalanche.core.ingestion.models.AbstractLog;

/**
 * Start session log.
 */
public class StartSessionLog extends AbstractLog {

    public static final String TYPE = "startSession";

    @Override
    public String getType() {
        return TYPE;
    }
}
