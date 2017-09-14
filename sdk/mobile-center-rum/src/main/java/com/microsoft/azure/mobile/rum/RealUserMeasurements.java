package com.microsoft.azure.mobile.rum;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.microsoft.azure.mobile.AbstractMobileCenterService;
import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.http.DefaultHttpClient;
import com.microsoft.azure.mobile.http.HttpClientNetworkStateHandler;
import com.microsoft.azure.mobile.http.ServiceCallback;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.NetworkStateHelper;
import com.microsoft.azure.mobile.utils.UUIDUtils;
import com.microsoft.azure.mobile.utils.async.MobileCenterFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
     * Rum key length.
     */
    private static final int RUM_KEY_LENGTH = 32;

    /**
     * Rum configuration endpoint.
     */
    private static final String CONFIGURATION_ENDPOINT = "https://rumconfig.trafficmanager.net";

    /**
     * JSON configuration file name.
     */
    private static final String CONFIGURATION_FILE_NAME = "rumConfig.js";

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
    private static final String REPORT_URL_FORMAT = "https://%s?MonitorID=atm&rid=%s&w3c=false&prot=https&v=2017061301&tag=%s&DATA=%s";

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
     * Configuration URL.
     */
    private String mConfigurationUrl = CONFIGURATION_ENDPOINT;

    /**
     * Rum configuration.
     */
    private JSONObject mConfig;

    /**
     * Tests to run.
     */
    private Collection<TestUrl> mTestUrls;

    /**
     * HTTP client.
     */
    private HttpClientNetworkStateHandler mHttpClient;

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

    public static void setConfigurationUrl(String url) {
        getInstance().setInstanceConfigurationUrl(url);
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

    /**
     * Implements {@link #setRumKey(String)} at instance level.
     */
    private synchronized void setInstanceRumKey(String rumKey) {
        if (rumKey == null) {
            MobileCenterLog.error(LOG_TAG, "Rum key is invalid.");
            return;
        }
        rumKey = rumKey.trim();
        if (rumKey.length() != RUM_KEY_LENGTH) {
            MobileCenterLog.error(LOG_TAG, "Rum key is invalid.");
            return;
        }
        mRumKey = rumKey;
    }

    private void setInstanceConfigurationUrl(String url) {
        mConfigurationUrl = url;
    }

    @Override
    public synchronized void onStarted(@NonNull Context context, @NonNull String appSecret, @NonNull Channel channel) {
        mContext = context;
        super.onStarted(context, appSecret, channel);
    }

    /**
     * React to enable state change.
     *
     * @param enabled current state.
     */
    @Override
    protected synchronized void applyEnabledState(boolean enabled) {
        if (enabled) {

            /* Check rum key. */
            if (mRumKey == null) {
                MobileCenterLog.error(LOG_TAG, "Rum key must be configured before start.");
                return;
            }

            /* Configure HTTP client with no retries but handling network state. */
            NetworkStateHelper networkStateHelper = NetworkStateHelper.getSharedInstance(mContext);
            mHttpClient = new HttpClientNetworkStateHandler(new DefaultHttpClient(), networkStateHelper);

            /* Get configuration. */
            mHttpClient.callAsync(mConfigurationUrl + "/" + CONFIGURATION_FILE_NAME, METHOD_GET, HEADERS, null, new ServiceCallback() {

                @Override
                public void onCallSucceeded(String payload) {

                    /* Read JSON configuration and start testing. */
                    try {

                        /* Parse configuration. */
                        mConfig = new JSONObject(payload);

                        /* Implement weighted random. */
                        JSONArray endpoints = mConfig.getJSONArray("e");
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
                        int testCount = Math.min(mConfig.getInt("n"), weightedEndpoints.size());
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

                            /* Port this Javascript behavior regarding url and requestId. */
                            else if (requestId.startsWith("*") && requestId.length() > 2) {
                                String domain = requestId.substring(2);
                                String uuid = UUIDUtils.randomUUID().toString();
                                baseUrl = uuid + "." + domain;
                                requestId = domain.equals("clo.footprintdns.com") ? uuid : domain;
                            }

                            /* Generate test urls. */
                            String probeId = UUIDUtils.randomUUID().toString();
                            String testUrl = String.format(TEST_URL_FORMAT, protocolSuffix, baseUrl, WARM_UP_IMAGE, probeId);
                            mTestUrls.add(new TestUrl(testUrl, requestId, WARM_UP_IMAGE, "cold"));
                            String testImage = (measurementType & FLAG_SEVENTEENK) > 0 ? SEVENTEENK_IMAGE : WARM_UP_IMAGE;
                            probeId = UUIDUtils.randomUUID().toString();
                            testUrl = String.format(TEST_URL_FORMAT, protocolSuffix, baseUrl, testImage, probeId);
                            mTestUrls.add(new TestUrl(testUrl, requestId, testImage, "warm"));
                        }

                        /* Run tests. */
                        testUrl(mTestUrls.iterator());
                    } catch (JSONException e) {
                        MobileCenterLog.error(LOG_TAG, "Could not read configuration file.", e);
                    }
                }

                @Override
                public void onCallFailed(Exception e) {
                    MobileCenterLog.error(LOG_TAG, "Could not get configuration file.", e);
                }
            });
        }
    }

    /**
     * Test urls one by one.
     */
    private void testUrl(final Iterator<TestUrl> iterator) {

        /* Iterate over next test url. */
        if (iterator.hasNext()) {
            final long startTime = System.currentTimeMillis();
            final TestUrl testUrl = iterator.next();
            MobileCenterLog.verbose(LOG_TAG, "Calling " + testUrl.url);
            mHttpClient.callAsync(testUrl.url, METHOD_GET, HEADERS, null, new ServiceCallback() {

                @Override
                public void onCallSucceeded(String payload) {
                    testUrl.result = System.currentTimeMillis() - startTime;
                    testUrl(iterator);
                }

                @Override
                public void onCallFailed(Exception e) {
                    MobileCenterLog.error(LOG_TAG, testUrl.url + " call failed", e);
                    testUrl(iterator);
                }
            });
        }

        /* Or report results after last one. */
        else {
            try {

                /* Generate report. */
                String reportId = UUIDUtils.randomUUID().toString();
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
                if (MobileCenter.getLogLevel() <= Log.VERBOSE) {
                    MobileCenterLog.verbose(LOG_TAG, "Report payload=" + results.toString(2));
                }

                /* There can be more than 1 report URL, parse them. */
                JSONArray reportUrls = mConfig.getJSONArray("r");

                /* Report 1 by 1. */
                report(reportUrls, reportJson, reportId, 0);
            } catch (JSONException e) {
                MobileCenterLog.error(LOG_TAG, "Failed to generate report.", e);
            }
        }
    }

    /**
     * Report the results to 1 endpoint at a time.
     */
    private void report(final JSONArray reportUrls, final String reportJson, final String reportId, final int reportUrlIndex) {
        if (reportUrlIndex < reportUrls.length()) {
            try {
                String reportUrl = reportUrls.getString(reportUrlIndex);
                String parameters = URLEncoder.encode(reportJson, "UTF-8");
                reportUrl = String.format(REPORT_URL_FORMAT, reportUrl, reportId, mRumKey, parameters);
                MobileCenterLog.verbose(LOG_TAG, "Calling " + reportUrl);
                mHttpClient.callAsync(reportUrl, METHOD_GET, HEADERS, null, new ServiceCallback() {

                    @Override
                    public void onCallSucceeded(String payload) {
                        MobileCenterLog.info(LOG_TAG, "Measurements reported.");
                        reportNextUrl();
                    }

                    @Override
                    public void onCallFailed(Exception e) {
                        MobileCenterLog.error(LOG_TAG, "Failed to report measurements.", e);
                        reportNextUrl();
                    }

                    private void reportNextUrl() {
                        report(reportUrls, reportJson, reportId, reportUrlIndex + 1);
                    }
                });
            } catch (JSONException | UnsupportedEncodingException e) {
                MobileCenterLog.error(LOG_TAG, "Failed to generate report.", e);
            }
        } else {
            MobileCenterLog.info(LOG_TAG, "Measurements reported to all report endpoints.");
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

        TestUrl(String url, String requestId, String object, String conn) {
            this.url = url;
            this.requestId = requestId;
            this.object = object;
            this.conn = conn;
        }
    }
}
