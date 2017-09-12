package com.microsoft.azure.mobile.rum;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;

import com.microsoft.azure.mobile.AbstractMobileCenterService;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.http.DefaultHttpClient;
import com.microsoft.azure.mobile.http.HttpClientNetworkStateHandler;
import com.microsoft.azure.mobile.http.HttpClientRetryer;
import com.microsoft.azure.mobile.http.ServiceCallback;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.NetworkStateHelper;
import com.microsoft.azure.mobile.utils.UUIDUtils;
import com.microsoft.azure.mobile.utils.async.MobileCenterFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import static com.microsoft.azure.mobile.http.DefaultHttpClient.METHOD_GET;

/**
 * RealUserMeasurements service.
 */
public class RealUserMeasurements extends AbstractMobileCenterService {

    /**
     * Name of the service.
     */
    private static final String SERVICE_NAME = "RealUserMeasurements";

    /**
     * TAG used in logging for Analytics.
     */
    private static final String LOG_TAG = MobileCenterLog.LOG_TAG + SERVICE_NAME;

    /**
     * Maximum length for the tag parameter.
     */
    private static final int MAX_TAG_LENGTH = 200;

    /**
     * JSON configuration file name.
     */
    private static final String CONFIGURATION_FILE_NAME = "rumconfig.json";

    /**
     * Warm up image path.
     */
    private static final String WARM_UP_IMAGE = "trans.gif";

    /**
     * TestUrl image path.
     */
    private static final String TEST_IMAGE = "17k.gif";

    /**
     * Report query string format.
     */
    private static String REPORT_URL_FORMAT = "https://%s?MonitorID=atm&rid=%s&w3c=false&prot=https&v=2017061301&tag=%s&DATA=%s";

    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static RealUserMeasurements sInstance;

    private Context mContext;

    private String mRumKey = "";

    private HttpClientNetworkStateHandler mHttpClient;

    private JSONObject mConfig;

    private Collection<TestUrl> mTestUrls;

    /**
     * Get shared instance.
     *
     * @return shared instance.
     */
    @SuppressWarnings("WeakerAccess")
    public static synchronized RealUserMeasurements getInstance() {
        if (sInstance == null) {
            sInstance = new RealUserMeasurements();
        }
        return sInstance;
    }

    public static void setRumKey(String rumKey) {
        getInstance().setInstanceRumKey(rumKey);
    }

    /**
     * Check whether RealUserMeasurements service is enabled or not.
     *
     * @return future with result being <code>true</code> if enabled, <code>false</code> otherwise.
     * @see MobileCenterFuture
     */
    public static MobileCenterFuture<Boolean> isEnabled() {
        return getInstance().isInstanceEnabledAsync();
    }

    /**
     * Enable or disable RealUserMeasurements service.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     * @return future with null result to monitor when the operation completes.
     */
    public static MobileCenterFuture<Void> setEnabled(boolean enabled) {
        return getInstance().setInstanceEnabledAsync(enabled);
    }

    private synchronized void setInstanceRumKey(String rumKey) {
        if (rumKey == null) {
            mRumKey = "";
        } else {
            rumKey = rumKey.trim();
            if (rumKey.length() > MAX_TAG_LENGTH) {
                rumKey = rumKey.substring(0, MAX_TAG_LENGTH);
            }
            mRumKey = rumKey;
        }
    }

    @Override
    public synchronized void onStarted(@NonNull Context context, @NonNull String appSecret, @NonNull Channel channel) {
        super.onStarted(context, appSecret, channel);
        mContext = context;
    }

