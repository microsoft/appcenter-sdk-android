/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.fragments.TypedPropertyFragment;
import com.microsoft.appcenter.storage.Constants;
import com.microsoft.appcenter.storage.Storage;
import com.microsoft.appcenter.storage.models.BaseOptions;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.WriteOptions;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NewUserDocumentActivity extends AppCompatActivity {

    private final List<TypedPropertyFragment> mProperties = new ArrayList<>();

    private EditText mEditDocumentId;

    private WriteOptions mWriteOptions = new WriteOptions(BaseOptions.DEFAULT_EXPIRATION_IN_SECONDS);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_user_document);
        mEditDocumentId = findViewById(R.id.user_document_id);
        Spinner ttlSpinner = findViewById(R.id.ttl_spinner);
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.storage_ttls));
        ttlSpinner.setAdapter(typeAdapter);
        ttlSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateWriteOptions(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        addProperty();
    }

    private void updateWriteOptions(int position) {
        StorageTtl storageTtl = StorageTtl.values()[position];
        switch (storageTtl) {
            case DEFAULT:
                mWriteOptions = new WriteOptions(BaseOptions.DEFAULT_EXPIRATION_IN_SECONDS);
                break;
            case NO_CACHE:
                mWriteOptions = WriteOptions.CreateNoCacheOption();
                break;
            case TWO_SECONDS:
                mWriteOptions = new WriteOptions(2);
                break;
            case INFINITE:
                mWriteOptions = WriteOptions.CreateInfiniteCacheOption();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                addProperty();
                break;
        }
        return true;
    }

    private void addProperty() {
        TypedPropertyFragment fragment = new TypedPropertyFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.list, fragment).commit();
        mProperties.add(fragment);
    }

    public void save(View view) {
        Map<String, Object> document = new LinkedHashMap<>();
        for (TypedPropertyFragment property : mProperties) {
            property.setGenericProperty(document);
        }
        String documentId = mEditDocumentId.getText().toString();
        documentId = documentId.replace(" ", "-");
        Storage.replace(Constants.USER, documentId, document, Map.class, mWriteOptions).thenAccept(new AppCenterConsumer<Document<Map>>() {

            @Override
            public void accept(Document<Map> mapDocument) {
                if (mapDocument.failed()) {
                    Toast.makeText(NewUserDocumentActivity.this, R.string.message_whether_error, Toast.LENGTH_SHORT).show();
                    mProperties.clear();
                } else {
                    Toast.makeText(NewUserDocumentActivity.this, R.string.message_whether_success, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    private enum StorageTtl {
        DEFAULT,
        NO_CACHE,
        TWO_SECONDS,
        INFINITE
    }
}
