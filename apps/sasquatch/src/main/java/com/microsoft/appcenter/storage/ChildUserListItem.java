package com.microsoft.appcenter.storage;

public class ChildUserListItem implements ListItem {

    private String mChild;

    public void setChild(String child) {
        this.mChild = child;
    }

    @Override
    public boolean isHeader() {
        return false;
    }

    @Override
    public String getName() {
        return mChild.substring(2);
    }

    @Override
    public String getItem() {
        return mChild;
    }
}
