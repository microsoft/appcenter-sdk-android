/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.json;

import com.microsoft.appcenter.ingestion.models.Model;

import java.util.List;

public interface ModelFactory<M extends Model> {

    M create();

    List<M> createList(int capacity);
}
