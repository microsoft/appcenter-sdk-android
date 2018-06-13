package com.microsoft.appcenter.crashes.ingestion.models.json;

import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.appcenter.ingestion.models.json.AbstractLogFactory;

public class ErrorAttachmentLogFactory extends AbstractLogFactory {

    private static final ErrorAttachmentLogFactory sInstance = new ErrorAttachmentLogFactory();

    private ErrorAttachmentLogFactory() {
    }

    public static ErrorAttachmentLogFactory getInstance() {
        return sInstance;
    }

    @Override
    public ErrorAttachmentLog create() {
        return new ErrorAttachmentLog();
    }
}
