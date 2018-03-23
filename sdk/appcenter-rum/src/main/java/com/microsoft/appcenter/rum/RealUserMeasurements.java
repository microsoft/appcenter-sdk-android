package com.microsoft.appcenter.rum;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.http.DefaultHttpClient;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpClientNetworkStateHandler;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.UUIDUtils;
import com.microsoft.appcenter.utils.async.AppCenterFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;

import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_GET;

/**
 * RealUserMeasurements service.
 */
public class RealUserMeasurements extends AbstractAppCenterService {

    /**
     * Name of the service.
     */
    private static final String SERVICE_NAME = "RealUserMeasurements";

    /**
     * TAG used in logging for Analytics.
     */
    private static final String LOG_TAG = AppCenterLog.LOG_TAG + SERVICE_NAME;

    /**
     * Rum key length.
     */
    private static final int RUM_KEY_LENGTH = 32;

    /**
     * Rum configuration endpoints.
     */
    private static final String[] CONFIGURATION_ENDPOINTS = {
            "https://www.atmrum.net/conf/v1/atm/fpconfig.min.json",
    };

    /**
     * Warm up image path.
     */
    private static final String WARM_UP_IMAGE = "trans.gif";

    /**
     * TestUrl image path.
     */
    private static final String SEVENTEENK_IMAGE = "17k.gif";

    /**
     * Flag to support https.
     */
    private static final int FLAG_HTTPS = 1;

    /**
     * Flag to use the 17k image to test.
     */
    private static final int FLAG_SEVENTEENK = 12;

    /**
     * Test url format.
     */
    private static final String TEST_URL_FORMAT = "http%s://%s/apc/%s?%s";

    /**
     * Report url format.
     */
    private static final String REPORT_URL_FORMAT = "https://%s?MonitorID=atm-mc&rid=%s&w3c=false&prot=https&v=2017061301&tag=%s&DATA=%s";

    /**
     * Additional headers.
     */
    private static final Map<String, String> HEADERS = Collections.emptyMap();

    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static RealUserMeasurements sInstance;

    /**
     * Application context.
     */
    private Context mContext;

    /**
     * Rum key.
     */
    private String mRumKey;

    /**
     * HTTP client.
     */
    private HttpClientNetworkStateHandler mHttpClient;

    /**
     * Rum configuration.
     */
    private JSONObject mConfiguration;

    /**
     * Tests to run.
     */
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

    @SuppressWarnings("WeakerAccess")
    public static void setRumKey(String rumKey) {
        getInstance().setInstanceRumKey(rumKey);
    }

    /**
     * Check whether RealUserMeasurements service is enabled or not.
     *
     * @return future with result being <code>true</code> if enabled, <code>false</code> otherwise.
     * @see AppCenterFuture
     */
    public static AppCenterFuture<Boolean> isEnabled() {
        return getInstance().isInstanceEnabledAsync();
    }

    /**
     * Enable or disable RealUserMeasurements service.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     * @return future with null result to monitor when the operation completes.
     */
    public static AppCenterFuture<Void> setEnabled(boolean enabled) {
        return getInstance().setInstanceEnabledAsync(enabled);
    }

    /**
     * All unique identifiers used in Rum don't have dashes...
     */
    private static String rumUniqueId() {
        return UUIDUtils.randomUUID().toString().replace("-", "");
    }

    /**
     * Implements {@link #setRumKey(String)} at instance level.
     */
    private synchronized void setInstanceRumKey(String rumKey) {
        if (rumKey == null) {
            AppCenterLog.error(LOG_TAG, "Rum key is invalid.");
            return;
        }
        rumKey = rumKey.trim();
        if (rumKey.length() != RUM_KEY_LENGTH) {
            AppCenterLog.error(LOG_TAG, "Rum key is invalid.");
            return;
        }
        mRumKey = rumKey;
    }

    @Override
    public synchronized void onStarted(@NonNull Context context, String appSecret, String transmissionTargetToken, @NonNull Channel channel) {
        mContext = context;
        super.onStarted(context, appSecret, transmissionTargetToken, channel);
    }

    /**
     * React to enable state change.
     *
     * @param enabled current state.
     */
    @Override
    protected synchronized void applyEnabledState(boolean enabled) {
        if (enabled) {

            /* Snapshot run key for the run to avoid race conditions, we ignore updates. */
            final String rumKey = mRumKey;

            /* Check rum key. */
            if (rumKey == null) {
                AppCenterLog.error(LOG_TAG, "Rum key must be configured before start.");
                return;
            }

            /* Configure HTTP client with no retries but handling network state. */
            NetworkStateHelper networkStateHelper = NetworkStateHelper.getSharedInstance(mContext);
            mHttpClient = new HttpClientNetworkStateHandler(new DefaultHttpClient(), networkStateHelper);

            /* Get configuration. */
            getConfiguration(0, rumKey, mHttpClient);
        }

        /* On disabling, cancel everything. */
        else if (mHttpClient != null) {
            try {
                mHttpClient.close();
            } catch (IOException e) {
                AppCenterLog.error(LOG_TAG, "Failed to close http client.", e);
            }
            mHttpClient = null;
            mConfiguration = null;
            mTestUrls = null;
        }
    }

