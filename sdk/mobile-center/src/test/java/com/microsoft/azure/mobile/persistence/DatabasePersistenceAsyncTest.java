package com.microsoft.azure.mobile.persistence;

import android.os.Handler;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unused")
@PrepareForTest(DatabasePersistenceAsync.class)
public class DatabasePersistenceAsyncTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Mock
    private Handler mHandler;

    @Mock
    private Persistence mPersistence;

    private DatabasePersistenceAsync mDatabase;

    @Before
    public void setUp() throws Exception {
        whenNew(Handler.class).withAnyArguments().thenReturn(mHandler);
        doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(mHandler).post(any(Runnable.class));
        mDatabase = new DatabasePersistenceAsync(mPersistence);
    }

    @Test
    public void deleteLogsByGroup() {
        mDatabase.deleteLogs("test");
        verify(mPersistence).deleteLogs("test");
    }

    @Test
    public void deleteLogsByGroupWithCallback() {
        DatabasePersistenceAsync.DatabasePersistenceAsyncCallback callback = mock(DatabasePersistenceAsync.DatabasePersistenceAsyncCallback.class);
        mDatabase.deleteLogs("test", callback);
        verify(mPersistence).deleteLogs("test");
        verify(callback).onSuccess(null);
    }

    @Test
    public void onFailure() {
        /* Dummy test for callback that is not possible to be covered by unit test. */
        DatabasePersistenceAsync.AbstractDatabasePersistenceAsyncCallback callback = new DatabasePersistenceAsync.AbstractDatabasePersistenceAsyncCallback() {
            @Override
            public void onSuccess(Object result) {
            }
        };
        callback.onFailure(null);
    }
}
