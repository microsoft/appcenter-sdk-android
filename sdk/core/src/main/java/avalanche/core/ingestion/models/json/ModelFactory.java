package avalanche.core.ingestion.models.json;

import java.util.List;

import avalanche.core.ingestion.models.Model;

public interface ModelFactory<M extends Model> {

    M create();

    List<M> createList(int capacity);
}
