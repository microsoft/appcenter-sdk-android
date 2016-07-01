package avalanche.analytics.ingestion.models;

import avalanche.base.ingestion.models.AbstractLog;

/**
 * End session log.
 */
public class EndSessionLog extends AbstractLog {

    public static final String TYPE = "endSession";

    @Override
    public String getType() {
        return TYPE;
    }
}
