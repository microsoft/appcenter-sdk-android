package com.microsoft.appcenter;

import org.junit.Test;

import static com.microsoft.appcenter.AppCenter.PAIR_DELIMITER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AppCenterUserIdTest extends AbstractAppCenterTest {

    private static String longUserId() {
        StringBuilder userId = new StringBuilder();
        for (int i = 0; i <= Constants.USER_ID_MAX_LENGTH; i++) {
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
    public void setLongUserIdWithNoSecret() {
        AppCenter.configure(mApplication);
        String userId = longUserId();
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
        String userId = longUserId();
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
}
