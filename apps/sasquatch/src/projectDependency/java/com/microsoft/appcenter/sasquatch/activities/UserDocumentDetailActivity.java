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

import static com.microsoft.appcenter.sasquatch.SasquatchConstants.USER_DOCUMENT_CONTENTS;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.USER_DOCUMENT_LIST;

public class UserDocumentDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_document_detail);
        TextView documentIdView = findViewById(R.id.user_document_id);
        TextView documentContentsView = findViewById(R.id.user_document_content);
        Intent intent = getIntent();
        documentIdView.setText(intent.getStringExtra(USER_DOCUMENT_LIST));
        documentContentsView.setText(intent.getStringExtra(USER_DOCUMENT_CONTENTS));
    }
}
