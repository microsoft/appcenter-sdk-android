/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.microsoft.appcenter.sasquatch.JwtUtils;
import com.microsoft.appcenter.sasquatch.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.appcenter.sasquatch.SasquatchConstants.USER_INFORMATION_ACCESS_TOKEN;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.USER_INFORMATION_ID;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.USER_INFORMATION_ID_TOKEN;

public class UserInformationActivity extends AppCompatActivity {

    private static final int MAX_CONTENT_LENGTH = 50;

    private String mFullIdToken;

    private String mFullAccessToken;

    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_information);
        Intent intent = getIntent();
        String accountId = intent.getStringExtra(USER_INFORMATION_ID);
        String idToken = intent.getStringExtra(USER_INFORMATION_ID_TOKEN);
        String accessToken = intent.getStringExtra(USER_INFORMATION_ACCESS_TOKEN);
        mListView = findViewById(R.id.user_info_list_view);
        fillInfo(accountId, idToken, accessToken);
    }

    private void fillInfo(String accountId, String idToken, String accessToken) {
        final List<UserInfoDisplayModel> list = getUserInfoDisplayModelList(accountId, idToken, accessToken);
        ArrayAdapter<UserInfoDisplayModel> adapter = new ArrayAdapter<UserInfoDisplayModel>(this, R.layout.info_list_item, R.id.info_title, list) {

            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView titleView = view.findViewById(R.id.info_title);
                final TextView valueView = view.findViewById(R.id.info_content);
                titleView.setText(list.get(position).mTitle);
                valueView.setText(list.get(position).mValue);
                if (list.get(position).mTitle.equals(getString(R.string.user_info_id_token_title))) {
                    view.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {
                            valueView.setText(mFullIdToken);
                        }
                    });
                }
                if (list.get(position).mTitle.equals(getString(R.string.user_info_access_token_title))) {
                    view.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {
                            valueView.setText(mFullAccessToken);
                        }
                    });
                }
                return view;
            }
        };
        mListView.setAdapter(adapter);
    }

    private List<UserInfoDisplayModel> getUserInfoDisplayModelList(String accountId, String idToken, String accessToken) {
        List<UserInfoDisplayModel> list = new ArrayList<>();
        JSONObject idTokenJSON = idToken == null ? null : JwtUtils.getParsedToken(this, idToken);
        if (accountId == null && idTokenJSON != null) {
            accountId = idTokenJSON.optString("sub");
        }
        if (!TextUtils.isEmpty(accountId)) {
            list.add(new UserInfoDisplayModel(getString(R.string.user_info_id_title), accountId));
        }
        if (idToken != null) {
            mFullIdToken = parseAndAddTokenToList(getString(R.string.user_info_id_token_title), idTokenJSON, list);
        }
        if (accessToken != null) {
            JSONObject accessTokenJSON = JwtUtils.getParsedToken(this, accessToken);
            mFullAccessToken = parseAndAddTokenToList(getString(R.string.user_info_access_token_title), accessTokenJSON, list);
        }
        return list;
    }

    private String parseAndAddTokenToList(String title, JSONObject tokenJSON, List<UserInfoDisplayModel> list) {
        String fullToken;
        String tokenPreview;
        try {
            if (tokenJSON == null) {
                fullToken = tokenPreview = getString(R.string.jwt_parse_error);
            } else {
                fullToken = tokenJSON.toString(4).replace("\\", "");
                tokenPreview = tokenJSON.toString();
            }
        } catch (JSONException e) {
            fullToken = tokenPreview = getString(R.string.jwt_parse_json_error);
        }
        if (tokenPreview.length() > MAX_CONTENT_LENGTH) {
            tokenPreview = tokenPreview.substring(0, MAX_CONTENT_LENGTH) + "...";
        }
        list.add(new UserInfoDisplayModel(title, tokenPreview));
        return fullToken;
    }

    @VisibleForTesting
    static class UserInfoDisplayModel {

        final String mTitle;

        final String mValue;

        UserInfoDisplayModel(String title, String value) {
            mTitle = title;
            mValue = value;
        }
    }
}
