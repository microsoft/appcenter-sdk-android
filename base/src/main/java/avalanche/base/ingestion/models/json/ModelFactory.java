package avalanche.base.ingestion.models.json;

import java.util.List;

import avalanche.base.ingestion.models.Model;

public interface ModelFactory<M extends Model> {

    M create();

    List<M> createList(int capacity);
}
