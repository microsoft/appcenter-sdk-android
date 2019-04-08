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

import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.storage.Constants;
import com.microsoft.appcenter.storage.Storage;
import com.microsoft.appcenter.storage.Utils;
import com.microsoft.appcenter.storage.models.PaginatedDocuments;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;

import java.util.ArrayList;

class TestDocument {

    @SuppressWarnings("unused")
    String test = "ABC";
}

public class StorageActivity extends AppCompatActivity {

    private ListView mListView;

    private ArrayAdapter<String> mAppDocumentListAdapter;

    private ArrayList<String> mDocumentContents = new ArrayList<String>();

    private ArrayList<String> mUserDocumentList = new ArrayList<String>() {{
        add("Doc1-User");
        add("Doc2-User");
    }};

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
                int listSize = documents.getCurrentPage().getItems().size();
                for (int i = 0; i < listSize; i++) {
                    mAppDocumentListAdapter.add(documents.getCurrentPage().getItems().get(i).getId());
                    mDocumentContents.add(Utils.getGson().toJson(documents.getCurrentPage().getItems().get(i).getDocument()));
                }
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
                        intent.putExtra("documentId", mAppDocumentListAdapter.getItem(position));
                        intent.putExtra("documentContent", mDocumentContents.get(position).toString());
                        startActivity(intent);
                        break;
                }
            }
        });

        /* Some ad-hoc calls TODO replace by a UI equivalent. */
        Storage.create("test-partition", "document-id-123", new TestDocument(), TestDocument.class);
        Storage.read("test-partition-other", "document-id-123", TestDocument.class);
        Storage.delete("test-partition", "document-id-123");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                switch (mStorageType) {
                    case USER:
                        SharedPreferences preferences = getSharedPreferences("Id", Context.MODE_PRIVATE);
                        String accountId = preferences.getString("accountId", null);
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
        return true;
    }

    private void updateStorageType(int position) {
        mStorageType = StorageType.values()[position];
        switch (mStorageType) {
            case READONLY:
                mListView.setAdapter(mAppDocumentListAdapter);
                break;
            case USER:
                SharedPreferences preferences = getSharedPreferences("Id", Context.MODE_PRIVATE);
                String accountId = preferences.getString("accountId", null);
                if (accountId != null) {
                    CustomItemAdapter adapterUser = new CustomItemAdapter(mUserDocumentList, this);
                    mListView.setAdapter(adapterUser);
                } else {
                    ArrayList<String> signInReminder = new ArrayList<String>() {{
                        add(getApplicationContext().getResources().getString(R.string.sign_in_reminder));
                    }};
                    mListView.setAdapter(new ArrayAdapter<>(this, R.layout.item_view_app, signInReminder));
                }
                break;
        }
    }

    private enum StorageType {
        READONLY,
        USER
    }
}
