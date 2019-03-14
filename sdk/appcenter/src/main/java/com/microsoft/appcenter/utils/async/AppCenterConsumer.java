/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.async;

/**
 * Subset of java.util.function.Consumer that works on Java 7.
 * Represents an operation that accepts a single input argument and returns no result.
 * Unlike most other functional interfaces, Consumer is expected to operate via side-effects.
 *
 * @param <T> input argument type.
 */
public interface AppCenterConsumer<T> {

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument.
     */
    void accept(T t);
}
