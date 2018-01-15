package com.microsoft.appcenter.codepush.utils;

public class DownloadProgress {

    /**
     * Total size of the package in bytes.
     */
    private long totalBytes;

    /**
     * The amount of bytes received from the package.
     */
    private long receivedBytes;

    /**
     * Creates new instance of the progress.
     *
     * @param totalBytes    total size of the package.
     * @param receivedBytes the amount of bytes received from the package.
     * @return instance of the {@link DownloadProgress}.
     */
    public static DownloadProgress newProgress(long totalBytes, long receivedBytes) {
        DownloadProgress downloadProgress = new DownloadProgress();
        downloadProgress.setReceivedBytes(receivedBytes);
        downloadProgress.setTotalBytes(totalBytes);
        return downloadProgress;
    }

    /**
     * Gets the total size of the package in bytes.
     *
     * @return total size of the package in bytes.
     */
    public long getTotalBytes() {
        return totalBytes;
    }

    /**
     * Gets the amount of bytes received from the package.
     *
     * @return the amount of bytes received from the package.
     */
    public long getReceivedBytes() {
        return receivedBytes;
    }

    /**
     * Checks whether the download is completed.
     *
     * @return whether the download is completed.
     */
    public boolean isCompleted() {
        return totalBytes == receivedBytes;
    }

    /**
     * Sets the total size of the package in bytes.
     *
     * @param totalBytes total size of the package in bytes.
     */
    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    /**
     * Sets the amount of bytes received from the package.
     *
     * @param receivedBytes the amount of bytes received from the package.
     */
    public void setReceivedBytes(long receivedBytes) {
        this.receivedBytes = receivedBytes;
    }
}
