package avalanche.core.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UUID.class, UUIDUtils.class, AvalancheLog.class})
public class UUIDUtilsTest {

    @Test
    public void secureRandom() {
        mockStatic(AvalancheLog.class);
        UUID uuid = UUIDUtils.randomUUID();
        System.out.println(uuid);
        assertEquals(4, uuid.version());
        assertEquals(2, uuid.variant());
        verifyStatic(never());
        AvalancheLog.error(anyString(), any(Exception.class));
    }

    @Test
    public void securityException() {
        mockStatic(UUID.class);
        mockStatic(AvalancheLog.class);
        SecurityException exception = new SecurityException("mock");
        when(UUID.randomUUID()).thenThrow(exception);
        for (int i = 0; i < 2; i++) {
            UUID uuid = UUIDUtils.randomUUID();
            System.out.println(uuid);
            assertEquals(4, uuid.version());
            assertEquals(2, uuid.variant());
        }
        verifyStatic();
        AvalancheLog.error(anyString(), eq(exception));
    }
}
