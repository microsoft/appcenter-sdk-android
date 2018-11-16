package com.microsoft.appcenter;

import org.junit.Test;

import static com.microsoft.appcenter.AppCenter.PAIR_DELIMITER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AppCenterUserIdTest extends AbstractAppCenterTest {

    private static String longUserId() {
        StringBuilder userId = new StringBuilder();
        for (int i = 0; i <= Constants.USER_ID_APP_CENTER_MAX_LENGTH; i++) {
            userId.append("x");
        }
        return userId.toString();
    }

    @Test
    public void setUserIdBeforeStart() {
        AppCenter.setUserId("test");
        assertNull(AppCenter.getInstance().getUserId());
    }

    @Test
    public void setUserIdAfterStart() {
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        AppCenter.setUserId("test");
        assertEquals("test", AppCenter.getInstance().getUserId());

        /* Unset. */
        AppCenter.setUserId(null);
        assertNull(AppCenter.getInstance().getUserId());

        /* Another value. */
        AppCenter.setUserId("test2");
        assertEquals("test2", AppCenter.getInstance().getUserId());
    }

    @Test
    public void setUserIdWithNoSecret() {
        AppCenter.configure(mApplication);
        AppCenter.setUserId("test");
        assertNull(AppCenter.getInstance().getUserId());
    }

    @Test
    public void setUserIdFromLibrary() {
        AppCenter.startFromLibrary(mApplication, DummyService.class);
        String userId = longUserId();
        AppCenter.setUserId(userId);
        assertNull(AppCenter.getInstance().getUserId());
    }

    @Test
    public void setUserIdAfterStartedFromLibraryThenApplication() {
        AppCenter.startFromLibrary(mApplication, DummyService.class);
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        String userId = "test";
        AppCenter.setUserId(userId);
        assertEquals(userId, AppCenter.getInstance().getUserId());
    }

    @Test
    public void setLongUserIdWithAppSecret() {
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        String userId = longUserId();
        AppCenter.setUserId(userId);
        assertNull(AppCenter.getInstance().getUserId());
    }

    @Test
    public void setLongUserIdWithTargetTokenSecret() {
        AppCenter.start(mApplication, DUMMY_TARGET_TOKEN_STRING, DummyService.class);
        String userId = "c:" + longUserId();
        AppCenter.setUserId(userId);
        assertEquals(userId, AppCenter.getInstance().getUserId());
    }

    @Test
    public void setLongUserIdWithBothSecrets() {
        String secret = DUMMY_TARGET_TOKEN_STRING + PAIR_DELIMITER +
                DUMMY_APP_SECRET;
        AppCenter.start(mApplication, secret, DummyService.class);
        String userId = longUserId();
        AppCenter.setUserId(userId);
        assertNull(AppCenter.getInstance().getUserId());
    }

    @Test
    public void setInvalidOneCollectorUserIdWithAppSecret() {
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        AppCenter.setUserId("x:test");
        assertEquals("x:test", AppCenter.getInstance().getUserId());
    }

    @Test
    public void setInvalidOneCollectorUserIdWithTargetToken() {
        AppCenter.start(mApplication, DUMMY_TARGET_TOKEN_STRING, DummyService.class);
        AppCenter.setUserId("x:test");
        assertNull(AppCenter.getInstance().getUserId());
    }

    @Test
    public void setInvalidOneCollectorUserIdWithBothSecrets() {
        String secret = DUMMY_TARGET_TOKEN_STRING + PAIR_DELIMITER +
                DUMMY_APP_SECRET;
        AppCenter.start(mApplication, secret, DummyService.class);
        AppCenter.setUserId("x:test");
        assertNull(AppCenter.getInstance().getUserId());
    }
}
