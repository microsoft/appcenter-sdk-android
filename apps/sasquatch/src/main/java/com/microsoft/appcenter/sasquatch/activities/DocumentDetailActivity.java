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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.appcenter.data.Data;
import com.microsoft.appcenter.data.DefaultPartitions;
import com.microsoft.appcenter.data.Utils;
import com.microsoft.appcenter.data.models.DocumentWrapper;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.view.View.GONE;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_CONTENT;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_DATE;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_ERROR;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_ERROR_NULL_STATUS;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_ID;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_PARTITION;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_STATE;

public class DocumentDetailActivity extends AppCompatActivity {

    private static final int MAX_CONTENT_LENGTH = 50;

    private String mDocumentId;

    private String mDocumentPartition;

    private String mFullDocContents;

    private String mFullErrorContents;

    private String mDocumentContent;

    private String mDocumentDate;

    private boolean mDocumentState;

    private String mDocumentError;

    private boolean mDocumentNullStatus;

    private ProgressBar mDetailProgress;

    private ListView mListView;

    private final AppCenterConsumer<DocumentWrapper<Map>> getAppDocument = new AppCenterConsumer<DocumentWrapper<Map>>() {

        @Override
        public void accept(DocumentWrapper<Map> document) {
            if (document.hasFailed()) {
                Toast.makeText(DocumentDetailActivity.this, String.format(getResources().getString(R.string.get_document_failed), mDocumentId), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(DocumentDetailActivity.this, String.format(getResources().getString(R.string.get_document_success), mDocumentId), Toast.LENGTH_SHORT).show();
            }
            refreshDocumentWithRead(document);
        }
    };

    private final AppCenterConsumer<DocumentWrapper<Map>> getUserDocument = new AppCenterConsumer<DocumentWrapper<Map>>() {

        @Override
        public void accept(DocumentWrapper<Map> document) {
            if (document.hasFailed()) {
                Toast.makeText(DocumentDetailActivity.this, String.format(getResources().getString(R.string.get_document_failed), mDocumentId), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(DocumentDetailActivity.this, String.format(getResources().getString(R.string.get_document_success), mDocumentId), Toast.LENGTH_SHORT).show();
            }
            refreshDocumentWithRead(document);
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
        mDocumentContent = intent.getStringExtra(DOCUMENT_CONTENT);
        mDocumentDate = intent.getStringExtra(DOCUMENT_DATE);
        mDocumentState = intent.getBooleanExtra(DOCUMENT_STATE, false);
        mDocumentError = intent.getStringExtra(DOCUMENT_ERROR);
        mDocumentNullStatus = intent.getBooleanExtra(DOCUMENT_ERROR_NULL_STATUS, false);
        fillInfo(mDocumentNullStatus, mDocumentError, mDocumentDate, mDocumentState, mDocumentContent);
    }

    private void fillInfo(boolean documentErrorNullStatus, String errorMessage, String date, boolean documentState, String docContents) {
        mDetailProgress.setVisibility(GONE);
        mListView.setVisibility(View.VISIBLE);
        final List<DocumentInfoDisplayModel> list = getDocumentInfoDisplayModelList(documentErrorNullStatus, errorMessage, date, documentState, docContents);
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

    private List<DocumentInfoDisplayModel> getDocumentInfoDisplayModelList(boolean documentErrorNullStatus, String errorMessage, String date, boolean documentState, String docContents) {
        List<DocumentInfoDisplayModel> list = new ArrayList<>();
        list.add(new DocumentInfoDisplayModel(getString(R.string.document_info_id_title), mDocumentId));
        list.add(new DocumentInfoDisplayModel(getString(R.string.document_info_partition_title), mDocumentPartition));
        if (documentErrorNullStatus) {
            mFullErrorContents = errorMessage;
            list.add(new DocumentInfoDisplayModel(getString(R.string.document_info_error_title), errorMessage));
            return list;
        }
        list.add(new DocumentInfoDisplayModel(getString(R.string.document_info_date_title), date));
        list.add(new DocumentInfoDisplayModel(getString(R.string.document_info_state_title), documentState ? getString(R.string.document_info_cached_state) : getString(R.string.document_info_remote_state)));
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

    private void refreshDocumentWithRead(DocumentWrapper document) {
        if (document.getError() != null) {
            String message = document.getError().getMessage();
            if (message.length() > MAX_CONTENT_LENGTH) {
                message = message.substring(0, MAX_CONTENT_LENGTH) + "...";
                mDocumentError = message;
                mDocumentNullStatus = true;
            }
        } else {
            Object doc = document.getDeserializedValue();
            mDocumentContent = doc == null ? "{}" : Utils.getGson().toJson(doc);
            mDocumentNullStatus = false;
        }
        if (document.getLastUpdatedDate() != null) {
            mDocumentDate = document.getLastUpdatedDate().toString();
        }
        mDocumentState = document.isFromDeviceCache();
        fillInfo(mDocumentNullStatus, mDocumentError, mDocumentDate, mDocumentState, mDocumentContent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            if (mDocumentPartition.equals(DefaultPartitions.USER_DOCUMENTS)) {
                Data.read(mDocumentId, Map.class, mDocumentPartition).thenAccept(getUserDocument);
            } else {
                Data.read(mDocumentId, Map.class, mDocumentPartition).thenAccept(getAppDocument);
            }
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.refresh, menu);
        return true;
    }

    @VisibleForTesting
    static class DocumentInfoDisplayModel {

        final String mTitle;

        final String mValue;

        DocumentInfoDisplayModel(String title, String value) {
            mTitle = title;
            mValue = value;
        }
    }
}
