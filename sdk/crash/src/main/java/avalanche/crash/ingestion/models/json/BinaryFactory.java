package avalanche.crash.ingestion.models.json;

import java.util.ArrayList;
import java.util.List;

import avalanche.core.ingestion.models.json.ModelFactory;
import avalanche.crash.ingestion.models.Binary;

public class BinaryFactory implements ModelFactory<Binary> {

    private static final BinaryFactory sInstance = new BinaryFactory();

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
