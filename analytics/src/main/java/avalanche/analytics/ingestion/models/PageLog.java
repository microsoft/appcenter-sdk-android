package avalanche.analytics.ingestion.models;

import avalanche.base.ingestion.models.InSessionLog;

/**
 * Page log.
 */
public class PageLog extends InSessionLog {

    /**
     * Name of the page.
     */
    private String name;

    @Override
    public String getType() {
        return "page";
    }

    /**
     * Get the name value.
     *
     * @return the name value
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set the name value.
     *
     * @param name the name value to set
     */
    public void setName(String name) {
        this.name = name;
    }
}
