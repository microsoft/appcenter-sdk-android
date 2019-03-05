package com.microsoft.appcenter.storage;

import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.ReadOptions;
import com.microsoft.appcenter.storage.models.WriteOptions;
import com.microsoft.appcenter.utils.storage.FileManager;

import java.io.File;
import java.io.IOException;

public class DocumentCache {
    private static final String filePathFormat = "%s_%s";

    public static <T> void write(Document<T> document, WriteOptions writeOptions) {
        try {
            FileManager.write(getDocumentFile(document), Utils.getGson().toJson(document));
        } catch (IOException e) {
            // TODO: process the exception
        }
    }

    public static <T> Document<T> read(String partition, String documentId, Class<T> documentType, ReadOptions readOptions) {
        File documentFile = getDocumentFile(partition, documentId);
        if (!documentFile.exists()) {
            return null;
        }
        if (readOptions.isExpired(documentFile.lastModified())) {
            documentFile.delete();
            return null;
        }
        Document<T> cachedDocument = Utils.parseDocument(FileManager.read(documentFile), documentType);
        cachedDocument.setIsFromCache(true);
        return cachedDocument;
    }

    public static void delete(String partition, String documentId) {
        File documentFile = getDocumentFile(partition, documentId);
        if (documentFile.exists()) {
            documentFile.delete();
        }
    }

    private static <T> File getDocumentFile(Document<T> document) {
        return getDocumentFile(document.getPartition(), document.getId());
    }

    private static <T> File getDocumentFile(String partition, String documentId) {
        return new File(String.format(filePathFormat, partition, documentId));
    }
}
