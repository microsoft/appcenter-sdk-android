package com.microsoft.azure.mobile.sasquatch.utils;

import android.support.v7.app.AppCompatActivity;

import com.microsoft.azure.mobile.crashes.CrashesListener;
import com.microsoft.azure.mobile.crashes.ErrorAttachments;
import com.microsoft.azure.mobile.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.azure.mobile.crashes.model.ErrorReport;

import java.util.Arrays;

/**
 * CrashesListener provider for projectDependency flavour.
 */
public class CrashesListenerProvider {
    public static CrashesListener provideCrashesListener(AppCompatActivity activity) {
        return new SasquatchCrashesListener(activity) {
            @Override
            public Iterable<ErrorAttachmentLog> getErrorAttachments(ErrorReport report) {
                ErrorAttachmentLog textLog = ErrorAttachments.attachmentWithText("This is a text attachment.", "text.txt");
                ErrorAttachmentLog binaryLog = ErrorAttachments.attachmentWithBinary("This is a binary attachment.".getBytes(), "binary.jpeg", "image/jpeg");
                return Arrays.asList(textLog, binaryLog);
            }
        };
    }
}