/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data;

import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.AppCenterHandler;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.data.client.CosmosDb;
import com.microsoft.appcenter.data.client.TokenExchange;
import com.microsoft.appcenter.data.models.RemoteOperationListener;
import com.microsoft.appcenter.http.AbstractAppCallTemplate;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpClientRetryer;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.models.json.JSONUtils;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.PrefStorageConstants;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.context.AuthTokenContext;
import com.microsoft.appcenter.utils.crypto.CryptoUtils;
import com.microsoft.appcenter.utils.storage.FileManager;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.util.HashMap;

import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@PrepareForTest({
        Data.class,
        SystemClock.class,
        SharedPreferencesManager.class,
        FileManager.class,
        AppCenterLog.class,
        AppCenter.class,
        CryptoUtils.class,
        HandlerUtils.class,
        HttpUtils.class,
        JSONUtils.class,
        NetworkStateHelper.class,
        LocalDocumentStorage.class,
        Utils.class,
        AuthTokenContext.class,
        CosmosDb.class,
        TokenExchange.class,
        TokenManager.class
})
abstract public class AbstractDataTest {

    static final String DATABASE_NAME = "mbaas";

    static final String COLLECTION_NAME = "appcenter";

    static final String ACCOUNT_ID = "bd45f90e-6eb1-4c47-817e-e59b82b5c03d";

    static final String RESOLVED_USER_PARTITION = DefaultPartitions.USER_DOCUMENTS + "-" + ACCOUNT_ID;

    static final String DOCUMENT_ID = "document-id";

    static final String TEST_FIELD_VALUE = "Test Value";

    static final String ETAG = "06000da6-0000-0000-0000-5c7093c30000";

    static final long CURRENT_TIMESTAMP = System.currentTimeMillis();

    static final long FUTURE_TIMESTAMP = CURRENT_TIMESTAMP + 2 * 24 * 60 * 60 * 1000L;

    static final long PAST_TIMESTAMP = CURRENT_TIMESTAMP - 2 * 24 * 60 * 60 * 1000L;

    final static String COSMOS_DB_DOCUMENT_RESPONSE_PAYLOAD = String.format("{\n" +
            "    \"document\": {\n" +
            "        \"test\": \"%s\"\n" +
            "    },\n" +
            "    \"id\": \"%s\",\n" +
            "    \"PartitionKey\": \"%s\",\n" +
            "    \"_rid\": \"mFBtAPPa528HAAAAAAAAAA==\",\n" +
            "    \"_self\": \"dbs/mFBtAA==/colls/mFBtAPPa528=/docs/mFBtAPPa528HAAAAAAAAAA==/\",\n" +
            "    \"_etag\": \"%s\",\n" +
            "    \"_attachments\": \"attachments/\",\n" +
            "    \"_ts\": 1550881731\n" +
            "}", TEST_FIELD_VALUE, DOCUMENT_ID, RESOLVED_USER_PARTITION, ETAG);

    static final String TOKEN_RESULT = String.format("{\n" +
            "            \"partition\": \"%s\",\n" +
            "            \"dbAccount\": \"lemmings-01-8f37d78902\",\n" +
            "            \"dbName\": \"db\",\n" +
            "            \"dbCollectionName\": \"collection\",\n" +
            "            \"token\": \"%s\",\n" +
            "            \"status\": \"Succeed\",\n" +
            "            \"accountId\": \"%s\"\n" +
            "}", RESOLVED_USER_PARTITION, "token", ACCOUNT_ID);

    static final String USER_TABLE_NAME = Utils.getUserTableName(AbstractDataTest.ACCOUNT_ID);

    static final String DATA_ENABLED_KEY = PrefStorageConstants.KEY_ENABLED + "_" + Data.getInstance().getServiceName();

    static final String TOKEN = "ha-ha-ha-ha";

    private static final String TOKEN_EXCHANGE_RESPONSE_FORMAT = "{\n" +
            "    \"tokens\": [\n" +
            "        {\n" +
            "            \"partition\": \"%s\",\n" +
            "            \"dbAccount\": \"lemmings-01-8f37d78902\",\n" +
            "            \"dbName\": \"%s\",\n" +
            "            \"dbCollectionName\": \"%s\",\n" +
            "            \"token\": \"%s\",\n" +
            "            \"status\": \"Succeed\"\n" +
            "            %s" +
            "        }\n" +
            "    ]\n" +
            "}";

