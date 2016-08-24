package com.microsoft.sonoma.core.ingestion.models.json;

import com.microsoft.sonoma.core.ingestion.models.Model;

import java.util.List;

public interface ModelFactory<M extends Model> {

    M create();

    List<M> createList(int capacity);
}