    /**
     * Get configuration.
     */
    private synchronized void getConfiguration(final int configurationUrlIndex, final String rumKey, final HttpClient httpClient) {

        /* Check if a disable happened while we were waiting the remote configuration. */
        if (httpClient != mHttpClient) {
            return;
        }
        if (configurationUrlIndex >= CONFIGURATION_ENDPOINTS.length) {
            AppCenterLog.error(LOG_TAG, "Could not get configuration file from any of the endpoints.");
            return;
        }
        final String url = CONFIGURATION_ENDPOINTS[configurationUrlIndex];
        AppCenterLog.verbose(LOG_TAG, "Calling " + url);
        httpClient.callAsync(url, METHOD_GET, HEADERS, null, new ServiceCallback() {

            @Override
            public void onCallSucceeded(String payload) {

                /* Read JSON configuration and start testing. */
                handleRemoteConfiguration(httpClient, rumKey, payload);
            }

            @Override
            public void onCallFailed(Exception e) {

                /* Log error and try the next configuration endpoint. */
                AppCenterLog.error(LOG_TAG, "Could not get configuration file at " + url, e);
                getConfiguration(configurationUrlIndex + 1, rumKey, httpClient);
            }
        });
    }

    /**
     * After getting the remote configuration, schedule test runs.
     */
    private synchronized void handleRemoteConfiguration(HttpClient httpClient, String rumKey, String configurationPayload) {

        /* Check if a disable happened while we were waiting the remote configuration. */
        if (httpClient != mHttpClient) {
            return;
        }
        try {

            /* Parse configuration. */
            mConfiguration = new JSONObject(configurationPayload);

            /* Implement weighted random. */
            JSONArray endpoints = mConfiguration.getJSONArray("e");
            int totalWeight = 0;
            List<JSONObject> weightedEndpoints = new ArrayList<>(endpoints.length());
            for (int i = 0; i < endpoints.length(); i++) {
                JSONObject endpoint = endpoints.getJSONObject(i);
                int weight = endpoint.getInt("w");
                if (weight > 0) {
                    totalWeight += weight;
                    endpoint.put("cumulatedWeight", totalWeight);
                    weightedEndpoints.add(endpoint);
                }
            }

            /* Select n endpoints randomly. */
            mTestUrls = new ArrayList<>();
            Random random = new Random();
            int testCount = Math.min(mConfiguration.getInt("n"), weightedEndpoints.size());
            for (int n = 0; n < testCount; n++) {

                /* Select random endpoint. */
                double randomWeight = Math.floor(random.nextDouble() * totalWeight);
                JSONObject endpoint = null;
                ListIterator<JSONObject> iterator = weightedEndpoints.listIterator();
                while (iterator.hasNext()) {
                    JSONObject weightedEndpoint = iterator.next();
                    int cumulatedWeight = weightedEndpoint.getInt("cumulatedWeight");
                    if (endpoint == null) {
                        if (randomWeight <= cumulatedWeight) {
                            endpoint = weightedEndpoint;
                            iterator.remove();
                        }
                    }

                    /* Update subsequent endpoints cumulated weights since we removed an element. */
                    else {
                        cumulatedWeight -= endpoint.getInt("w");
                        weightedEndpoint.put("cumulatedWeight", cumulatedWeight);
                    }
                }

                /* Update total weight since we removed the picked endpoint. */
                //noinspection ConstantConditions
                totalWeight -= endpoint.getInt("w");

                /* Use endpoint to generate test urls. */
                String protocolSuffix = "";
                int measurementType = endpoint.getInt("m");
                if ((measurementType & FLAG_HTTPS) > 0) {
                    protocolSuffix = "s";
                }
                String requestId = endpoint.getString("e");

                /* Handle backward compatibility with FPv1. */
                String baseUrl = requestId;
                if (!requestId.contains(".")) {
                    baseUrl += ".clo.footprintdns.com";
                }

                /* Handle wildcard sub-domain testing. */
                else if (requestId.startsWith("*") && requestId.length() > 2) {
                    String domain = requestId.substring(2);
                    String uuid = rumUniqueId();
                    baseUrl = uuid + "." + domain;
                    requestId = domain.equals("clo.footprintdns.com") ? uuid : domain;
                }

                /* Generate test urls. */
                String probeId = rumUniqueId();
                String testUrl = String.format(TEST_URL_FORMAT, protocolSuffix, baseUrl, WARM_UP_IMAGE, probeId);
                mTestUrls.add(new TestUrl(testUrl, requestId, WARM_UP_IMAGE, "cold"));
                String testImage = (measurementType & FLAG_SEVENTEENK) > 0 ? SEVENTEENK_IMAGE : WARM_UP_IMAGE;
                probeId = rumUniqueId();
                testUrl = String.format(TEST_URL_FORMAT, protocolSuffix, baseUrl, testImage, probeId);
                mTestUrls.add(new TestUrl(testUrl, requestId, testImage, "warm"));
            }

            /* Run tests. */
            testUrl(httpClient, rumKey, mTestUrls.iterator());
        } catch (JSONException e) {
            AppCenterLog.error(LOG_TAG, "Could not read configuration file.", e);
        }
    }

