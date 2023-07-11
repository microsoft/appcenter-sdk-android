package com.microsoft.appcenter.crashes;

import org.xml.sax.helpers.DefaultHandler;

public class ParserDelegate extends DefaultHandler {
    private String cabUploadToken;
    private String cabUploadDestination;
    private String buckettbl;
    private String bucketHash;
    private String bucketID;
    private String cabID;
    private String cabGUID;
    private boolean shouldRetryCabUpload;
    private boolean didCabUploadFailed;
    private boolean shouldCollectCab;
    private boolean isInvalidRequest;
    private boolean shouldThrottle;
    private boolean shouldBypassThrottle;
    private int throttleDays;

    public String getCabUploadToken() {
        return cabUploadToken;
    }

    public void setCabUploadToken(String cabUploadToken) {
        this.cabUploadToken = cabUploadToken;
    }

    public String getCabUploadDestination() {
        return cabUploadDestination;
    }

    public void setCabUploadDestination(String cabUploadDestination) {
        this.cabUploadDestination = cabUploadDestination;
    }

    public String getBuckettbl() {
        return buckettbl;
    }

    public void setBuckettbl(String buckettbl) {
        this.buckettbl = buckettbl;
    }

    public String getBucketHash() {
        return bucketHash;
    }

    public void setBucketHash(String bucketHash) {
        this.bucketHash = bucketHash;
    }

    public String getBucketID() {
        return bucketID;
    }

    public void setBucketID(String bucketID) {
        this.bucketID = bucketID;
    }

    public String getCabID() {
        return cabID;
    }

    public void setCabID(String cabID) {
        this.cabID = cabID;
    }

    public String getCabGUID() {
        return cabGUID;
    }

    public void setCabGUID(String cabGUID) {
        this.cabGUID = cabGUID;
    }

    public boolean isShouldRetryCabUpload() {
        return shouldRetryCabUpload;
    }

    public void setShouldRetryCabUpload(boolean shouldRetryCabUpload) {
        this.shouldRetryCabUpload = shouldRetryCabUpload;
    }

    public boolean isDidCabUploadFailed() {
        return didCabUploadFailed;
    }

    public void setDidCabUploadFailed(boolean didCabUploadFailed) {
        this.didCabUploadFailed = didCabUploadFailed;
    }

    public boolean isShouldCollectCab() {
        return shouldCollectCab;
    }

    public void setShouldCollectCab(boolean shouldCollectCab) {
        this.shouldCollectCab = shouldCollectCab;
    }

    public boolean isInvalidRequest() {
        return isInvalidRequest;
    }

    public void setInvalidRequest(boolean invalidRequest) {
        isInvalidRequest = invalidRequest;
    }

    public boolean isShouldThrottle() {
        return shouldThrottle;
    }

    public void setShouldThrottle(boolean shouldThrottle) {
        this.shouldThrottle = shouldThrottle;
    }

    public boolean isShouldBypassThrottle() {
        return shouldBypassThrottle;
    }

    public void setShouldBypassThrottle(boolean shouldBypassThrottle) {
        this.shouldBypassThrottle = shouldBypassThrottle;
    }

    public int getThrottleDays() {
        return throttleDays;
    }

    public void setThrottleDays(int throttleDays) {
        this.throttleDays = throttleDays;
    }
}


