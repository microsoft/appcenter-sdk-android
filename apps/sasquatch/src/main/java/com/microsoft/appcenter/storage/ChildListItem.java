package com.microsoft.appcenter.storage;

import com.microsoft.appcenter.storage.models.Document;

public class ChildListItem implements ListItem {

    private Document<TestDocument> mChild;

    public void setChild(Document<TestDocument> child) {
        this.mChild = child;
    }

    @Override
    public boolean isHeader() {
        return false;
    }

    @Override
    public String getName() {
        return mChild.getId();
    }

    @Override
    public String getItem() {
        TestDocument document = mChild.getDocument();
        return document == null ? "{}" : Utils.getGson().toJson(document);
    }
}
