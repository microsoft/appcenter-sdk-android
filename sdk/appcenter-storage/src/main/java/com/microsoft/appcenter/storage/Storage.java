package com.microsoft.appcenter.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.gson.Gson;
import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.storage.cosmosdb.*;
import com.microsoft.appcenter.storage.models.ConflictResolutionPolicy;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.Documents;
import com.microsoft.appcenter.storage.models.TokenResult;
import com.microsoft.appcenter.storage.models.TokensResponse;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;

import org.json.JSONException;
import org.json.JSONStringer;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import static com.microsoft.appcenter.Constants.DEFAULT_API_URL;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_GET;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;
import static com.microsoft.appcenter.http.HttpUtils.createHttpClient;
import static com.microsoft.appcenter.storage.Constants.*;

/**
 * Storage service.
 */
public class Storage extends AbstractAppCenterService {

    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Storage sInstance;

    /**
     * Application context.
     */
    private Context mContext;

    /**
     * Application secret.
     */
    private String mAppSecret;

    /**
     * Current API base URL.
     */
    private String mApiUrl = DEFAULT_API_URL;

    private final Gson gson;

    private Storage() {
        gson = new Gson();
    }

    /**
     * Get shared instance.
     *
     * @return shared instance.
     */
    @SuppressWarnings("WeakerAccess")
    public static synchronized Storage getInstance() {
        if (sInstance == null) {
            sInstance = new Storage();
        }
        return sInstance;
    }

    @VisibleForTesting
    static synchronized void unsetInstance() {
        sInstance = null;
    }

    /**
     * Change the base URL used to make API calls.
     *
     * @param apiUrl API base URL.
     */
    @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
    public static void setApiUrl(String apiUrl) {
        getInstance().setInstanceApiUrl(apiUrl);
    }

    /**
     * Implements {@link #setApiUrl(String)}}.
     */
    private synchronized void setInstanceApiUrl(String apiUrl) {
        mApiUrl = apiUrl;
    }

    /**
     * Check whether Storage service is enabled or not.
     *
     * @return future with result being <code>true</code> if enabled, <code>false</code> otherwise.
     * @see AppCenterFuture
     */
    @SuppressWarnings({"unused", "WeakerAccess"}) // TODO Remove warning suppress after release.
    public static AppCenterFuture<Boolean> isEnabled() {
        return getInstance().isInstanceEnabledAsync();
    }

    /**
     * Enable or disable Storage service.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     * @return future with null result to monitor when the operation completes.
     */
    @SuppressWarnings({"unused", "WeakerAccess"}) // TODO Remove warning suppress after release.
    public static AppCenterFuture<Void> setEnabled(boolean enabled) {
        return getInstance().setInstanceEnabledAsync(enabled);
    }

    @Override
    public synchronized void onStarted(@NonNull Context context, @NonNull Channel channel, String appSecret, String transmissionTargetToken, boolean startedFromApp) {
        mContext = context;
        mAppSecret = appSecret;
        super.onStarted(context, channel, appSecret, transmissionTargetToken, startedFromApp);
    }

    /**
     * React to enable state change.
     *
     * @param enabled current state.
     */
    @Override
    protected synchronized void applyEnabledState(boolean enabled) {
        if (enabled) {
        } else {
        }
    }

