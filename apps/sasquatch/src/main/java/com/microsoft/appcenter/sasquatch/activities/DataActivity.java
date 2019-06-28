/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.appcenter.data.Data;
import com.microsoft.appcenter.data.DefaultPartitions;
import com.microsoft.appcenter.data.Utils;
import com.microsoft.appcenter.data.models.DocumentWrapper;
import com.microsoft.appcenter.data.models.Page;
import com.microsoft.appcenter.data.models.PaginatedDocuments;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.activities.data.AppDocumentListAdapter;
import com.microsoft.appcenter.sasquatch.activities.data.CustomItemAdapter;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.microsoft.appcenter.sasquatch.SasquatchConstants.ACCOUNT_ID;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_CONTENT;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_DATE;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_ERROR;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_ERROR_NULL_STATUS;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_ID;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_PARTITION;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_STATE;

public class DataActivity extends AppCompatActivity {

    private static final int MAX_CONTENT_LENGTH = 50;

    private RecyclerView mListView;

    private boolean mLoading;

    private MenuItem mAddNewDocument;

    private CustomItemAdapter mAdapterUser;

    private AppDocumentListAdapter mAppDocumentListAdapter;

    private ProgressBar mProgressBar;

    private Spinner mDocumentTypeSpinner;

    private DocumentType mDocumentType = DocumentType.READONLY;

    private PaginatedDocuments<Map> mCurrentAppDocuments;

    private PaginatedDocuments<Map> mCurrentUserDocuments;

    private TextView mMessageText;

    private boolean mUserDocumentsLoading;

    private boolean mAppDocumentsLoading;

    private final AppCenterConsumer<PaginatedDocuments<Map>> mUploadApp = new AppCenterConsumer<PaginatedDocuments<Map>>() {

        @Override
        public void accept(PaginatedDocuments<Map> documents) {
            mAppDocumentsLoading = false;
            if (!mUserDocumentsLoading) {
                hideProgress();
            }
            mCurrentAppDocuments = documents;
            if (documents != null && documents.getCurrentPage() != null) {
                updateAppDocument(documents.getCurrentPage().getItems());
            }
        }
    };

    private final AppCenterConsumer<PaginatedDocuments<Map>> mUploadUser = new AppCenterConsumer<PaginatedDocuments<Map>>() {

        @Override
        public void accept(PaginatedDocuments<Map> documents) {
            mUserDocumentsLoading = false;
            if (!mAppDocumentsLoading) {
                hideProgress();
            }
            mCurrentUserDocuments = documents;
            if (documents != null && documents.getCurrentPage() != null) {
                updateUserDocuments(documents.getCurrentPage().getItems());
            }
        }
    };

    private void hideProgress() {
        mProgressBar.setVisibility(View.GONE);
        mDocumentTypeSpinner.setEnabled(true);
    }

    private void showProgress() {
        mProgressBar.setVisibility(View.VISIBLE);
        mDocumentTypeSpinner.setEnabled(false);
    }

