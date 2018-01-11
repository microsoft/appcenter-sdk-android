package com.microsoft.appcenter.codepush;

import com.microsoft.appcenter.codepush.enums.CodePushCheckFrequency;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CodePushTest {

    @Test
    public void enumsTest() throws Exception {
        CodePushCheckFrequency codePushCheckFrequency = CodePushCheckFrequency.MANUAL;
        int checkFrequencyValue = codePushCheckFrequency.getValue();
        assertEquals(checkFrequencyValue, 2);
    }
}