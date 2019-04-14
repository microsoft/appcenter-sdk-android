/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.Spinner;

import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.storage.Constants;
import com.microsoft.appcenter.storage.Storage;
import com.microsoft.appcenter.storage.Utils;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.Page;
import com.microsoft.appcenter.storage.models.PaginatedDocuments;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;

import java.util.ArrayList;
import java.util.Map;

import static com.microsoft.appcenter.sasquatch.SasquatchConstants.ACCOUNT_ID;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_CONTENT;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_ID;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.USER_DOCUMENT_CONTENTS;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.USER_DOCUMENT_LIST;

public class StorageActivity extends AppCompatActivity {

    public static String CACHED_PREFIX = "#";

    public static String REMOTE_PREFIX = "!";

    public static ArrayList<String> sUserDocumentList = new ArrayList<>();

    private RecyclerView mListView;

    private MenuItem addNewDocument;

    private CustomItemAdapter mAdapterUser;

    private AppDocumentListAdapter mAppDocumentListAdapter;

    private ArrayList<String> mUserDocumentContents = new ArrayList<>();

    private StorageType mStorageType = StorageType.READONLY;

    private PaginatedDocuments<TestDocument> currentDocuments;
    private AppCenterConsumer<PaginatedDocuments<TestDocument>> upload = new AppCenterConsumer<PaginatedDocuments<TestDocument>>() {

        @Override
        public void accept(PaginatedDocuments<TestDocument> documents) {
            currentDocuments = documents;
            mAppDocumentListAdapter.upload(documents.getCurrentPage().getItems());
            mAppDocumentListAdapter.notifyDataSetChanged();
        }
    };
    private RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {

        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (currentDocuments.hasNextPage()) {
                currentDocuments.getNextPage().thenAccept(new AppCenterConsumer<Page<TestDocument>>() {
                    @Override
                    public void accept(Page<TestDocument> testDocumentPage) {
                        mAppDocumentListAdapter.upload(testDocumentPage.getItems());
                    }
                });
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage);
        mListView = findViewById(R.id.list);
        mListView.setLayoutManager(new LinearLayoutManager(this));

        /* List the app read-only documents. */
        mAppDocumentListAdapter = new AppDocumentListAdapter(this, new ArrayList<Document<TestDocument>>());
        mAppDocumentListAdapter.setOnItemClickListener(new AppDocumentListAdapter.OnItemClickListener() {

            @Override
            public void onItemClick(int position) {
                Intent intent = new Intent(StorageActivity.this, AppDocumentDetailActivity.class);
                intent.putExtra(DOCUMENT_ID, mAppDocumentListAdapter.getItem(position));
                intent.putExtra(DOCUMENT_CONTENT, mAppDocumentListAdapter.getDocumentByPosition(position));
                startActivity(intent);
            }
        });
        Storage.list(Constants.READONLY, TestDocument.class).thenAccept(upload);

        /* List the user documents. */
        sUserDocumentList.clear();
        mUserDocumentContents.clear();
        if (mAdapterUser == null) {
            mAdapterUser = new CustomItemAdapter(new ArrayList<String>(), this);
            String accountId = MainActivity.sSharedPreferences.getString(ACCOUNT_ID, null);
            if (accountId != null) {
                Storage.list(Constants.USER, Map.class).thenAccept(new AppCenterConsumer<PaginatedDocuments<Map>>() {

                    @Override
                    public void accept(PaginatedDocuments<Map> documents) {
                        for (Document<Map> document : documents.getCurrentPage().getItems()) {
                            sUserDocumentList.add(String.format("%s_%s", document.isFromCache() ? CACHED_PREFIX : REMOTE_PREFIX, document.getId()));
                            mUserDocumentContents.add(Utils.getGson().toJson(document.getDocument()));
                        }
                    }
                });
            }
        }

        mAdapterUser.setOnItemClickListener(new AppDocumentListAdapter.OnItemClickListener() {

            @Override
            public void onItemClick(int position) {
                Intent intent = new Intent(StorageActivity.this, UserDocumentDetailActivity.class);
                intent.putExtra(USER_DOCUMENT_LIST, loadArrayFromPreferences(USER_DOCUMENT_LIST).get(position));
                intent.putExtra(USER_DOCUMENT_CONTENTS, loadArrayFromPreferences(USER_DOCUMENT_CONTENTS).get(position));
                startActivity(intent);
            }
        });

        /* Selector for App VS User documents. */
        Spinner storageTypeSpinner = findViewById(R.id.storage_type);
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.storage_type_names));
        storageTypeSpinner.setAdapter(typeAdapter);
        storageTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateStorageType(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                switch (mStorageType) {
                    case USER:
                        String accountId = MainActivity.sSharedPreferences.getString(ACCOUNT_ID, null);
                        if (accountId != null) {
                            Intent intent = new Intent(StorageActivity.this, NewUserDocumentActivity.class);
                            startActivity(intent);
                        }
                        break;

                    case READONLY:
                        final AlertDialog.Builder builder = new AlertDialog.Builder(StorageActivity.this);
                        builder.setIcon(R.drawable.ic_appcenter_logo);
                        builder.setTitle(getApplicationContext().getResources().getString(R.string.storage_type_reminder));
                        builder.setPositiveButton(getApplicationContext().getResources().getString(R.string.alert_ok), new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                builder.setCancelable(true);
                            }
                        });
                        builder.show();
                        break;
                }
                break;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add, menu);
        addNewDocument = menu.findItem(R.id.action_add);
        return true;
    }

    private void updateStorageType(int position) {
        mStorageType = StorageType.values()[position];
        switch (mStorageType) {
            case READONLY:
                addNewDocument.setVisible(false);
                mListView.setAdapter(mAppDocumentListAdapter);
                mListView.addOnScrollListener(scrollListener);
                break;
            case USER:
                addNewDocument.setVisible(true);
                mListView.removeOnScrollListener(scrollListener);
                String accountId = MainActivity.sSharedPreferences.getString(ACCOUNT_ID, null);
                if (accountId != null) {
                    mAdapterUser = new CustomItemAdapter(sUserDocumentList, this);
                    mListView.setAdapter(mAdapterUser);
                    saveArrayToPreferences(sUserDocumentList, USER_DOCUMENT_LIST);
                    saveArrayToPreferences(mUserDocumentContents, USER_DOCUMENT_CONTENTS);
                } else {

                    // TODO add message.
                    ArrayList<String> signInReminder = new ArrayList<String>() {{
                        add(getApplicationContext().getResources().getString(R.string.sign_in_reminder));
                    }};
                    mListView.setAdapter(null);
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapterUser != null) {
            sUserDocumentList.clear();
            mUserDocumentContents.clear();
            String accountId = MainActivity.sSharedPreferences.getString(ACCOUNT_ID, null);
            if (accountId != null) {
                Storage.list(Constants.USER, Map.class).thenAccept(new AppCenterConsumer<PaginatedDocuments<Map>>() {

                    @Override
                    public void accept(PaginatedDocuments<Map> documents) {
                        for (Document<Map> document : documents.getCurrentPage().getItems()) {
                            sUserDocumentList.add(document.getId());
                            mUserDocumentContents.add(Utils.getGson().toJson(document.getDocument()));
                        }
                        saveArrayToPreferences(sUserDocumentList, USER_DOCUMENT_LIST);
                        saveArrayToPreferences(mUserDocumentContents, USER_DOCUMENT_CONTENTS);
                        mAdapterUser.notifyDataSetChanged();
                    }
                });
            }
        }
    }

    public void saveArrayToPreferences(ArrayList<String> list, String name) {
        MainActivity.sSharedPreferences = getSharedPreferences(name, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = MainActivity.sSharedPreferences.edit();
        editor.putInt("Status_size", list.size());
        for (int i = 0; i < list.size(); i++) {
            editor.remove("Status_" + i);
            editor.putString("Status_" + i, list.get(i));
        }
        editor.commit();
    }

    private ArrayList<String> loadArrayFromPreferences(String name) {
        MainActivity.sSharedPreferences = getSharedPreferences(name, Context.MODE_PRIVATE);
        ArrayList<String> list = new ArrayList<>();
        int size = MainActivity.sSharedPreferences.getInt("Status_size", 0);
        for (int i = 0; i < size; i++) {
            list.add(MainActivity.sSharedPreferences.getString("Status_" + i, null));
        }
        return list;
    }

    private enum StorageType {
        READONLY,
        USER
    }
}
