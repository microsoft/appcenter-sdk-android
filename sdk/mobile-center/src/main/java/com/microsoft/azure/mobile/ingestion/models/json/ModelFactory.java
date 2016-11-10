package com.microsoft.azure.mobile.ingestion.models.json;

import com.microsoft.azure.mobile.ingestion.models.Model;

import java.util.List;

public interface ModelFactory<M extends Model> {

    M create();

    List<M> createList(int capacity);
}
