package com.microsoft.android.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * <h3>Description</h3>
 *
 * To avoid external apache library "httpmime" this is a simple implementation for a MultipartEntity.
 * Please note that first all key value pairs have to be written and then at least one file part has to be added.
 * Otherwise the boundaries are not written correctly.
 *
 */
public class SimpleMultipartEntity {

    private final static char[] BOUNDARY_CHARS = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private boolean mIsSetLast;

    private boolean mIsSetFirst;

    private ByteArrayOutputStream mOut;

    private String mBoundary;

    public SimpleMultipartEntity() {
        this.mIsSetFirst = false;
        this.mIsSetLast = false;
        this.mOut = new ByteArrayOutputStream();

        /** Create boundary String */
        final StringBuffer buffer = new StringBuffer();
        final Random rand = new Random();

        for (int i = 0; i < 30; i++) {
            buffer.append(BOUNDARY_CHARS[rand.nextInt(BOUNDARY_CHARS.length)]);
        }
        this.mBoundary = buffer.toString();
    }

    public String getBoundary() {
        return mBoundary;
    }

    public void writeFirstBoundaryIfNeeds() throws IOException {
        if (!mIsSetFirst) {
            mOut.write(("--" + mBoundary + "\r\n").getBytes());
        }
        mIsSetFirst = true;
    }

    public void writeLastBoundaryIfNeeds() {
        if (mIsSetLast) {
            return;
        }
        try {
            mOut.write(("\r\n--" + mBoundary + "--\r\n").getBytes());

        } catch (final IOException e) {
            e.printStackTrace();
        }
        mIsSetLast = true;
    }

    public void addPart(final String key, final String value) throws IOException {
        writeFirstBoundaryIfNeeds();

        mOut.write(("Content-Disposition: form-data; name=\"" + key + "\"\r\n").getBytes());
        mOut.write("Content-Type: text/plain; charset=UTF-8\r\n".getBytes());
        mOut.write("Content-Transfer-Encoding: 8bit\r\n\r\n".getBytes());
        mOut.write(value.getBytes());
        mOut.write(("\r\n--" + mBoundary + "\r\n").getBytes());
    }

    public void addPart(final String key, final File value, boolean lastFile) throws IOException {
        addPart(key, value.getName(), new FileInputStream(value), lastFile);
    }

    public void addPart(final String key, final String fileName, final InputStream fin, boolean lastFile) throws IOException {
        addPart(key, fileName, fin, "application/octet-stream", lastFile);
    }

    public void addPart(final String key, final String fileName, final InputStream fin, String type, boolean lastFile) throws IOException {
        writeFirstBoundaryIfNeeds();
        try {
            type = "Content-Type: " + type + "\r\n";
            mOut.write(("Content-Disposition: form-data; name=\"" + key + "\"; filename=\"" + fileName + "\"\r\n").getBytes());
            mOut.write(type.getBytes());
            mOut.write("Content-Transfer-Encoding: binary\r\n\r\n".getBytes());

            final byte[] tmp = new byte[4096];
            int l = 0;
            while ((l = fin.read(tmp)) != -1) {
                mOut.write(tmp, 0, l);
            }
            mOut.flush();

            if (lastFile) {
                /** This is the last file: write last boundary. */
                writeLastBoundaryIfNeeds();

            } else {
                /** Another file will follow: write normal boundary. */
                mOut.write(("\r\n--" + mBoundary + "\r\n").getBytes());
            }

        } finally {
            try {
                fin.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    public long getContentLength() {
        writeLastBoundaryIfNeeds();
        return mOut.toByteArray().length;
    }

    public String getContentType() {
        return "multipart/form-data; boundary=" + getBoundary();
    }

    public ByteArrayOutputStream getOutputStream() {
        writeLastBoundaryIfNeeds();
        return mOut;
    }

}
