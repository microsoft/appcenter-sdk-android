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
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.storage.Constants;
import com.microsoft.appcenter.storage.Storage;
import com.microsoft.appcenter.storage.Utils;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.PaginatedDocuments;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;

import java.util.ArrayList;
import java.util.Map;

import static com.microsoft.appcenter.sasquatch.SasquatchConstants.ACCOUNT_ID;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_CONTENT;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_ID;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.USER_DOCUMENT_CONTENTS;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.USER_DOCUMENT_LIST;

class TestDocument {

    @SuppressWarnings("unused")
    String key;
}

public class StorageActivity extends AppCompatActivity {

    public static ArrayList<String> sUserDocumentList = new ArrayList<String>();

    private ListView mListView;

    private CustomItemAdapter mAdapterUser;

    private ArrayAdapter<String> mAppDocumentListAdapter;

    private ArrayList<String> mDocumentContents = new ArrayList<>();

    private ArrayList<String> mUserDocumentContents = new ArrayList<>();

    private StorageType mStorageType = StorageType.READONLY;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage);

        /* List the app read-only documents. */
        mAppDocumentListAdapter = new ArrayAdapter<>(this, R.layout.item_view_app);
        Storage.list(Constants.READONLY, TestDocument.class).thenAccept(new AppCenterConsumer<PaginatedDocuments<TestDocument>>() {

            @Override
            public void accept(PaginatedDocuments<TestDocument> documents) {

                /* TODO handle multiple pages. */
                for (Document<TestDocument> document : documents.getCurrentPage().getItems()) {
                    mAppDocumentListAdapter.add(document.getId());
                    mDocumentContents.add(Utils.getGson().toJson(document.getDocument()));
                }
            }
        });

        /* List the user documents. */
        sUserDocumentList.clear();
        mUserDocumentContents.clear();
        if (mAdapterUser == null) {
            String accountId = MainActivity.sSharedPreferences.getString(ACCOUNT_ID, null);
            if (accountId != null) {
                Storage.list(Constants.USER, Map.class).thenAccept(new AppCenterConsumer<PaginatedDocuments<Map>>() {

                    @Override
                    public void accept(PaginatedDocuments<Map> documents) {
                        for (Document<Map> document : documents.getCurrentPage().getItems()) {
                            sUserDocumentList.add(document.getId());
                            mUserDocumentContents.add(Utils.getGson().toJson(document.getDocument()));
                        }
                    }
                });
            }
        }

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

        /* Create the list view. */
        mListView = findViewById(R.id.list);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (mStorageType) {
                    case USER:
                        break;
                    case READONLY:
                        Intent intent = new Intent(StorageActivity.this, AppDocumentDetailActivity.class);
                        intent.putExtra(DOCUMENT_ID, mAppDocumentListAdapter.getItem(position));
                        intent.putExtra(DOCUMENT_CONTENT, mDocumentContents.get(position));
                        startActivity(intent);
                        break;
                }
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
                break;
            case USER:
                addNewDocument.setVisible(true);
                /* Remove the toast and string resource once implementation ready. */
                Toast.makeText(this, R.string.user_document_wip, Toast.LENGTH_LONG).show();
                String accountId = MainActivity.sSharedPreferences.getString(ACCOUNT_ID, null);
                if (accountId != null) {
                    mAdapterUser = new CustomItemAdapter(sUserDocumentList, this);
                    mListView.setAdapter(mAdapterUser);
                    saveArrayToPreferences(sUserDocumentList, USER_DOCUMENT_LIST);
                    saveArrayToPreferences(mUserDocumentContents, USER_DOCUMENT_CONTENTS);
                } else {
                    ArrayList<String> signInReminder = new ArrayList<String>() {{
                        add(getApplicationContext().getResources().getString(R.string.sign_in_reminder));
                    }};
                    mListView.setAdapter(new ArrayAdapter<>(this, R.layout.item_view_app, signInReminder));
                }
                break;
        }
    }

    public boolean saveArrayToPreferences(ArrayList<String> list, String name) {
        MainActivity.sSharedPreferences = getSharedPreferences(name, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = MainActivity.sSharedPreferences.edit();
        editor.putInt("Status_size", list.size());
        for (int i = 0; i < list.size(); i++) {
            editor.remove("Status_" + i);
            editor.putString("Status_" + i, list.get(i));
        }
        return editor.commit();
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

    private enum StorageType {
        READONLY,
        USER
    }
}
