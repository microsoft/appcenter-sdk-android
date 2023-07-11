package com.microsoft.appcenter.crashes;

//import static com.microsoft.appcenter.crashes.WatsonCrashReporting.WatsonStage.kWatsonStageDone;

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


//    public void watsonCode() throws JSONException, IOException, NoSuchAlgorithmException {
//        File[] folder = ErrorLogHelper.getStoredErrorLogFiles();
//        for (File curLogFile : ErrorLogHelper.getStoredErrorLogFiles()) {
//            ErrorReport errorReport = null;
//            String logfileContents = FileManager.read(curLogFile);
//            mLogSerializer = new DefaultLogSerializer();
//            mLogSerializer.addLogFactory(ManagedErrorLog.TYPE, ManagedErrorLogFactory.getInstance());
//            mLogSerializer.addLogFactory(ErrorAttachmentLog.TYPE, ErrorAttachmentLogFactory.getInstance());
//            ManagedErrorLog log = null;
//            try {
//                log = (ManagedErrorLog) mLogSerializer.deserializeLog(logfileContents, null);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            errorReport = CrashUtils.buildErrorReport(log);
//            if (stage == 1) {
//                executeStageOneNewProtocol(curLogFile ,log, errorReport);
//                stage = 2 ;
//            }
//            else if(stage == 2)
//            {
//                executeStageTwoNewProtocol(curLogFile,log);
//            }
//            curLogFile.delete();
//            System.out.println(folder);
//        }
//
//    }
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
                "eventtypeValue", log.getDevice().getAppNamespace(), log.getDevice().getAppVersion(), log.getAppLaunchTimestamp(), log.getException(), stackHashValue);

//            return formattedXmlBodyOne.getBytes(StandardCharsets.UTF_8);
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
                m_stageOneHitReceiptBucketHash, m_stageOneHitReceiptBucketTBL, "", cabSize, m_stageOneCabUploadToken);

