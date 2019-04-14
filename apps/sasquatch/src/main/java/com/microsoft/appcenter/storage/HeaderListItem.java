package com.microsoft.appcenter.storage;

public class HeaderListItem implements ListItem {

    private String mHeader;

    @Override
    public boolean isHeader() {
        return true;
    }

    public void setHeader(String header) {
        this.mHeader = header;
    }

    @Override
    public String getName() {
        return mHeader;
    }

    @Override
    public String getItem() {
        return mHeader;
    }
}