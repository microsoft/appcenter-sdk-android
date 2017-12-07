package com.microsoft.appcenter.push;

import com.google.firebase.iid.FirebaseInstanceId;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(FirebaseInstanceId.class)
public class FirebaseUtilsTest {

    @Before
    public void setUp() {
        mockStatic(FirebaseInstanceId.class);
    }

    @Test
    public void coverInit() {
        assertNotNull(new FirebaseUtils());
    }

    @Test
    public void firebaseUnavailable() throws Exception {
        IllegalStateException exception = new IllegalStateException("Not init.");
        when(FirebaseInstanceId.getInstance()).thenThrow(exception);
        assertFalse(FirebaseUtils.isFirebaseAvailable());
    }

    @Test
    public void firebaseIsAvailable() throws Exception {
        FirebaseInstanceId instanceId = mock(FirebaseInstanceId.class);
        when(FirebaseInstanceId.getInstance()).thenReturn(instanceId);
        when(instanceId.getToken()).thenReturn("token");
        assertTrue(FirebaseUtils.isFirebaseAvailable());
    }

    @Test
    public void firebaseNullInstance() throws Exception {
        assertFalse(FirebaseUtils.isFirebaseAvailable());
    }
}
