/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes.ingestion.models;

import com.microsoft.appcenter.ingestion.models.Model;
import com.microsoft.appcenter.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * The StackFrame model.
 */
public class StackFrame implements Model {

    private static final String CLASS_NAME = "className";

    private static final String METHOD_NAME = "methodName";

    private static final String LINE_NUMBER = "lineNumber";

    private static final String FILE_NAME = "fileName";

    /**
     * The fully qualified name of the Class containing the execution point
     * represented by this stack trace element.
     */
    private String className;

    /**
     * The name of the method containing the execution point represented by
     * this stack trace element.
     */
    private String methodName;

    /**
     * The line number of the source line containing the execution point
     * represented by this stack trace element.
     */
    private Integer lineNumber;

    /**
     * The name of the file containing the execution point represented by this
     * stack trace element.
     */
    private String fileName;

    /**
     * Get the className value.
     *
     * @return the className value
     */
    public String getClassName() {
        return this.className;
    }

    /**
     * Set the className value.
     *
     * @param className the className value to set
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Get the methodName value.
     *
     * @return the methodName value
     */
    public String getMethodName() {
        return this.methodName;
    }

    /**
     * Set the methodName value.
     *
     * @param methodName the methodName value to set
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    /**
     * Get the lineNumber value.
     *
     * @return the lineNumber value
     */
    public Integer getLineNumber() {
        return this.lineNumber;
    }

    /**
     * Set the lineNumber value.
     *
     * @param lineNumber the lineNumber value to set
     */
    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * Get the fileName value.
     *
     * @return the fileName value
     */
    public String getFileName() {
        return this.fileName;
    }

    /**
     * Set the fileName value.
     *
     * @param fileName the fileName value to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        setClassName(object.optString(CLASS_NAME, null));
        setMethodName(object.optString(METHOD_NAME, null));
        setLineNumber(JSONUtils.readInteger(object, LINE_NUMBER));
        setFileName(object.optString(FILE_NAME, null));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, CLASS_NAME, getClassName());
        JSONUtils.write(writer, METHOD_NAME, getMethodName());
        JSONUtils.write(writer, LINE_NUMBER, getLineNumber());
        JSONUtils.write(writer, FILE_NAME, getFileName());
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StackFrame that = (StackFrame) o;
        if (className != null ? !className.equals(that.className) : that.className != null) {
            return false;
        }
        if (methodName != null ? !methodName.equals(that.methodName) : that.methodName != null) {
            return false;
        }
        if (lineNumber != null ? !lineNumber.equals(that.lineNumber) : that.lineNumber != null) {
            return false;
        }
        return fileName != null ? fileName.equals(that.fileName) : that.fileName == null;
    }

    @Override
    public int hashCode() {
        int result = className != null ? className.hashCode() : 0;
        result = 31 * result + (methodName != null ? methodName.hashCode() : 0);
        result = 31 * result + (lineNumber != null ? lineNumber.hashCode() : 0);
        result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
        return result;
    }
}