    /**
     * Test urls one by one.
     */
    private synchronized void testUrl(final HttpClient httpClient, final String rumKey, final Iterator<TestUrl> iterator) {

        /* Check if a disable happened while we were waiting for a call result. */
        if (httpClient != mHttpClient) {
            return;
        }

        /* Iterate over next test url. */
        if (iterator.hasNext()) {
            final long startTime = System.currentTimeMillis();
            final TestUrl testUrl = iterator.next();
            AppCenterLog.verbose(LOG_TAG, "Calling " + testUrl.url);
            mHttpClient.callAsync(testUrl.url, METHOD_GET, HEADERS, null, new ServiceCallback() {

                @Override
                public void onCallSucceeded(String payload) {
                    testUrl.result = System.currentTimeMillis() - startTime;
                    testUrl(httpClient, rumKey, iterator);
                }

                @Override
                public void onCallFailed(Exception e) {
                    AppCenterLog.error(LOG_TAG, testUrl.url + " call failed", e);
                    testUrl(httpClient, rumKey, iterator);
                }
            });
        }

        /* Or report results after last one. */
        else {
            try {

                /* Generate report. */
                String reportId = rumUniqueId();
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
                String reportJson = results.toString();
                if (AppCenter.getLogLevel() <= Log.VERBOSE) {
                    AppCenterLog.verbose(LOG_TAG, "Report payload=" + results.toString(2));
                }

                /* There can be more than 1 report URL, parse them. */
                JSONArray reportUrls = mConfiguration.getJSONArray("r");

                /* Report. */
                report(httpClient, rumKey, reportUrls, reportJson, reportId, 0);
            } catch (JSONException e) {
                AppCenterLog.error(LOG_TAG, "Failed to generate report.", e);
            }
        }
    }

    /**
     * Report the results to 1 endpoint at a time, stopping at the first one that succeeds.
     */
    private synchronized void report(final HttpClient httpClient, final String rumKey, final JSONArray reportUrls, final String reportJson, final String reportId, final int reportUrlIndex) {

        /* Check if a disable happened while we were waiting for a call result. */
        if (httpClient != mHttpClient) {
            return;
        }

        /* Check if we still have urls to report to. */
        if (reportUrlIndex < reportUrls.length()) {
            try {
                String reportUrl = reportUrls.getString(reportUrlIndex);
                String parameters = URLEncoder.encode(reportJson, "UTF-8");
                reportUrl = String.format(REPORT_URL_FORMAT, reportUrl, reportId, rumKey, parameters);
                final String finalReportUrl = reportUrl;
                AppCenterLog.verbose(LOG_TAG, "Calling " + finalReportUrl);
                mHttpClient.callAsync(finalReportUrl, METHOD_GET, HEADERS, null, new ServiceCallback() {

                    @Override
                    public void onCallSucceeded(String payload) {
                        AppCenterLog.info(LOG_TAG, "Measurements reported successfully.");
                    }

                    @Override
                    public void onCallFailed(Exception e) {
                        AppCenterLog.error(LOG_TAG, "Failed to report measurements at " + finalReportUrl, e);
                        reportNextUrl();
                    }

                    private void reportNextUrl() {
                        report(httpClient, rumKey, reportUrls, reportJson, reportId, reportUrlIndex + 1);
                    }
                });
            } catch (JSONException | UnsupportedEncodingException e) {
                AppCenterLog.error(LOG_TAG, "Failed to generate report.", e);
            }
        } else {
            AppCenterLog.error(LOG_TAG, "Measurements report failed on all report endpoints.");
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

    /**
     * Test description.
     */
    private static class TestUrl {

        /**
         * Test url.
         */
        final String url;

        /**
         * Request identifier.
         */
        final String requestId;

        /**
         * What image is tested.
         */
        final String object;

        /**
         * Is it cold or warm test?.
         */
        final String conn;

        /**
         * Time it took to call the url in ms.
         */
        Long result;

        /**
         * Init.
         */
        TestUrl(String url, String requestId, String object, String conn) {
            this.url = url;
            this.requestId = requestId;
            this.object = object;
            this.conn = conn;
        }
    }
}
