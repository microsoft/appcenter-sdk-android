/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.activities.storage.TestDocument;
import com.microsoft.appcenter.storage.Constants;
import com.microsoft.appcenter.storage.Storage;
import com.microsoft.appcenter.storage.Utils;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static android.view.View.GONE;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_ID;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_PARTITION;

public class DocumentDetailActivity extends AppCompatActivity {

    private static final int MAX_CONTENT_LENGTH = 50;

    private String mDocumentId;

    private String mDocumentPartition;

    private String mFullDocContents;

    private String mFullErrorContents;

    private ProgressBar mDetailProgress;

    private ListView mListView;

    private AppCenterConsumer<Document<TestDocument>> getAppDocument = new AppCenterConsumer<Document<TestDocument>>() {

        @Override
        public void accept(Document<TestDocument> document) {
            if (document.hasFailed()) {
                Toast.makeText(DocumentDetailActivity.this, String.format(getResources().getString(R.string.get_document_failed), mDocumentId), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(DocumentDetailActivity.this, String.format(getResources().getString(R.string.get_document_success), mDocumentId), Toast.LENGTH_SHORT).show();
            }
            fillInfo(document);
        }
    };

    private AppCenterConsumer<Document<Map>> getUserDocument = new AppCenterConsumer<Document<Map>>() {

        @Override
        public void accept(Document<Map> document) {
            if (document.hasFailed()) {
                Toast.makeText(DocumentDetailActivity.this, String.format(getResources().getString(R.string.get_document_failed), mDocumentId), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(DocumentDetailActivity.this, String.format(getResources().getString(R.string.get_document_success), mDocumentId), Toast.LENGTH_SHORT).show();
            }
            fillInfo(document);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_document_detail);
        Intent intent = getIntent();
        mDetailProgress = findViewById(R.id.detail_progress);
        mListView = findViewById(R.id.document_info_list_view);
        mDetailProgress.setVisibility(View.VISIBLE);
        mDocumentPartition = intent.getStringExtra(DOCUMENT_PARTITION);
        mDocumentId = intent.getStringExtra(DOCUMENT_ID);
        if (mDocumentPartition.equals(Constants.USER)) {
            Storage.read(mDocumentPartition, mDocumentId, Map.class).thenAccept(getUserDocument);
        } else {
            Storage.read(mDocumentPartition, mDocumentId, TestDocument.class).thenAccept(getAppDocument);
        }
    }

    private void fillInfo(Document document) {
        mDetailProgress.setVisibility(GONE);
        mListView.setVisibility(View.VISIBLE);
        final List<DocumentInfoDisplayModel> list = getDocumentInfoDisplayModelList(document);
        ArrayAdapter<DocumentInfoDisplayModel> adapter = new ArrayAdapter<DocumentInfoDisplayModel>(this, R.layout.info_list_item, R.id.info_title, list) {

            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView titleView = view.findViewById(R.id.info_title);
                final TextView valueView = view.findViewById(R.id.info_content);
                titleView.setText(list.get(position).mTitle);
                valueView.setText(list.get(position).mValue);
                if (list.get(position).mTitle.equals(getString(R.string.document_info_content_title))) {
                    view.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {
                            valueView.setText(mFullDocContents);
                        }
                    });
                }
                if (list.get(position).mTitle.equals(getString(R.string.document_info_error_title))) {
                    view.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {
                            valueView.setText(mFullErrorContents);
                        }
                    });
                }
                return view;
            }
        };
        mListView.setAdapter(adapter);
    }

    private List<DocumentInfoDisplayModel> getDocumentInfoDisplayModelList(Document document) {
        List<DocumentInfoDisplayModel> list = new ArrayList<>();
        list.add(new DocumentInfoDisplayModel(getString(R.string.document_info_id_title), mDocumentId));
        list.add(new DocumentInfoDisplayModel(getString(R.string.document_info_partition_title), mDocumentPartition));
        if (document.getDocumentError() != null) {
            String message = document.getDocumentError().getError().getMessage();
            mFullErrorContents = message;
            if (message.length() > MAX_CONTENT_LENGTH) {
                message = message.substring(0, MAX_CONTENT_LENGTH) + "...";
            }
            list.add(new DocumentInfoDisplayModel(getString(R.string.document_info_error_title), message));
            return list;
        }
        list.add(new DocumentInfoDisplayModel(getString(R.string.document_info_date_title), new Date(document.getTimestamp()).toString()));
        list.add(new DocumentInfoDisplayModel(getString(R.string.document_info_state_title), document.isFromCache() ? getString(R.string.document_info_cached_state) : getString(R.string.document_info_remote_state)));
        Object doc = document.getDocument();
        String docContents = doc == null ? "{}" : Utils.getGson().toJson(doc);
        try {
            JSONObject docContentsJSON = new JSONObject(docContents);
            mFullDocContents = docContentsJSON.toString(4);
        } catch (JSONException e) {
            mFullDocContents = docContents;
        }
        if (docContents.length() > MAX_CONTENT_LENGTH) {
            docContents = docContents.substring(0, MAX_CONTENT_LENGTH) + "...";
        }
        list.add(new DocumentInfoDisplayModel(getString(R.string.document_info_content_title), docContents));
        return list;
    }

    @VisibleForTesting
    class DocumentInfoDisplayModel {

        String mTitle;

        String mValue;

        DocumentInfoDisplayModel(String title, String value) {
            mTitle = title;
            mValue = value;
        }
    }
}
