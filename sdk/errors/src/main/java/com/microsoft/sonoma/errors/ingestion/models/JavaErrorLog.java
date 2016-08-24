package com.microsoft.sonoma.errors.ingestion.models;

import com.microsoft.sonoma.core.ingestion.models.json.JSONUtils;
import com.microsoft.sonoma.errors.ingestion.models.json.JavaExceptionFactory;
import com.microsoft.sonoma.errors.ingestion.models.json.JavaThreadFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.List;

/**
 * Error log for Java platforms (such as Android).
 */
public class JavaErrorLog extends AbstractErrorLog {

    /**
     * Log type.
     */
    public static final String TYPE = "javaError";

    private static final String ARCHITECTURE = "architecture";

    private static final String THREADS = "threads";

    private static final String EXCEPTIONS = "exceptions";

    /**
     * CPU architecture.
     */
    private String architecture;

    /**
     * Exception causal chain as an array.
     * The first element is the top level exception.
     * Last element is the root cause.
     * This is in the same order as in Java causal chain.
     */
    private List<JavaException> exceptions;

    /**
     * Thread stack frames.
     */
    private List<JavaThread> threads;

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Get the architecture value.
     *
     * @return the architecture value
     */
    public String getArchitecture() {
        return this.architecture;
    }

    /**
     * Set the architecture value.
     *
     * @param architecture the architecture value to set
     */
    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    /**
     * Get the exceptions value.
     *
     * @return the exceptions value
     */
    public List<JavaException> getExceptions() {
        return this.exceptions;
    }

    /**
     * Set the exceptions value.
     *
     * @param exceptions the exceptions value to set
     */
    public void setExceptions(List<JavaException> exceptions) {
        this.exceptions = exceptions;
    }

    /**
     * Get the threads value.
     *
     * @return the threads value
     */
    public List<JavaThread> getThreads() {
        return this.threads;
    }

    /**
     * Set the threads value.
     *
     * @param threads the threads value to set
     */
    public void setThreads(List<JavaThread> threads) {
        this.threads = threads;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        super.read(object);
        setArchitecture(object.optString(ARCHITECTURE, null));
        setExceptions(JSONUtils.readArray(object, EXCEPTIONS, JavaExceptionFactory.getInstance()));
        setThreads(JSONUtils.readArray(object, THREADS, JavaThreadFactory.getInstance()));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        super.write(writer);
        JSONUtils.write(writer, ARCHITECTURE, getArchitecture());
        JSONUtils.writeArray(writer, EXCEPTIONS, getExceptions());
        JSONUtils.writeArray(writer, THREADS, getThreads());
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        JavaErrorLog that = (JavaErrorLog) o;

        if (architecture != null ? !architecture.equals(that.architecture) : that.architecture != null)
            return false;
        if (exceptions != null ? !exceptions.equals(that.exceptions) : that.exceptions != null)
            return false;
        return threads != null ? threads.equals(that.threads) : that.threads == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (architecture != null ? architecture.hashCode() : 0);
        result = 31 * result + (exceptions != null ? exceptions.hashCode() : 0);
        result = 31 * result + (threads != null ? threads.hashCode() : 0);
        return result;
    }
}
