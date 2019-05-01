/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.crypto;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.KeyGenerator;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * This class is the only way to cover the default KeyGenerator implementation,
 * it cannot use PowerMockRule or real java implementation.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(KeyGenerator.class)
public class CryptoDefaultKeyGeneratorMockTest {

    @Before
    public void setUp() throws Exception {
        mockStatic(KeyGenerator.class);
        when(KeyGenerator.getInstance(anyString(), anyString())).thenReturn(mock(KeyGenerator.class));
    }

    @Test
    public void checkParametersPassingForKeyGenerator() throws Exception {
        mockStatic(KeyGenerator.class);
        KeyGenerator mockKeyGenerator = mock(KeyGenerator.class);
        when(KeyGenerator.getInstance(anyString(), anyString())).thenReturn(mockKeyGenerator);
        CryptoUtils.IKeyGenerator keyGenerator = CryptoUtils.DEFAULT_CRYPTO_FACTORY.getKeyGenerator("AES", "MockProvider");
        AlgorithmParameterSpec parameters = mock(AlgorithmParameterSpec.class);
        keyGenerator.init(parameters);
        verify(mockKeyGenerator).init(parameters);
        keyGenerator.generateKey();
        verify(mockKeyGenerator).generateKey();
    }
}