    final static String TOKEN_EXCHANGE_READONLY_PAYLOAD =
            String.format(TOKEN_EXCHANGE_RESPONSE_FORMAT, DefaultPartitions.APP_DOCUMENTS, DATABASE_NAME, COLLECTION_NAME, TOKEN, "");

    final static String TOKEN_EXCHANGE_USER_PAYLOAD =
            String.format(TOKEN_EXCHANGE_RESPONSE_FORMAT, RESOLVED_USER_PARTITION, DATABASE_NAME, COLLECTION_NAME, TOKEN, String.format(",\"accountId\": \"%s\"\n", ACCOUNT_ID));

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    protected HttpClientRetryer mHttpClient;

    @Mock
    protected RemoteOperationListener mRemoteOperationListener;

    @Mock
    AppCenterHandler mAppCenterHandler;

    Channel mChannel;

    Data mData;

    @Mock
    private AppCenterFuture<Boolean> mCoreEnabledFuture;

    @Mock
    NetworkStateHelper mNetworkStateHelper;

    @Mock
    LocalDocumentStorage mLocalDocumentStorage;

    @Mock
    protected AuthTokenContext mAuthTokenContext;

    @Before
    public void setUp() throws Exception {
        Data.unsetInstance();
        mockStatic(SystemClock.class);
        mockStatic(AppCenterLog.class);
        mockStatic(AppCenter.class);
        when(AppCenter.getLogLevel()).thenReturn(Log.WARN);
        when(AppCenter.isConfigured()).thenReturn(true);
        when(AppCenter.getInstance()).thenReturn(mock(AppCenter.class));
        when(AppCenter.isEnabled()).thenReturn(mCoreEnabledFuture);
        when(mCoreEnabledFuture.get()).thenReturn(true);
        Answer<Void> runNow = new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        };
        doAnswer(runNow).when(mAppCenterHandler).post(any(Runnable.class), any(Runnable.class));
        mockStatic(HandlerUtils.class);
        doAnswer(runNow).when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));

        /* First call to com.microsoft.appcenter.AppCenter.isEnabled shall return true, initial state. */
        mockStatic(SharedPreferencesManager.class);
        when(SharedPreferencesManager.getBoolean(anyString(), eq(true))).thenReturn(true);

        /* Then simulate further changes to state. */
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) {

                /* Whenever the new state is persisted, make further calls return the new state. */
                String key = (String) invocation.getArguments()[0];
                boolean enabled = (Boolean) invocation.getArguments()[1];
                when(SharedPreferencesManager.getBoolean(eq(key), eq(true))).thenReturn(enabled);
                return null;
            }
        }).when(SharedPreferencesManager.class);
        SharedPreferencesManager.putBoolean(anyString(), anyBoolean());

        /* Mock file storage. */
        mockStatic(FileManager.class);
        mHttpClient = mock(HttpClientRetryer.class);
        whenNew(HttpClientRetryer.class).withAnyArguments().thenReturn(mHttpClient);
        when(SharedPreferencesManager.getBoolean(DATA_ENABLED_KEY, true)).thenReturn(true);
        mockStatic(NetworkStateHelper.class);
        when(NetworkStateHelper.getSharedInstance(any(Context.class))).thenReturn(mNetworkStateHelper);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        whenNew(LocalDocumentStorage.class).withAnyArguments().thenReturn(mLocalDocumentStorage);
        mData = Data.getInstance();
        mChannel = start(mData);
        Data.setTokenExchangeUrl("default");

        /* Wait until the module has finished enabling so pending operations have already been processed. */
        Data.setEnabled(true).get();

        /* Verify this here so it is ignored in "verify no more interactions." */
        verify(mLocalDocumentStorage).getPendingOperations(anyString());

        /* Mock utils. */
        mockStatic(CryptoUtils.class);
        mockStatic(JSONUtils.class);

        /* Mock CryptoUtils. */
        CryptoUtils cryptoUtils = mock(CryptoUtils.class);
        when(cryptoUtils.encrypt(anyString())).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                return (String) args[0];
            }
        });
        when(cryptoUtils.decrypt(anyString(), anyBoolean())).thenAnswer(new Answer<CryptoUtils.DecryptedData>() {

            @Override
            public CryptoUtils.DecryptedData answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                return new CryptoUtils.DecryptedData((String) args[0], "encrypted");
            }
        });
        when(CryptoUtils.getInstance(any(Context.class))).thenReturn(cryptoUtils);
    }

    void setUpAuthContext() {

        /* Mock auth context. */
        mockStatic(AuthTokenContext.class);
        when(AuthTokenContext.getInstance()).thenReturn(mAuthTokenContext);
        when(mAuthTokenContext.getAccountId()).thenReturn(AbstractDataTest.ACCOUNT_ID);
    }

    @NonNull
    Channel start(Data data) {
        Channel channel = mock(Channel.class);
        data.onStarting(mAppCenterHandler);
        data.onStarted(mock(Context.class), channel, "", null, true);
        return channel;
    }

    String verifyTokenExchangeToCosmosDbFlow(
            String documentId,
            String tokenExchangePayload,
            String cosmosCallApiMethod,
            String cosmosSuccessPayload,
            Exception cosmosFailureException) throws JSONException {
        verifyTokenExchangeFlow(tokenExchangePayload, null);
        return verifyCosmosDbFlow(documentId, cosmosCallApiMethod, cosmosSuccessPayload, cosmosFailureException);
    }

    String verifyCosmosDbFlow(String documentId, String cosmosCallApiMethod, String cosmosSuccessPayload, Exception cosmosFailureException) throws JSONException {
        ArgumentCaptor<HttpClient.CallTemplate> cosmosDbCallTemplateCallbackArgumentCaptor =
                ArgumentCaptor.forClass(HttpClient.CallTemplate.class);
        ArgumentCaptor<ServiceCallback> cosmosDbServiceCallbackArgumentCaptor =
                ArgumentCaptor.forClass(ServiceCallback.class);
        verify(mHttpClient).callAsync(
                endsWith(CosmosDb.getDocumentBaseUrl(DATABASE_NAME, COLLECTION_NAME, documentId)),
                eq(cosmosCallApiMethod),
                anyMapOf(String.class, String.class),
                cosmosDbCallTemplateCallbackArgumentCaptor.capture(),
                cosmosDbServiceCallbackArgumentCaptor.capture());
        ServiceCallback cosmosDbServiceCallback = cosmosDbServiceCallbackArgumentCaptor.getValue();
        HttpClient.CallTemplate callTemplate = cosmosDbCallTemplateCallbackArgumentCaptor.getValue();
        String requestBody = callTemplate.buildRequestBody();
        callTemplate.onBeforeCalling(null, new HashMap<String, String>());
        assertNotNull(cosmosDbServiceCallback);
        if (cosmosSuccessPayload != null) {
            cosmosDbServiceCallback.onCallSucceeded(cosmosSuccessPayload, new HashMap<String, String>());
        }
        if (cosmosFailureException != null) {
            cosmosDbServiceCallback.onCallFailed(cosmosFailureException);
        }
        return requestBody;
    }

    void verifyTokenExchangeFlow(
            String tokenExchangeSuccessResponsePayload,
            Exception tokenExchangeFailureResponse) throws JSONException {
        ArgumentCaptor<AbstractAppCallTemplate> tokenExchangeTemplateCallbackArgumentCaptor =
                ArgumentCaptor.forClass(AbstractAppCallTemplate.class);
        ArgumentCaptor<TokenExchange.TokenExchangeServiceCallback> tokenExchangeServiceCallbackArgumentCaptor =
                ArgumentCaptor.forClass(TokenExchange.TokenExchangeServiceCallback.class);
        verify(mHttpClient).callAsync(
                endsWith(TokenExchange.GET_TOKEN_PATH_FORMAT),
                eq(METHOD_POST),
                anyMapOf(String.class, String.class),
                tokenExchangeTemplateCallbackArgumentCaptor.capture(),
                tokenExchangeServiceCallbackArgumentCaptor.capture());
        TokenExchange.TokenExchangeServiceCallback tokenExchangeServiceCallback = tokenExchangeServiceCallbackArgumentCaptor.getValue();
        assertNotNull(tokenExchangeTemplateCallbackArgumentCaptor);
        assertNotNull(tokenExchangeServiceCallback);

        tokenExchangeTemplateCallbackArgumentCaptor.getValue().onBeforeCalling(null, new HashMap<String, String>());
        String body = tokenExchangeTemplateCallbackArgumentCaptor.getValue().buildRequestBody();
        assertFalse(body.contains(ACCOUNT_ID));
        if (tokenExchangeSuccessResponsePayload != null) {
            tokenExchangeServiceCallback.onCallSucceeded(tokenExchangeSuccessResponsePayload, new HashMap<String, String>());
        }
        if (tokenExchangeFailureResponse != null) {
            tokenExchangeServiceCallback.onCallFailed(tokenExchangeFailureResponse);
        }
    }
}
