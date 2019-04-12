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
import android.widget.EditText;
import android.widget.Toast;

import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.fragments.TypedPropertyFragment;
import com.microsoft.appcenter.storage.Constants;
import com.microsoft.appcenter.storage.Storage;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NewUserDocumentActivity extends AppCompatActivity {

    private final List<TypedPropertyFragment> mProperties = new ArrayList<>();

    private EditText mEditDocumentId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_user_document);
        mEditDocumentId = findViewById(R.id.user_document_id);
        addProperty();
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
        Storage.replace(Constants.USER, documentId, document, Map.class).thenAccept(new AppCenterConsumer<Document<Map>>() {
            @Override
            public void accept(Document<Map> mapDocument) {
                if (mapDocument.failed())
                    Toast.makeText(NewUserDocumentActivity.this, R.string.message_whether_error, Toast.LENGTH_SHORT).show();
                else {
                    Toast.makeText(NewUserDocumentActivity.this, R.string.message_whether_success, Toast.LENGTH_SHORT).show();
                    NewUserDocumentActivity.this.finish();
                }
            }
        });
    }
}