//            return formattedXmlBodyTwo.getBytes(StandardCharsets.UTF_8);
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
//                urlConnection.setConnectTimeout(10000);    // wait at most 10 seconds for a response
            httpStatus = urlConnection.getResponseCode();
            if (httpStatus == HttpURLConnection.HTTP_OK) {
//                InputStream inputStream = urlConnection.getInputStream();
//                responseData = IOUtils.toByteArray(inputStream);
//                inputStream.close();


                //////****** DONT DELETE IT*****////


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

//            sem.release();
//              try {
//                  sem.tryAcquire(10, TimeUnit.SECONDS);
//              } catch (InterruptedException e) {
//                  e.printStackTrace();
//              }
        urlConnection.disconnect();

        if (dataReturned != null) {
            dataReturned = responseData;
        }


        return httpStatus;
    }

    private static String getStackHash(ErrorReport errorReport) throws NoSuchAlgorithmException {
//            Throwable throwable = new Throwable();
//            throwable.setStackTrace(parseStackTrace(log.getException().getStackTrace()));
//
//// Access the stack trace as an array of StackTraceElement
//            StackTraceElement[] stackTraceElements = throwable.getStackTrace();
//            StackTraceElement[] stackTrace = log.getException().getStackTrace();
//            StringBuilder stackHashBuilder = new StringBuilder();
//            for (StackTraceElement element : stackTrace) {
//                stackHashBuilder.append(element.getClassName())
//                        .append(element.getMethodName())
//                        .append(element.getLineNumber());
//            }
//            String stackHash = stackHashBuilder.toString();
//            try {
//                byte[] stackHashBytes = stackHash.getBytes();
//                MessageDigest md5Digest = MessageDigest.getInstance("MD5");
//                byte[] hashBytes = md5Digest.digest(stackHashBytes);
//                stackHash = Arrays.toString(hashBytes);
//            } catch (NoSuchAlgorithmException e) {
//                // Handle the exception
//            }
//            return stackHash;
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hashBytes = md.digest(errorReport.getStackTrace().getBytes());

        // Convert the byte array to hexadecimal representation
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
        WatsonStage newStage = WatsonStage.kWatsonStageDone;
//            byte[] requestXMLBody = getLogFiles(1).getBytes(StandardCharsets.UTF_8);
        byte[] requestXMLBody = xmlStringOne(log, errorReport).getBytes(StandardCharsets.UTF_8);

        // Get the encoded length of the requestXMLBody
        int encodedLength = requestXMLBody.length;

        // Before doing any kind of reporting, let's check if this app is not under any throttling.
        // If so, don't send a hit request unless it is a Wednesday just in case there's a throttle bypass.
//            Date endThrottleDate = (Date) SharedPreferences.getInstance().getObject(v_exceptionContext.m_appBundleID + "_" + kThrottleDays);
//            int dayOfTheWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
//
//            if (endThrottleDate != null && endThrottleDate.compareTo(new Date()) > 0 && dayOfTheWeek != Calendar.WEDNESDAY) {
//                newStage = kWatsonStageDone;
//                _crashLogHasBeenProcessed = true; //We are throttled, so assume Watson doesn't want the crash log.
//            }
//
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Integer> future = executor.submit(new Callable<Integer>() {
            public Integer call() throws Exception {
                try {
                    return handleHTTPRequestNewProtocol("POST", FIELD_URL, requestXMLBody, stageOneResponse, encodedLength); // Call your specific method here
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
            if (m_stageOneCabUploadToken != null && m_stageOneHitReceiptBucketHash != null && m_stageOneHitReceiptBucketTBL != null) {    // If the response contains a send command item (<cmd nm="send">) along with a token and bucket info, it means we should upload a CAB file and move to stage two.

                newStage = WatsonStage.kWatsonStageTwo;
                stage = 2;
//                executeStageTwoNewProtocol(curLogFile, log);
            } else {   // If no token but http result == 200 and the response <cmd nm="receipt"> contains an iBucket, then reporting was successful. We are done.

                newStage = WatsonStage.kWatsonStageDone;
                stage = 3;
            }
        } else if (!stageOneRetried) {
            // If something went wrong, let's give it one more shot.
            newStage = WatsonStage.kWatsonStageRetryStageOne;
            stage = 1;
//            executeStageOneNewProtocol(curLogFile ,log, errorReport);
            stageOneRetried = true;
            //doubt it is written for IOS only
            _crashLogHasBeenProcessed = true; //Marking this as processed regardless of what happens with our retry. At this point we are done.

        }
        return newStage;
    }

    private static void parseStageOneResponseNewProtocol(byte[] response) {
        m_stageOneHitReceiptBucketID = null;
        m_stageOneCabUploadToken = null;
//            try {
//                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//                DocumentBuilder builder = factory.newDocumentBuilder();
//                InputSource is = new InputSource(new ByteArrayInputStream(response));
//                Document doc = builder.parse(is);
//
//                ParserDelegate parserDelegate = new ParserDelegate();
//                if (parserDelegate.isInvalidRequest()) {
//                    throw new AssertionError("Invalid request, we should not retry. Something is formatted incorrectly.");
//                }
//                else {
//                    m_stageOneHitReceiptBucketID = parserDelegate.getBucketID();
//                }
//                if (parserDelegate.isShouldCollectCab() || parserDelegate.isShouldBypassThrottle()) {
//                    m_stageOneCabUploadToken = parserDelegate.getCabUploadToken();
//                    m_stageOneHitReceiptBucketHash = parserDelegate.getBucketHash();
//                    m_stageOneHitReceiptBucketTBL = parserDelegate.getBuckettbl();
//                }

//            if (parserDelegate.isShouldThrottle()) {
//                // Store the date this app should go back to sending hit requests to Watson.
//                Calendar theCalendar = Calendar.getInstance();
//                theCalendar.add(Calendar.DAY_OF_MONTH, parserDelegate.getThrottleDays());
//                Date endThrottleDate = theCalendar.getTime();
//                UserDefaults.standard.set(endThrottleDate, forKey: "\(v_exceptionContext.m_appBundleID)_\(kThrottleDays)")
//            }
//
//            if (parserDelegate.isShouldBypassThrottle()) {
//                // There was a throttle bypass, get rid of the endThrottleDate as a way to resume sending hits to Watson.
//                // Watson can allways throttle us back at any time.
//                UserDefaults.standard.removeObject(forKey: "\(v_exceptionContext.m_appBundleID)_\(kThrottleDays)")
//            }
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
//
//            if (parserDelegate.isShouldThrottle()) {
//                // Store the date this app should go back to sending hit requests to Watson.
//                Calendar theCalendar = Calendar.getInstance();
//                theCalendar.add(Calendar.DAY_OF_MONTH, parserDelegate.getThrottleDays());
//                Date endThrottleDate = theCalendar.getTime();
//                UserDefaults.standard.set(endThrottleDate, "\(v_exceptionContext.m_appBundleID)_\(kThrottleDays)");
////                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
////                sharedPreferences.edit().remove("(v_exceptionContext.m_appBundleID)_(kThrottleDays)").apply();
//            }

//            if (parserDelegate.isShouldBypassThrottle()) {
//                // There was a throttle bypass, get rid of the endThrottleDate as a way to resume sending hits to Watson.
//                // Watson can allways throttle us back at any time.
//                UserDefaults.standard.removeObject(forKey: "\(v_exceptionContext.m_appBundleID)_\(kThrottleDays)")
//            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static WatsonStage executeStageTwoNewProtocol(File curLogFile, ManagedErrorLog log) throws IOException, JSONException {
        byte[] stageTwoResponse = null;
        int httpResult = 0;
        WatsonStage newStage = WatsonStage.kWatsonStageDone;
        boolean shouldRetryCabUpload = false;
        URL reportURL = new URL(FIELD_URL);
//            InputStream reportData = reportURL.openStream();
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
// Append the report data to the end of the requestXMLBody
        requestXMLBody.write(cabFile);
//            httpResult = handleHTTPRequestNewProtocol("PUT", FIELD_URL, requestXMLBody.toByteArray(), stageTwoResponse, encodedLength);
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
            newStage = WatsonStage.kWatsonStageDone;
//            stage = 3;
//#if MS_TARGET_IOS
            _crashLogHasBeenProcessed = true; //The service didn't ask for a retry, safe to delete the crash log now.
//#endif
        } else if (!stageTwoRetried) {
            newStage = WatsonStage.kWatsonStageRetryStageTwo;
//            stage = 2;
            stageTwoRetried = true;
        }

        return newStage;
    }

    private static boolean parseStageTwoResponseNewProtocol(byte[] response) {
        mStageTwoCabID = null;
        mStageTwoCabGUID = null;
        boolean shouldRetryCabUpload = false;
//            ParserDelegate parserDelegate = new ParserDelegate();
//            try
//            {
//                Xml.parse(new ByteArrayInputStream(response), Xml.Encoding.UTF_8, parserDelegate);
//                if (!parserDelegate.isDidCabUploadFailed())
//                {
//                    mStageTwoCabID = parserDelegate.getCabID();
//                    mStageTwoCabGUID = parserDelegate.getCabGUID();
//
//                    if (mStageTwoCabID != null && mStageTwoCabGUID != null)
//                    {
//                        _crashLogHasBeenProcessed = true;
//                    }
//
//                }
//                else
//                {
//                    shouldRetryCabUpload = parserDelegate.isShouldRetryCabUpload();
//                }
//            }
//            catch (Exception e)
//            {
//                e.printStackTrace();
//            }
//
//            return shouldRetryCabUpload;
//        }
//            try {
//                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
//                factory.setNamespaceAware(true);
//                XmlPullParser xpp = factory.newPullParser();
//                xpp.setInput(new ByteArrayInputStream(response), null);
//                int eventType = xpp.getEventType();
//                while (eventType != XmlPullParser.END_DOCUMENT) {
//                    if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("CabUploadResponse")) {
//                        String cabID = xpp.getAttributeValue(null, "CabID");
//                        String cabGUID = xpp.getAttributeValue(null, "CabGUID");
//                        String cabUploadFailed = xpp.getAttributeValue(null, "CabUploadFailed");
//                        if (cabUploadFailed == null || !cabUploadFailed.equalsIgnoreCase("true")) {
//                            mStageTwoCabID = cabID;
//                            mStageTwoCabGUID = cabGUID;
//
//                            if (mStageTwoCabID != null && mStageTwoCabGUID != null) {
//                                _crashLogHasBeenProcessed = true;
//                            }
//
//                        } else {
//                            shouldRetryCabUpload = true;
//                        }
//                    }
//                    eventType = xpp.next();
//                }
//            } catch (IOException | XmlPullParserException e) {
//                e.printStackTrace();
//            }
//            return shouldRetryCabUpload;
//
//
//        }
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            ParserDelegate parserDelegate = new ParserDelegate();
            saxParser.parse(new ByteArrayInputStream(response), parserDelegate);

            if (!parserDelegate.isDidCabUploadFailed()) {
                mStageTwoCabID = parserDelegate.getCabID();
                mStageTwoCabGUID = parserDelegate.getCabGUID();
//#if MS_TARGET_IOS
                if (mStageTwoCabID != null && mStageTwoCabGUID != null) {
                    _crashLogHasBeenProcessed = true;
                }
//#endif
            } else {
                shouldRetryCabUpload = parserDelegate.isShouldRetryCabUpload();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return shouldRetryCabUpload;
    }
    //
//        public void run() {
//            WatsonStage stage = kWatsonStageOne;
//            // Set up m_thread
//            m_threadLock.lock();
//            m_thread = Thread.currentThread();
//            m_threadLock.unlock();
//
//            while (stage != kWatsonStageDone) {
//                m_currentStage = stage;
//                switch (stage) {
//                    case kWatsonStageOne:
//                    case kWatsonStageRetryStageOne:
//                        stage = executeStageOneNewProtocol();
//                        break;
//
//                    case kWatsonStageTwo:
//                    case kWatsonStageRetryStageTwo:
//                        stage = executeStageTwoNewProtocol();
//                        break;
//
//                    default:
//                        stage = kWatsonStageDone;
//                        break;
//                }
//            }
//
//            m_currentStage = kWatsonStageDone;
//        }
}