    /**
     * React to enable state change.
     *
     * @param enabled current state.
     */
    @Override
    protected synchronized void applyEnabledState(boolean enabled) {
        if (enabled) {

            /* Configure HTTP client. */
            HttpClientRetryer retryer = new HttpClientRetryer(new DefaultHttpClient());
            NetworkStateHelper networkStateHelper = NetworkStateHelper.getSharedInstance(mContext);
            mHttpClient = new HttpClientNetworkStateHandler(retryer, networkStateHelper);

            /* Read JSON configuration. */
            mTestUrls = new ArrayList<>();
            try {
                InputStream stream = mContext.getAssets().open(CONFIGURATION_FILE_NAME);
                StringBuilder builder = new StringBuilder();
                //noinspection TryFinallyCanBeTryWithResources
                try {
                    InputStreamReader in = new InputStreamReader(stream, "UTF-8");
                    char[] buffer = new char[1024];
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        builder.append(buffer, 0, len);
                    }
                    String json = builder.toString();
                    mConfig = new JSONObject(json);
                } finally {
                    stream.close();
                }

                /* For each URL. */
                JSONArray endpoints = mConfig.getJSONArray("e");
                for (int i = 0; i < endpoints.length(); i++) {
                    JSONObject endpoint = endpoints.getJSONObject(i);
                    String url = "https://" + endpoint.getString("e") + "/apc/";
                    String requestId = UUIDUtils.randomUUID().toString();
                    mTestUrls.add(new TestUrl(url + WARM_UP_IMAGE + "?" + requestId, requestId, WARM_UP_IMAGE, "cold"));
                    mTestUrls.add(new TestUrl(url + TEST_IMAGE + "?" + requestId, requestId, TEST_IMAGE, "warm"));
                }
            } catch (IOException | JSONException e) {
                MobileCenterLog.error(LOG_TAG, "Could not read configuration file.", e);
            }

            /* Schedule tests. */
            testUrl(mTestUrls.iterator());
        }
    }

    private void testUrl(final Iterator<TestUrl> iterator) {
        if (iterator.hasNext()) {
            final long startTime = System.currentTimeMillis();
            final TestUrl testUrl = iterator.next();
            mHttpClient.callAsync(testUrl.url, METHOD_GET, Collections.<String, String>emptyMap(), null, new ServiceCallback() {

                @Override
                public void onCallSucceeded(String payload) {
                    testUrl.result = System.currentTimeMillis() - startTime;
                    testUrl(iterator);
                }

                @Override
                public void onCallFailed(Exception e) {
                    // TODO ignore failures later
                    onCallSucceeded(null);
                }
            });
        } else {

            /* Generate report. */
            try {
                JSONArray results = new JSONArray();
                for (TestUrl testUrl : mTestUrls) {
                    if (testUrl.result != null) {

                        JSONObject result = new JSONObject();
                        result.put("RequestID", testUrl.requestId);
                        result.put("Object", testUrl.object);
                        result.put("Conn", testUrl.conn);
                        result.put("Result", testUrl.result);
                        results.put(result);
                    }
                }
                JSONArray reportUrls = mConfig.getJSONArray("r");
                String reportJson = results.toString();
                String reportId = UUIDUtils.randomUUID().toString();
                report(reportUrls, reportJson, reportId, 0);
            } catch (JSONException e) {
                MobileCenterLog.error(LOG_TAG, "Failed to generate report.", e);
            }
        }
    }

    private void report(JSONArray reportUrls, String reportJson, String reportId, int reportUrlIndex) {
        if (reportUrlIndex < reportUrls.length()) {
            try {
                String reportUrl = String.format(REPORT_URL_FORMAT, reportUrls.getString(reportUrlIndex), reportId, mRumKey, reportJson);
                mHttpClient.callAsync(reportUrl, METHOD_GET, Collections.<String, String>emptyMap(), null, new ServiceCallback() {

                    @Override
                    public void onCallSucceeded(String payload) {
                        MobileCenterLog.info(LOG_TAG, "Measurements reported.");
                    }

                    @Override
                    public void onCallFailed(Exception e) {
                        MobileCenterLog.error(LOG_TAG, "Failed to report measurements.", e);
                    }
                });
            } catch (JSONException e) {
                MobileCenterLog.error(LOG_TAG, "Failed to generate report.", e);
            }
        }
    }

    @Override
    protected String getGroupName() {
        return null;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected String getLoggerTag() {
        return LOG_TAG;
    }

    private class TestUrl {

        final String url;

        final String requestId;

        final String object;

        final String conn;

        Long result;

        TestUrl(String url, String requestId, String object, String conn) {
            this.url = url;
            this.requestId = requestId;
            this.object = object;
            this.conn = conn;
        }
    }
}
