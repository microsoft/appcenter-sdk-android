/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes.ingestion.models;

import com.microsoft.appcenter.crashes.ingestion.models.json.ExceptionFactory;
import com.microsoft.appcenter.crashes.ingestion.models.json.StackFrameFactory;
import com.microsoft.appcenter.ingestion.models.Model;
import com.microsoft.appcenter.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.List;

import static com.microsoft.appcenter.ingestion.models.CommonProperties.FRAMES;
import static com.microsoft.appcenter.ingestion.models.CommonProperties.TYPE;

/**
 * The Exception model.
 */
public class Exception implements Model {

    private static final String MESSAGE = "message";

    private static final String STACK_TRACE = "stackTrace";

    private static final String INNER_EXCEPTIONS = "innerExceptions";

    private static final String WRAPPER_SDK_NAME = "wrapperSdkName";

    private static final String MINIDUMP_FILE_PATH = "minidumpFilePath";

    /**
     * Exception type (fully qualified class name).
     */
    private String type;

    /**
     * Exception message.
     */
    private String message;

    /**
     * Raw stack trace. Sent when the frames property is either missing or unreliable (used for Xamarin exceptions).
     */
    private String stackTrace;

    /**
     * Exception stack trace elements.
     */
    private List<StackFrame> frames;

    /**
     * Inner exceptions of this exception.
     */
    private List<Exception> innerExceptions;

    /**
     * Name of the wrapper SDK that emitted this exception.
     * Consists of the name of the SDK and the wrapper platform,
     * e.g. "appcenter.xamarin", "hockeysdk.cordova".
     */
    private String wrapperSdkName;

    /**
     * The path to the minidump file. Used for reports from the NDK.
     * This is stored locally but will not be sent to the server.
     */
    private String minidumpFilePath;

    /**
     * Get the type value.
     *
     * @return the type value
     */
    public String getType() {
        return this.type;
    }

    /**
     * Set the type value.
     *
     * @param type the type value to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get the message value.
     *
     * @return the message value
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Set the message value.
     *
     * @param message the message value to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Get the stack trace value.
     *
     * @return the stack trace value
     */
    public String getStackTrace() {
        return stackTrace;
    }

    /**
     * Set stack trace value.
     *
     * @param stackTrace the stack trace value to set.
     */
    @SuppressWarnings("WeakerAccess")
    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    /**
     * Get the frames value.
     *
     * @return the frames value
     */
    public List<StackFrame> getFrames() {
        return this.frames;
    }

    /**
     * Set the frames value.
     *
     * @param frames the frames value to set
     */
    public void setFrames(List<StackFrame> frames) {
        this.frames = frames;
    }

    /**
     * Get the innerExceptions value.
     *
     * @return the innerExceptions value
     */
    public List<Exception> getInnerExceptions() {
        return this.innerExceptions;
    }

    /**
     * Set the innerExceptions value.
     *
     * @param innerExceptions the innerExceptions value to set
     */
    public void setInnerExceptions(List<Exception> innerExceptions) {
        this.innerExceptions = innerExceptions;
    }

    /**
     * Get the wrapperSdkName value.
     *
     * @return the wrapperSdkName value
     */
    @SuppressWarnings("WeakerAccess")
    public String getWrapperSdkName() {
        return wrapperSdkName;
    }

    /**
     * Set the wrapperSdkName value.
     *
     * @param wrapperSdkName the wrapperSdkName value to set
     */
    public void setWrapperSdkName(String wrapperSdkName) {
        this.wrapperSdkName = wrapperSdkName;
    }

    /**
     * Get the minidump file path.
     *
     * @return the minidump file path.
     */
    public String getMinidumpFilePath() {
        return minidumpFilePath;
    }

    /**
     * Set minidump file path.
     *
     * @param minidumpFilePath the minidump file path to set.
     */
    public void setMinidumpFilePath(String minidumpFilePath) {
        this.minidumpFilePath = minidumpFilePath;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        setType(object.optString(TYPE, null));
        setMessage(object.optString(MESSAGE, null));
        setStackTrace(object.optString(STACK_TRACE, null));
        setFrames(JSONUtils.readArray(object, FRAMES, StackFrameFactory.getInstance()));
        setInnerExceptions(JSONUtils.readArray(object, INNER_EXCEPTIONS, ExceptionFactory.getInstance()));
        setWrapperSdkName(object.optString(WRAPPER_SDK_NAME, null));
        setMinidumpFilePath(object.optString(MINIDUMP_FILE_PATH, null));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, TYPE, getType());
        JSONUtils.write(writer, MESSAGE, getMessage());
        JSONUtils.write(writer, STACK_TRACE, getStackTrace());
        JSONUtils.writeArray(writer, FRAMES, getFrames());
        JSONUtils.writeArray(writer, INNER_EXCEPTIONS, getInnerExceptions());
        JSONUtils.write(writer, WRAPPER_SDK_NAME, getWrapperSdkName());
        JSONUtils.write(writer, MINIDUMP_FILE_PATH, getMinidumpFilePath());
    }

    @SuppressWarnings("EqualsReplaceableByObjectsCall")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Exception exception = (Exception) o;

        if (type != null ? !type.equals(exception.type) : exception.type != null) return false;
        if (message != null ? !message.equals(exception.message) : exception.message != null)
            return false;
        if (stackTrace != null ? !stackTrace.equals(exception.stackTrace) : exception.stackTrace != null)
            return false;
        if (frames != null ? !frames.equals(exception.frames) : exception.frames != null)
            return false;
        if (innerExceptions != null ? !innerExceptions.equals(exception.innerExceptions) : exception.innerExceptions != null)
            return false;
        if (wrapperSdkName != null ? !wrapperSdkName.equals(exception.wrapperSdkName) : exception.wrapperSdkName != null)
            return false;
        return minidumpFilePath != null ? minidumpFilePath.equals(exception.minidumpFilePath) : exception.minidumpFilePath == null;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (stackTrace != null ? stackTrace.hashCode() : 0);
        result = 31 * result + (frames != null ? frames.hashCode() : 0);
        result = 31 * result + (innerExceptions != null ? innerExceptions.hashCode() : 0);
        result = 31 * result + (wrapperSdkName != null ? wrapperSdkName.hashCode() : 0);
        result = 31 * result + (minidumpFilePath != null ? minidumpFilePath.hashCode() : 0);
        return result;
    }
}
