package com.microsoft.appcenter.crashes;
import android.os.Build;
//import android.support.annotation.RequiresApi;

import androidx.annotation.RequiresApi;

import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.appcenter.crashes.ingestion.models.ManagedErrorLog;
import com.microsoft.appcenter.crashes.ingestion.models.json.ErrorAttachmentLogFactory;
import com.microsoft.appcenter.crashes.ingestion.models.json.ManagedErrorLogFactory;
import com.microsoft.appcenter.crashes.model.ErrorReport;
import com.microsoft.appcenter.crashes.utils.ErrorLogHelper;
import com.microsoft.appcenter.ingestion.models.json.DefaultLogSerializer;
import com.microsoft.appcenter.utils.storage.FileManager;

import org.json.JSONException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class WatsonCrashReporting {
    private static final String FIELD_URL = "https://nw-umwatson.events.data.microsoft.com/telemetry.request";
    private static final String PARAM_HTTP_METHOD = "POST";
    private static final String PARAM_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "text/xml";
    private static final String PARAM_HOST= "Host";
    private static final String PARAM_HOST_VALUE = "watson.telemetry.microsoft.com";
    private static final String PARAM_EXPECT= "Expect";
    private static final String PARAM_EXPECT_VALUE = "100-continue";
    private static final String PARAM_CONNECTION = "CONNECTION";
    private static final String PARAM_CONNECTION_VALUE = "Keep-Alive";
    private static final String PARAM_CONTENT_TYPE_NOTPOST = "Content-Type";
    private static final String CONTENT_TYPE_VALUE_NOTPOST =  "Application/octet-stream";
    private static String m_stageOneHitReceiptBucketID;
    private static String  m_stageOneCabUploadToken;
    private static String mStageTwoCabID;
    private static String  mStageTwoCabGUID;
    private static String m_stageOneHitReceiptBucketHash;
    private static String m_stageOneHitReceiptBucketTBL;
    private static String cabSize;
    private static boolean stageOneRetried = false;
    private static boolean stageTwoRetried = false;
    private static final String PARAM_CONTENT_LENGTH =  "Content-Length";
    public static int stage = 1;
    private static boolean _crashLogHasBeenProcessed = false;



    public static String xmlStringOne(ManagedErrorLog log, ErrorReport errorReport) throws NoSuchAlgorithmException {


        String kStageOneXMLBodyRequest = "<req ver=\"2\"><tlm><src><desc><mach><os><arg nm=\"vermaj\" val=\"%s\" /><arg nm=\"vermin\" val=\"%s\" /><arg nm=\"verbld\" val=\"%s\" /><arg nm=\"arch\" val=\"%s\" /><arg nm=\"lcid\" val=\"%s\" /></os><hw><arg nm=\"sysmfg\" val=\"%s\" /><arg nm=\"syspro\" val=\"%s\" /></hw><ctrl><arg nm=\"tm\" val=\"%s\" /><arg nm=\"mid\" val=\"%s\" /><arg nm=\"sample\" val=\"1\" /><arg nm=\"msft\" val=\"%s\" /><arg nm=\"test\" val=\"%s\" /></ctrl></mach></desc></src><reqs><req key=\"1\"><namespace svc=\"watson\" ptr=\"APEX\" gp=\"Office\" app=\"%s\"></namespace><cmd nm=\"event\"><arg nm=\"cat\" val=\"generic\" /><arg nm=\"eventtype\" val=\"%s\" /><arg nm=\"p1\" val=\"%s\" /><arg nm=\"p2\" val=\"%s\" /><arg nm=\"p3\" val=\"%s\" /><arg nm=\"p8\" val=\"%s\" /><arg nm=\"p9\" val=\"%s\" /></cmd></req></reqs></tlm></req>";
        long timeStamp = (long) (new Date().getTime() / 1000) * 1000;
        String timeStampString = String.format("%.0f", (double) timeStamp);
        String architecture = "0";
        if (log.getArchitecture().equals("arm64")) {
            architecture = "12";
        } else if (log.getArchitecture().equals("x86_64")) {
            architecture = "9";
        }
        String stackHashValue = getStackHash(errorReport);
        String formattedXmlBodyOne = String.format(kStageOneXMLBodyRequest,
                //Os parameters
                log.getDevice().getOsVersion(), log.getDevice().getOsVersion(), log.getDevice().getOsBuild(), architecture, log.getDevice().getLocale(),
                //HW parameters
                log.getDevice().getOemName(), log.getDevice().getModel(),
                //CTRL parameters
                timeStampString, log.getId(), "1", "1",
                //namespace parameter
                log.getDevice().getAppNamespace(),
                //cmd parameters
                "AndroidAppCrash", log.getDevice().getAppNamespace(), log.getDevice().getAppVersion(), log.getAppLaunchTimestamp(), log.getException(), stackHashValue);

        return formattedXmlBodyOne;

    }

    public static String xmlStringTwo(ManagedErrorLog log) {


        String kStageTwoXMLBodyRequest = "<req ver=\"2\"><tlm><src><desc><mach><os><arg nm=\"vermaj\" val=\"%@\" /><arg nm=\"vermin\" val=\"%@\" /><arg nm=\"verbld\" val=\"%@\" /><arg nm=\"arch\" val=\"%@\" /><arg nm=\"lcid\" val=\"%u\" /></os><hw><arg nm=\"sysmfg\" val=\"apple\" /><arg nm=\"syspro\" val=\"%@\" /></hw><ctrl><arg nm=\"tm\" val=\"%@\" /><arg nm=\"mid\" val=\"%@\" /><arg nm=\"sample\" val=\"1\" /><arg nm=\"msft\" val=\"%@\" /><arg nm=\"test\" val=\"%@\" /></ctrl></mach></desc></src><reqs><payload><arg nm=\"size\" val=\"%@\" /><arg nm=\"comp\" val=\"zip\" /></payload><req key=\"dflt\"><namespace svc=\"watson\" ptr=\"apex\" gp=\"office\" app=\"%@\" /><cmd nm=\"dataupload\"><arg nm=\"bucket\" val=\"%@\" /><arg nm=\"buckettbl\" val=\"%@\" /><arg nm=\"eventtype\" val=\"%@\" /><arg nm=\"size\" val=\"%@\" /><arg nm=\"token\" val=\"%@\" /></cmd></req></reqs></tlm></req>";
        long timeStamp = (long) (new Date().getTime() / 1000) * 1000;
        String timeStampString = String.format("%.0f", (double) timeStamp);
        String architecture = "0";
        if (log.getArchitecture() == "arm64") {
            architecture = "12";
        } else if (log.getArchitecture() == "x86_64") {
            architecture = "9";
        }

        String formattedXmlBodyTwo = String.format(kStageTwoXMLBodyRequest,
                //Os parameters
                log.getDevice().getOsVersion(), log.getDevice().getOsVersion(), log.getDevice().getOsBuild(), architecture, log.getDevice().getLocale(),
                //HW parameters
                log.getDevice().getOemName(), log.getDevice().getModel(),
                //ctrl parameters
                timeStampString, log.getId(), "1", "1",
                //payload elements
                cabSize,
                //namespace parameters
                log.getDevice().getAppNamespace(),
                //cmd parameters
                m_stageOneHitReceiptBucketHash, m_stageOneHitReceiptBucketTBL, "AndroidAppCrash", cabSize, m_stageOneCabUploadToken);

        return formattedXmlBodyTwo;

    }

    public static int handleHTTPRequestNewProtocol(String method, String urlString, byte[] body, byte[] dataReturned, int encodedLength) throws IOException {
        int httpStatus = 0;
        byte[] responseData = null;
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = null;
        OutputStream outputStream = null;

        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty(PARAM_HOST, PARAM_HOST_VALUE);
            urlConnection.setRequestProperty(PARAM_EXPECT, PARAM_EXPECT_VALUE);

            if (method != null) {
                urlConnection.setRequestMethod(method);
                if (method == PARAM_HTTP_METHOD) {
                    urlConnection.setRequestProperty(PARAM_CONTENT_TYPE, CONTENT_TYPE_VALUE);
                    urlConnection.setRequestProperty(PARAM_CONNECTION, PARAM_CONNECTION_VALUE);
                } else {
                    urlConnection.setRequestProperty(PARAM_CONTENT_TYPE_NOTPOST, CONTENT_TYPE_VALUE_NOTPOST);
                }
            }
            if (body != null) {
                ByteArrayOutputStream requestBody = new ByteArrayOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(requestBody);
                dataOutputStream.writeInt(encodedLength);
                dataOutputStream.write(body);
                urlConnection.setRequestProperty(PARAM_CONTENT_LENGTH, String.valueOf(requestBody.size()));
                urlConnection.setDoOutput(true);
                outputStream = urlConnection.getOutputStream();
                outputStream.write(requestBody.toByteArray());
                outputStream.flush();
                outputStream.close();
            }

            httpStatus = urlConnection.getResponseCode();
            if (httpStatus == HttpURLConnection.HTTP_OK) {

                      String responseBody = "";
                      InputStream inputStream = urlConnection.getInputStream();
                      if (inputStream != null)
                      {
                          Scanner scanner = new Scanner(inputStream);
                          StringBuilder stringBuilder = new StringBuilder();
                          while (scanner.hasNextLine())
                          {
                              stringBuilder.append(scanner.nextLine());
                          }
                          responseBody = stringBuilder.toString();
                            responseData = responseBody.getBytes();
                      }

                  }

        } catch (Exception e) {
            e.printStackTrace();
        }

        urlConnection.disconnect();

        if (dataReturned != null) {
            dataReturned = responseData;
        }


        return httpStatus;
    }

    private static String getStackHash(ErrorReport errorReport) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hashBytes = md.digest(errorReport.getStackTrace().getBytes());
        StringBuilder hashBuilder = new StringBuilder();
        for (byte b : hashBytes) {
            hashBuilder.append(String.format("%02x", b));
        }
        return hashBuilder.toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static WatsonStage executeStageOneNewProtocol(ManagedErrorLog log, ErrorReport errorReport) throws IOException, JSONException, NoSuchAlgorithmException {
        byte[] stageOneResponse = null;
        Integer httpResult = 0;
      WatsonStage newStage = WatsonStage.kWatsonStageStarted;
        byte[] requestXMLBody = xmlStringOne(log, errorReport).getBytes(StandardCharsets.UTF_8);
        int encodedLength = requestXMLBody.length;



                                    //WE NEED TO HANDLE THROTTLING CASE

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Integer> future = executor.submit(new Callable<Integer>() {
            public Integer call() throws Exception {
                try {
                    return handleHTTPRequestNewProtocol("POST", FIELD_URL, requestXMLBody, stageOneResponse, encodedLength);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        });

        try {
            httpResult = future.get(); // Get the result of the background execution
        } catch (Exception e) {
            httpResult = 0; // Default value or error handling
        }
        executor.shutdown(); // Shutdown the executor

        if (httpResult >= 200 && httpResult < 300) {
            parseStageOneResponseNewProtocol(stageOneResponse);
        }
        if (m_stageOneHitReceiptBucketID != null && (httpResult >= 200 && httpResult < 300)) {
            if (m_stageOneCabUploadToken != null && m_stageOneHitReceiptBucketHash != null && m_stageOneHitReceiptBucketTBL != null) {
                // If the response contains a send command item (<cmd nm="send">) along with a token and bucket info, it means we should upload a CAB file and move to stage two.
                newStage = WatsonStage.kWatsonStageTwo;
            }
            else {
                // If no token but http result == 200 and the response <cmd nm="receipt"> contains an iBucket, then reporting was successful. We are done.
                newStage = WatsonStage.kWatsonStageComplete;
            }
        } else if (!stageOneRetried) {
            // If something went wrong, let's give it one more shot.
            newStage = WatsonStage.kWatsonStageRetryStageOne;
            stageOneRetried = true;
            _crashLogHasBeenProcessed = true;
            //Marking this as processed regardless of what happens with our retry. At this point we are done.
        }
        return newStage;
    }

    private static void parseStageOneResponseNewProtocol(byte[] response) {
        m_stageOneHitReceiptBucketID = null;
        m_stageOneCabUploadToken = null;
        try {
            ParserDelegate parserDelegate = new ParserDelegate();
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            InputSource is = new InputSource(new ByteArrayInputStream(response));
            saxParser.parse(is, parserDelegate);//xml parsing happens here
            if (parserDelegate.isInvalidRequest()) {
                throw new AssertionError("Invalid request, we should not retry. Something is formatted incorrectly.");
            } else {
                m_stageOneHitReceiptBucketID = parserDelegate.getBucketID();
            }
            if (parserDelegate.isShouldCollectCab() || parserDelegate.isShouldBypassThrottle()) {
                m_stageOneCabUploadToken = parserDelegate.getCabUploadToken();
                m_stageOneHitReceiptBucketHash = parserDelegate.getBucketHash();
                m_stageOneHitReceiptBucketTBL = parserDelegate.getBuckettbl();
            }

              //handle the cases of isShouldThrottle() , isShouldBypassThrottle() (Reference :Watson iOS code)


        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static WatsonStage executeStageTwoNewProtocol(File curLogFile, ManagedErrorLog log) throws IOException, JSONException {
        byte[] stageTwoResponse = null;
        int httpResult = 0;
        WatsonStage newStage = WatsonStage.kWatsonStageStarted;
        boolean shouldRetryCabUpload = false;
        File logFile = curLogFile;
        FileInputStream fis = new FileInputStream(logFile);
        byte[] fileBytes = new byte[(int) logFile.length()];
        fis.read(fileBytes);
        byte[] cabFile = fileBytes;
        int cabSizeInBytes = fileBytes.length;
        cabSize = Integer.toString(cabSizeInBytes);
        // Create a ByteArrayOutputStream object to hold the request XML body
        ByteArrayOutputStream requestXMLBody = new ByteArrayOutputStream();
// Write the XML body with parameters to the ByteArrayOutputStream object in UTF-8 format
        requestXMLBody.write(xmlStringTwo(log).getBytes(StandardCharsets.UTF_8));
// Calculate the encoded length of the requestXMLBody
        int encodedLength = requestXMLBody.size();
        requestXMLBody.write(cabFile);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Integer> future = executor.submit(new Callable<Integer>() {
            public Integer call() throws Exception {
                try {
                    return handleHTTPRequestNewProtocol("PUT", FIELD_URL, requestXMLBody.toByteArray(), stageTwoResponse, encodedLength);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        });

        try {
            httpResult = future.get(); // Get the result of the background execution
        } catch (Exception e) {
            httpResult = 0; // Default value or error handling
        }
        executor.shutdown();
        if (httpResult >= 200 && httpResult < 300) {
            shouldRetryCabUpload = parseStageTwoResponseNewProtocol(stageTwoResponse);
        }
        if (!shouldRetryCabUpload || (mStageTwoCabID != null && mStageTwoCabGUID != null)) {
            // If we failed to upload but the service is not asking for a retry or if we succeeded uploading, we are done.
            newStage = WatsonStage.kWatsonStageComplete;
            _crashLogHasBeenProcessed = true; //The service didn't ask for a retry, safe to delete the crash log now.

        } else if (!stageTwoRetried) {
            newStage = WatsonStage.kWatsonStageRetryStageTwo;

            stageTwoRetried = true;
        }

        return newStage;
    }

    private static boolean parseStageTwoResponseNewProtocol(byte[] response) {
        mStageTwoCabID = null;
        mStageTwoCabGUID = null;
        boolean shouldRetryCabUpload = false;

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            ParserDelegate parserDelegate = new ParserDelegate();
            saxParser.parse(new ByteArrayInputStream(response), parserDelegate);

            if (!parserDelegate.isDidCabUploadFailed()) {
                mStageTwoCabID = parserDelegate.getCabID();
                mStageTwoCabGUID = parserDelegate.getCabGUID();
                if (mStageTwoCabID != null && mStageTwoCabGUID != null) {
                    _crashLogHasBeenProcessed = true;
                }
            } else {
                shouldRetryCabUpload = parserDelegate.isShouldRetryCabUpload();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return shouldRetryCabUpload;
    }
}