    @Override
    protected String getGroupName() {
        return STORAGE_GROUP;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected String getLoggerTag() {
        return LOG_TAG;
    }

    class Test{
        String test;

    }

    // Read a document
    // The document type (T) must be JSON deserializable
    public static <T> AppCenterFuture<Document<T>> read(){//String partition, String documentId){

        AppCenterLog.debug(LOG_TAG, "Read started");
        return getInstance().instanceRead("readonly", "123");//partition, documentId);
    }

    // Read a document
    // The document type (T) must be JSON deserializable
    private synchronized  <T> AppCenterFuture<Document<T>> instanceRead(String partition, String documentId){
        DefaultAppCenterFuture<Document<T>> result = new DefaultAppCenterFuture<>();
        return result;
    }

    // List (need optional signature to configure page size)
    // The document type (T) must be JSON deserializable
    public <T> AppCenterFuture<Documents<T>> list(String partition, Class<T> documentType) {
        return null;
    }

    // Create a document
    // The document instance (T) must be JSON serializable
    public <T> AppCenterFuture<Document<T>> create(String partition, String documentId, T document) {
        return null;
    }

    public <T> AppCenterFuture<Document<T>> create(String partition, String documentId, T document, ConflictResolutionPolicy resolutionPolicy) {
        //docDbClient.Create(documentId)
        return null;
    }

    // Replace a document
    // The document instance (T) must be JSON serializable
    public <T> AppCenterFuture<Document<T>> replace(String partition, String documentId, T document){
        return null;
    }

    public <T> AppCenterFuture<Document<T>> replace(String partition, String documentId, T document, ConflictResolutionPolicy resolutionPolicy) {
        return null;
    }

    // Delete a document
    public AppCenterFuture<Document<Void>> delete(String partition, String documentId) {
        return null;
    }

    private synchronized <T> void getDbToken(final String partition, final DefaultAppCenterFuture<TokenResult> future) {
        final TokenResult token = TokenManager.getInstance().getToken(partition);
        if (token != null){
            future.complete(token);
        } else {
            AppCenterLog.debug(LOG_TAG, "Get token from the appcenter service...");
            String url = mApiUrl;
            HttpClient httpClient = createHttpClient(mContext);
            url += String.format(GET_TOKEN_PATH_FORMAT, mAppSecret);
            httpClient.callAsync(
                    url,
                    METHOD_POST,
                    new HashMap<String, String>() { { put(APP_SECRET_HEADER, mAppSecret); } },
                    new HttpClient.CallTemplate() {

                        @Override
                        public String buildRequestBody() {
                            return buildAppCenterGetDbTokenBodyPayload(partition);
                        }

                        @Override
                        public void onBeforeCalling(URL url, Map<String, String> headers) { }
                    }, new ServiceCallback() {

                        @Override
                        public void onCallSucceeded(final String payload, Map<String, String> headers) {
                            TokensResponse tokensResponse = gson.fromJson(payload, TokensResponse.class);

                            // TODO: provide a delegate to call other methods with `future` as the parameter
                            TokenResult token = tokensResponse.tokens().get(0);
                            TokenManager.getInstance().setToken(token);
                            future.complete(token);
                            // TODO: do we need to call `complete` here?
                        }

                        @Override
                        public void onCallFailed(Exception e) {
                            handleApiCallFailure(e);

                            // TODO: construct an error object
                            future.complete(null);
                        }
                    });
        }
    }

    private static String buildAppCenterGetDbTokenBodyPayload(final String partition) {
        String apiBody;
        JSONStringer writer = new JSONStringer();
        try {
            // TODO: use https://github.com/google/gson for serialization
            List<String> partitions = new ArrayList<String>() {{add(partition);}};
            writer.object();
            writer.key("partitions").array();
            for (String p : partitions) {
                writer.value(p);
            }
            writer.endArray();
            writer.endObject();
        } catch (JSONException e) {
            AppCenterLog.error(LOG_TAG, "Failed to build API body", e);
        }

        apiBody = writer.toString();
        return apiBody;
    }

    private synchronized <T> void readDbDocument(
            final TokenResult tokenResult,
            final String documentId,
            final DefaultAppCenterFuture<Document<T>> future) {
        AppCenterLog.debug(LOG_TAG, "Read a document from the DB...");

        // https://docs.microsoft.com/en-us/rest/api/cosmos-db/get-a-document
        final String documentResourceId = getDocumentResourceId(tokenResult.dbName(), tokenResult.dbCollectionName(), documentId);
        final String documentUrl = getDocumentUrl(tokenResult.dbAccount(), documentResourceId);

        ServiceCall documentResponse =
            createHttpClient(mContext).callAsync(
                documentUrl,
                METHOD_GET,
                    generateHeaders(documentResourceId, tokenResult.partition(), tokenResult.token()),
                new HttpClient.CallTemplate() {

                    @Override
                    public String buildRequestBody() { return ""; }

                    @Override
                    public void onBeforeCalling(URL url, Map<String, String> headers) { }
                }, new ServiceCallback() {

                    @Override
                    public void onCallSucceeded(final String payload, Map<String, String> headers) {
                        AppCenterLog.verbose(LOG_TAG, "Received a call back with payload " + payload);

                        future.complete(new Document<T>(payload, tokenResult.partition(), documentId));
                    }

                    @Override
                    public void onCallFailed(Exception e) {
                        handleApiCallFailure(e);

                        // TODO: construct an error object
                        future.complete(null);
                    }
                });
    }

    private static Map<String, String> generateHeaders(String documentResourceId, final String partition, String dbToken) {
        Map<String, String> headers = new HashMap<String, String>() {{
            put("x-ms-documentdb-partitionkey", partition);
            put("x-ms-version", "2017-02-22");
            put("x-ms-date", Utils.nowAsRFC1123());
            put("Content-Type", "application/json");
        }};
        final AuthorizationTokenProvider authTokenProvider = new BaseAuthorizationTokenProvider(dbToken);
        try {
            headers.put(
                    "Authorization",
                    authTokenProvider.generateKeyAuthorizationSignature(
                            METHOD_GET,
                            documentResourceId,
                            ResourceType.Document,
                            headers));
        } catch (Exception e) {
            AppCenterLog.error(LOG_TAG, "Could not sign Cosmos DB payload", e);
        }
        return headers;
    }

    private String getDocumentUrl(String dbAccount, String documentResourseId) {
        return String.format(Constants.DOCUMENT_DB_ENDPOINT, dbAccount) + "/" +
                documentResourseId;
    }

    private static String getDocumentResourceId(String databaseName, String collectionName, String documentId) {
        return String.format(Constants.DOCUMENT_DB_DATABASE_URL_SUFFIX, databaseName) + "/" +
        String.format(Constants.DOCUMENT_DB_COLLECTION_URL_SUFFIX, collectionName) + "/" +
        String.format(Constants.DOCUMENT_DB_DOCUMENT_URL_SUFFIX, documentId);
    }

    /**
     * Handle API call failure.
     */
    private synchronized void handleApiCallFailure(Exception e) {
        AppCenterLog.error(LOG_TAG, "Failed to call App Center APIs", e);
        if (!HttpUtils.isRecoverableError(e)) {
            if (e instanceof HttpException) {
                HttpException httpException = (HttpException) e;
                AppCenterLog.error(LOG_TAG, "Exception", httpException);
            }
        }
    }
}
