package com.microsoft.appcenter.persistence;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.microsoft.appcenter.ingestion.models.json.MockLog;
import com.microsoft.appcenter.utils.storage.StorageHelper;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@SuppressWarnings("unused")
@SmallTest
@RunWith(AndroidJUnit4.class)
public class PersistenceAndroidTest {

    /**
     * Context instance.
     */
    @SuppressLint("StaticFieldLeak")
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
    public void missingLogSerializer() throws Persistence.PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext, 1, DatabasePersistence.SCHEMA, Persistence.DEFAULT_CAPACITY);

        //noinspection TryFinallyCanBeTryWithResources (try with resources statement is API >= 19)
        try {
            /* Generate a log and persist. */
            persistence.putLog("exception", new MockLog());
        } finally {
            /* Close. */
            //noinspection ThrowFromFinallyBlock
            persistence.close();
        }
    }
}
