package avalanche.core.utils;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import avalanche.core.Constants;

/**
 * <h3>Description</h3>
 *
 * Builder class for HttpURLConnection.
 *
 **/
public class HttpURLConnectionBuilder {

    public static final String DEFAULT_CHARSET = "UTF-8";
    private static final int DEFAULT_TIMEOUT = 2 * 60 * 1000;
    private final String mUrlString;
    private final Map<String, String> mHeaders;
    private String mRequestMethod;
    private String mRequestBody;
    private SimpleMultipartEntity mMultipartEntity;
    private int mTimeout = DEFAULT_TIMEOUT;

    public HttpURLConnectionBuilder(String urlString) {
        mUrlString = urlString;
        mHeaders = new HashMap<String, String>();
        mHeaders.put("User-Agent", Constants.SDK_USER_AGENT);
    }

    private static String getFormString(Map<String, String> params, String charset) throws UnsupportedEncodingException {
        List<String> protoList = new ArrayList<String>();
        for (String key : params.keySet()) {
            String value = params.get(key);
            key = URLEncoder.encode(key, charset);
            value = URLEncoder.encode(value, charset);
            protoList.add(key + "=" + value);
        }
        return TextUtils.join("&", protoList);
    }

    public HttpURLConnectionBuilder setRequestMethod(String requestMethod) {
        mRequestMethod = requestMethod;
        return this;
    }

    public HttpURLConnectionBuilder setRequestBody(String requestBody) {
        mRequestBody = requestBody;
        return this;
    }

    public HttpURLConnectionBuilder writeFormFields(Map<String, String> fields) {
        try {
            String formString = getFormString(fields, DEFAULT_CHARSET);
            setHeader("Content-Type", "application/x-www-form-urlencoded");
            setRequestBody(formString);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public HttpURLConnectionBuilder writeMultipartData(Map<String, String> fields, Context context, List<Uri> attachmentUris) {
        try {
            mMultipartEntity = new SimpleMultipartEntity();
            mMultipartEntity.writeFirstBoundaryIfNeeds();

            for (String key : fields.keySet()) {
                mMultipartEntity.addPart(key, fields.get(key));
            }

            for (int i = 0; i < attachmentUris.size(); i++) {
                Uri attachmentUri = attachmentUris.get(i);
                boolean lastFile = (i == attachmentUris.size() - 1);

                InputStream input = context.getContentResolver().openInputStream(attachmentUri);
                String filename = attachmentUri.getLastPathSegment();
                mMultipartEntity.addPart("attachment" + i, filename, input, lastFile);
            }
            mMultipartEntity.writeLastBoundaryIfNeeds();

            setHeader("Content-Type", "multipart/form-data; boundary=" + mMultipartEntity.getBoundary());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    public HttpURLConnectionBuilder setTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("Timeout has to be positive.");
        }
        mTimeout = timeout;
        return this;
    }

    public HttpURLConnectionBuilder setHeader(String name, String value) {
        mHeaders.put(name, value);
        return this;
    }

    public HttpURLConnectionBuilder setBasicAuthorization(String username, String password) {
        String authString = "Basic " + avalanche.core.utils.Base64.encodeToString(
                (username + ":" + password).getBytes(), android.util.Base64.NO_WRAP);

        setHeader("Authorization", authString);
        return this;
    }

    public HttpURLConnection build() throws IOException {
        HttpURLConnection connection;
        URL url = new URL(mUrlString);
        connection = (HttpURLConnection) url.openConnection();

        connection.setConnectTimeout(mTimeout);
        connection.setReadTimeout(mTimeout);

        if (!TextUtils.isEmpty(mRequestMethod)) {
            connection.setRequestMethod(mRequestMethod);
            if (!TextUtils.isEmpty(mRequestBody) || mRequestMethod.equalsIgnoreCase("POST") || mRequestMethod.equalsIgnoreCase("PUT")) {
                connection.setDoOutput(true);
            }
        }

        for (String name : mHeaders.keySet()) {
            connection.setRequestProperty(name, mHeaders.get(name));
        }

        if (!TextUtils.isEmpty(mRequestBody)) {
            OutputStream outputStream = connection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, DEFAULT_CHARSET));
            writer.write(mRequestBody);
            writer.flush();
            writer.close();
        }

        if (mMultipartEntity != null) {
            connection.setRequestProperty("Content-Length", String.valueOf(mMultipartEntity.getContentLength()));
            BufferedOutputStream outputStream = new BufferedOutputStream(connection.getOutputStream());
            outputStream.write(mMultipartEntity.getOutputStream().toByteArray());
            outputStream.flush();
            outputStream.close();
        }

        return connection;
    }

}
