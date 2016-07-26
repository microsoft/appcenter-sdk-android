package avalanche.core.persistence;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import avalanche.core.ingestion.models.json.MockLog;
import avalanche.core.utils.StorageHelper;

@SuppressWarnings("unused")
@SmallTest
@RunWith(AndroidJUnit4.class)
public class AvalanchePersistenceTest {

    /**
     * Log tag.
     */
    private static final String TAG = "DatabasePersistenceTest";

    /**
     * Context instance.
     */
    private static Context sContext;

    @BeforeClass
    public static void setUpClass() {
        sContext = InstrumentationRegistry.getTargetContext();
        StorageHelper.initialize(sContext);

        /* Clean up database. */
        sContext.deleteDatabase("test-persistence");
    }

    @AfterClass
    public static void tearDownClass() {
        /* Clean up database. */
        sContext.deleteDatabase("test-persistence");
    }

    @Test(expected = IllegalStateException.class)
    public void missingLogSerializer() throws AvalanchePersistence.PersistenceException, IOException {
        android.util.Log.i(TAG, "Testing Database Persistence exception");

        /* Initialize database persistence. */
        AvalancheDatabasePersistence persistence = new AvalancheDatabasePersistence("test-persistence", "exception", 1);

        //noinspection TryFinallyCanBeTryWithResources (try with resources statement is API >= 19)
        try {
            /* Generate a log and persist. */
            persistence.putLog("exception", new MockLog());
        } finally {
            /* Close. */
            persistence.close();
        }
    }
}
