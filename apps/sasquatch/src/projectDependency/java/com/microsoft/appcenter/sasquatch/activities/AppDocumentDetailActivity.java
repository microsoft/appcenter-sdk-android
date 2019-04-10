/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.microsoft.appcenter.sasquatch.R;

import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_CONTENT;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_ID;

public class AppDocumentDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_document_detail);
        TextView documentIdView = findViewById(R.id.app_document_id);
        TextView documentContentsView = findViewById(R.id.app_document_content);
        Intent intent = getIntent();
        documentIdView.setText(intent.getStringExtra(DOCUMENT_ID));
        documentContentsView.setText(intent.getStringExtra(DOCUMENT_CONTENT));
    }
}