    private final RecyclerView.OnScrollListener mScrollAppListener = new RecyclerView.OnScrollListener() {

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (mCurrentAppDocuments != null && mCurrentAppDocuments.hasNextPage() && !mLoading) {
                mLoading = true;
                mCurrentAppDocuments.getNextPage().thenAccept(new AppCenterConsumer<Page<Map>>() {

                    @Override
                    public void accept(Page<Map> testDocumentPage) {
                        mLoading = false;
                        updateAppDocument(testDocumentPage.getItems());
                    }
                });
            }
        }
    };

    private final RecyclerView.OnScrollListener mScrollUserListener = new RecyclerView.OnScrollListener() {

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (mCurrentUserDocuments != null && mCurrentUserDocuments.hasNextPage() && !mLoading) {
                mLoading = true;
                mCurrentUserDocuments.getNextPage().thenAccept(new AppCenterConsumer<Page<Map>>() {

                    @Override
                    public void accept(Page<Map> mapPage) {
                        updateUserDocuments(mapPage.getItems());
                    }
                });
            }
        }
    };

    private void updateUserDocuments(List<DocumentWrapper<Map>> documents) {
        if (documents == null)
            return;
        mAdapterUser.setList(new ArrayList<>(documents));
        mAdapterUser.notifyDataSetChanged();
    }

    private void updateAppDocument(List<DocumentWrapper<Map>> list) {
        mAppDocumentListAdapter.upload(list);
        mAppDocumentListAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);
        mListView = findViewById(R.id.list);
        mListView.setLayoutManager(new LinearLayoutManager(this));
        mProgressBar = findViewById(R.id.load_progress);
        mDocumentTypeSpinner = findViewById(R.id.data_type);
        mMessageText = findViewById(R.id.data_message);

        /* List the app read-only documents. */
        mAppDocumentListAdapter = new AppDocumentListAdapter(this, new ArrayList<DocumentWrapper<Map>>());
        mAppDocumentListAdapter.setOnItemClickListener(new AppDocumentListAdapter.OnItemClickListener() {

            @Override
            public void onItemClick(int position) {
                Intent intent = new Intent(DataActivity.this, DocumentDetailActivity.class);
                DocumentWrapper<Map> document = mAppDocumentListAdapter.getDocument(position);
                fillIntentWithDocDetails(intent, document, DefaultPartitions.APP_DOCUMENTS);
                startActivity(intent);
            }
        });

        /* List the user documents. */
        mAdapterUser = new CustomItemAdapter(new ArrayList<DocumentWrapper<Map>>(), this);
        mAdapterUser.setOnItemClickListener(new CustomItemAdapter.OnItemClickListener() {

            @Override
            public void onItemClick(int position) {
                Intent intent = new Intent(DataActivity.this, DocumentDetailActivity.class);
                DocumentWrapper<Map> document = mAdapterUser.getDocument(position);
                fillIntentWithDocDetails(intent, document, DefaultPartitions.USER_DOCUMENTS);
                startActivity(intent);
            }

            @Override
            public void onRemoveClick(final int position) {
                Data.delete(mAdapterUser.getItem(position), DefaultPartitions.USER_DOCUMENTS).thenAccept(new AppCenterConsumer<DocumentWrapper<Void>>() {

                    @Override
                    public void accept(DocumentWrapper<Void> voidDocument) {
                        if (voidDocument.hasFailed()) {
                            Toast.makeText(DataActivity.this, R.string.data_file_remove_error, Toast.LENGTH_SHORT).show();
                        } else {
                            mAdapterUser.removeItem(position);
                            mAdapterUser.notifyDataSetChanged();
                            Toast.makeText(DataActivity.this, R.string.data_file_remove_success, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        /* Selector for App VS User documents. */
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.document_type_names));
        mDocumentTypeSpinner.setAdapter(typeAdapter);
        mDocumentTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateStorageType(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void fillIntentWithDocDetails(Intent intent, DocumentWrapper document, String partition) {
        intent.putExtra(DOCUMENT_PARTITION, partition);
        intent.putExtra(DOCUMENT_ID, document.getId());
        if (document.getError() != null) {
            String message = document.getError().getMessage();
            if (message.length() > MAX_CONTENT_LENGTH) {
                message = message.substring(0, MAX_CONTENT_LENGTH) + "...";
                intent.putExtra(DOCUMENT_ERROR, message);
                intent.putExtra(DOCUMENT_ERROR_NULL_STATUS, true);
            }
        } else {
            Object doc = document.getDeserializedValue();
            String docContents = doc == null ? "{}" : Utils.getGson().toJson(doc);
            intent.putExtra(DOCUMENT_CONTENT, docContents);
            intent.putExtra(DOCUMENT_ERROR_NULL_STATUS, false);
        }
        if (document.getLastUpdatedDate() != null) {
            intent.putExtra(DOCUMENT_DATE, document.getLastUpdatedDate().toString());
        }
        intent.putExtra(DOCUMENT_STATE, document.isFromDeviceCache());
    }

    private void loadAppDocuments() {

        /* List readonly documents. */
        mAppDocumentsLoading = true;
        showProgress();
        Data.list(Map.class, DefaultPartitions.APP_DOCUMENTS).thenAccept(mUploadApp);
    }

    private void loadUserDocuments() {

        /* List the user documents. */
        String accountId = MainActivity.sSharedPreferences.getString(ACCOUNT_ID, null);
        if (accountId != null) {
            mUserDocumentsLoading = true;
            showProgress();
            Data.list(Map.class, DefaultPartitions.USER_DOCUMENTS).thenAccept(mUploadUser);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mLoading = false;
        if (item.getItemId() == R.id.action_add) {
            switch (mDocumentType) {
                case USER:
                    String accountId = MainActivity.sSharedPreferences.getString(ACCOUNT_ID, null);
                    if (accountId != null) {
                        Intent intent = new Intent(DataActivity.this, NewUserDocumentActivity.class);
                        startActivity(intent);
                    }
                    break;

                case READONLY:
                    final AlertDialog.Builder builder = new AlertDialog.Builder(DataActivity.this);
                    builder.setIcon(R.drawable.ic_appcenter_logo);
                    builder.setTitle(getApplicationContext().getResources().getString(R.string.document_type_reminder));
                    builder.setPositiveButton(getApplicationContext().getResources().getString(R.string.alert_ok), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            builder.setCancelable(true);
                        }
                    });
                    builder.show();
                    break;
            }
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add, menu);
        mAddNewDocument = menu.findItem(R.id.action_add);
        return true;
    }

    private void updateStorageType(int position) {
        mMessageText.setText("");
        mDocumentType = DocumentType.values()[position];
        switch (mDocumentType) {
            case READONLY:
                mAddNewDocument.setVisible(false);
                mListView.setAdapter(mAppDocumentListAdapter);
                mListView.addOnScrollListener(mScrollAppListener);
                break;
            case USER:
                mAddNewDocument.setVisible(true);
                mListView.removeOnScrollListener(mScrollUserListener);
                String accountId = MainActivity.sSharedPreferences.getString(ACCOUNT_ID, null);
                if (accountId != null) {
                    mListView.setAdapter(mAdapterUser);
                } else {
                    mMessageText.setText(getApplicationContext().getResources().getString(R.string.sign_in_reminder));
                    mListView.setAdapter(null);
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAppDocuments();
        loadUserDocuments();
    }

    private enum DocumentType {
        READONLY,
        USER
    }
}
