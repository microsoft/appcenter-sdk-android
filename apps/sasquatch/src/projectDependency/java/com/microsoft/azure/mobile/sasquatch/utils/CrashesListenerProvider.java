package com.microsoft.azure.mobile.sasquatch.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;

import com.microsoft.azure.mobile.crashes.CrashesListener;
import com.microsoft.azure.mobile.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.azure.mobile.crashes.model.ErrorReport;
import com.microsoft.azure.mobile.sasquatch.R;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * CrashesListener provider for projectDependency flavour.
 */
public class CrashesListenerProvider {
    public static CrashesListener provideCrashesListener(final AppCompatActivity activity) {

        return new SasquatchCrashesListener(activity) {

            @Override
            public Iterable<ErrorAttachmentLog> getErrorAttachments(ErrorReport report) {

                /* Attach some text. */
                ErrorAttachmentLog textLog = ErrorAttachmentLog.attachmentWithText("This is a text attachment.", "text.txt");

                /* Attach app icon to test binary. */
                Bitmap bitmap = BitmapFactory.decodeResource(activity.getResources(), R.mipmap.ic_launcher);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] bitMapData = stream.toByteArray();
                ErrorAttachmentLog binaryLog = ErrorAttachmentLog.attachmentWithBinary(bitMapData, "icon.jpeg");

                /* Return attachments as list. */
                return Arrays.asList(textLog, binaryLog);
            }
        };
    }
}