/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models;

import java.util.List;

/**
 * The LogContainer model.
 */
public class LogContainer {

    /**
     * The list of logs.
     */
    private List<Log> logs;

    /**
     * Get the logs value.
     *
     * @return the logs value
     */
    public List<Log> getLogs() {
        return this.logs;
    }

    /**
     * Set the logs value.
     *
     * @param logs the logs value to set
     */
    public void setLogs(List<Log> logs) {
        this.logs = logs;
    }

    @SuppressWarnings("EqualsReplaceableByObjectsCall")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LogContainer container = (LogContainer) o;
        return logs != null ? logs.equals(container.logs) : container.logs == null;
    }

    @Override
    public int hashCode() {
        return logs != null ? logs.hashCode() : 0;
    }
}
