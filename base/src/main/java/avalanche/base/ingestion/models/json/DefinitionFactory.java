package avalanche.base.ingestion.models.json;

import java.util.List;

import avalanche.base.ingestion.models.Definition;

public interface DefinitionFactory<D extends Definition> {

    D create();

    List<D> createList(int capacity);
}
