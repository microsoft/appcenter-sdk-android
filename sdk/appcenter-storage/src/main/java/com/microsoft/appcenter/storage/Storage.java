package com.microsoft.appcenter.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.ServiceCallback;
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
     * Db Token.
     */
    private String mDbToken;

    /**
     * Current API base URL.
     */
    private String mApiUrl = DEFAULT_API_URL;

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

    // Read a document
    // The document type (T) must be JSON deserializable
    public static <T> AppCenterFuture<Document<T>> read(String partition, String documentId, Class<T> documentType){
        return getInstance().instanceRead(partition, documentId, documentType);
    }

    // Read a document
    // The document type (T) must be JSON deserializable
    private synchronized  <T> AppCenterFuture<Document<T>> instanceRead(String partition, String documentId, Class<T> documentType){
        return null;
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

    synchronized void getDbToken(final String partition) {
        AppCenterLog.debug(LOG_TAG, "Get token from the appcenter service...");
        HttpClient httpClient = createHttpClient(mContext);
        String url = mApiUrl;
        url += String.format(GET_TOKEN_PATH_FORMAT, mAppSecret);

        Object tokenResponse =
            httpClient.callAsync(
                url,
                METHOD_POST,
                new HashMap<String, String>() { { put(APP_SECRET_HEADER, mAppSecret); } },
                new HttpClient.CallTemplate() {

                    @Override
                    public String buildRequestBody() {
                        String apiBody;
                        JSONStringer writer = new JSONStringer();
                        try {
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
                        AppCenterLog.verbose(LOG_TAG, "API body is " + apiBody);
                        return apiBody;
                    }

                    @Override
                    public void onBeforeCalling(URL url, Map<String, String> headers) {
                    }
                }, new ServiceCallback() {

                    @Override
                    public void onCallSucceeded(final String payload, Map<String, String> headers) {
                        AppCenterLog.verbose(LOG_TAG, "Received a call back with payload " + payload);
                        mDbToken = payload;
                    }

                    @Override
                    public void onCallFailed(Exception e) {
                        handleApiCallFailure(e);
                    }
                });
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
