package avalanche.crash.ingestion.models.json;

import java.util.ArrayList;
import java.util.List;

import avalanche.base.ingestion.models.json.DefinitionFactory;
import avalanche.crash.ingestion.models.Binary;

public class BinaryFactory implements DefinitionFactory<Binary> {

    private static BinaryFactory sInstance = new BinaryFactory();

    private BinaryFactory() {
    }

    public static BinaryFactory getInstance() {
        return sInstance;
    }

    @Override
    public Binary create() {
        return new Binary();
    }

    @Override
    public List<Binary> createList(int capacity) {
        return new ArrayList<>(capacity);
    }
}
