/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.storage.Constants;
import com.microsoft.appcenter.storage.Storage;
import com.microsoft.appcenter.storage.models.PaginatedDocuments;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import static com.microsoft.appcenter.sasquatch.activities.MainActivity.LOG_TAG;

class TestDocument {

    @SuppressWarnings("unused")
    String test = "ABC";
}

public class StorageActivity extends AppCompatActivity {

    private Spinner mStorageTypeSpinner;

    private ListView listView;

    private ArrayList<String> userDocumentList = new ArrayList<String>() {{
        add("Doc1-User");
        add("Doc2-User");
    }};

    private final ArrayList<String> appDocumentList = new ArrayList<String>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage);

        /* List the document. */
        Storage.list(Constants.READONLY, TestDocument.class).thenAccept(new AppCenterConsumer<PaginatedDocuments<TestDocument>>() {

            @Override
            public void accept(PaginatedDocuments<TestDocument> documents) {
                int listSize = documents.getCurrentPage().getItems().size();
                for (int i = 0; i < listSize; i++) {
                    appDocumentList.add(documents.getCurrentPage().getItems().get(i).getId());
                }
            }
        });

        /* Transmission target views init. */
        mStorageTypeSpinner = findViewById(R.id.storage_type);
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.storage_type_names));
        mStorageTypeSpinner.setAdapter(typeAdapter);
        mStorageTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateStorageType(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        listView = findViewById(R.id.list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            }
        });

        Storage.create("test-partition", "document-id-123", new TestDocument(), TestDocument.class);
        Storage.read("test-partition-other", "document-id-123", TestDocument.class);
        Storage.delete("test-partition", "document-id-123");
    }

    private void updateStorageType(int position) {
        switch (position) {
            case 0:
                listView.setAdapter(new ArrayAdapter<String>(this, R.layout.item_view_app, appDocumentList));
                break;
            case 1:
                if (AuthenticationProviderActivity.userID != null) {
                    CustomItemAdapter adapterUser = new CustomItemAdapter(userDocumentList, this);
                    listView.setAdapter(adapterUser);
                } else {
                    final ArrayList<String> signInReminder = new ArrayList<String>() {{
                        add(getApplicationContext().getResources().getString(R.string.sign_in_reminder));
                    }};
                    listView.setAdapter(new ArrayAdapter<String>(this, R.layout.item_view_app, signInReminder));
                }
                break;
        }
    }
}
