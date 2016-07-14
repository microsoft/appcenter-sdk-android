package avalanche.base.ingestion.models;

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
