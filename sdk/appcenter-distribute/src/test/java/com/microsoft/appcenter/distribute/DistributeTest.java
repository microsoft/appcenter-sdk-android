package com.microsoft.appcenter.distribute;

import com.microsoft.appcenter.distribute.ingestion.models.DistributionStartSessionLog;
import com.microsoft.appcenter.distribute.ingestion.models.json.DistributionStartSessionLogFactory;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;

import org.junit.Test;

import java.util.Map;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DistributeTest extends AbstractDistributeTest {

    @Test
    public void singleton() {
        assertSame(Distribute.getInstance(), Distribute.getInstance());
    }

    @Test
    public void checkFactories() {
        Map<String, LogFactory> factories = Distribute.getInstance().getLogFactories();
        assertNotNull(factories);
        assertTrue(factories.remove(DistributionStartSessionLog.TYPE) instanceof DistributionStartSessionLogFactory);
        assertTrue(factories.isEmpty());
    }
}
