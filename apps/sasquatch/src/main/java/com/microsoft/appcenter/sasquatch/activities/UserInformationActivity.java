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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        String userId = intent.getStringExtra(USER_INFORMATION_ID);
        String idToken = intent.getStringExtra(USER_INFORMATION_ID_TOKEN);
        String accessToken = intent.getStringExtra(USER_INFORMATION_ACCESS_TOKEN);
        mListView = findViewById(R.id.user_info_list_view);
        fillInfo(userId, idToken, accessToken);
    }

    private JSONObject getParsedToken(String rawToken) {
        try {
            JWT parsedIdToken = JWTParser.parse(rawToken);
            Map<String, Object> claims = parsedIdToken.getJWTClaimsSet().getClaims();
            return new JSONObject(claims);
        } catch (ParseException ex) {
            AppCenterLog.error(AppCenterLog.LOG_TAG, getString(R.string.b2c_jwt_parse_error));
        }
        return null;
    }

    private void fillInfo(String userId, String idToken, String accessToken) {
        JSONObject idTokenJSON = getParsedToken(idToken);
        JSONObject accessTokenJSON = getParsedToken(accessToken);
        final List<UserInfoDisplayModel> list = getUserInfoDisplayModelList(userId, idTokenJSON, accessTokenJSON);
        ArrayAdapter<UserInfoDisplayModel> adapter = new ArrayAdapter<UserInfoDisplayModel>(this, R.layout.info_list_item, R.id.info_title, list) {

            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView titleView = view.findViewById(R.id.info_title);
                final TextView valueView = view.findViewById(R.id.info_content);
                titleView.setText(list.get(position).mTitle);
                valueView.setText(list.get(position).mValue);
                if (list.get(position).mTitle.equals(getString(R.string.b2c_user_info_id_token_title))) {
                    view.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {
                            valueView.setText(mFullIdToken);
                        }
                    });
                }
                if (list.get(position).mTitle.equals(getString(R.string.b2c_user_info_access_token_title))) {
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

    private List<UserInfoDisplayModel> getUserInfoDisplayModelList(String userId, JSONObject idTokenJSON, JSONObject accessTokenJSON) {
        List<UserInfoDisplayModel> list = new ArrayList<>();
        list.add(new UserInfoDisplayModel(getString(R.string.b2c_user_info_id_title), userId));
        String idTokenPreview;
        try {
            if (idTokenJSON == null) {
                mFullIdToken = idTokenPreview = getString(R.string.b2c_jwt_parse_error);
            } else {
                mFullIdToken = idTokenJSON.toString(4).replace("\\", "");
                idTokenPreview = idTokenJSON.toString();
            }
        } catch (JSONException e) {
            mFullIdToken = idTokenPreview = getString(R.string.b2c_jwt_parse_json_error);
        }
        if (idTokenPreview.length() > MAX_CONTENT_LENGTH) {
            idTokenPreview = idTokenPreview.substring(0, MAX_CONTENT_LENGTH) + "...";
        }
        list.add(new UserInfoDisplayModel(getString(R.string.b2c_user_info_id_token_title), idTokenPreview));
        String accessTokenPreview;
        try {
            if (accessTokenJSON == null) {
                mFullAccessToken = accessTokenPreview = getString(R.string.b2c_jwt_parse_error);
            } else {
                mFullAccessToken = accessTokenJSON.toString(4).replace("\\", "");
                accessTokenPreview = accessTokenJSON.toString();
            }
        } catch (JSONException e) {
            mFullAccessToken = accessTokenPreview = getString(R.string.b2c_jwt_parse_json_error);
        }
        if (accessTokenPreview.length() > MAX_CONTENT_LENGTH) {
            accessTokenPreview = accessTokenPreview.substring(0, MAX_CONTENT_LENGTH) + "...";
        }
        list.add(new UserInfoDisplayModel(getString(R.string.b2c_user_info_access_token_title), accessTokenPreview));
        return list;
    }

    @VisibleForTesting
    class UserInfoDisplayModel {

        final String mTitle;

        final String mValue;

        UserInfoDisplayModel(String title, String value) {
            mTitle = title;
            mValue = value;
        }
    }
}
