package com.microsoft.appcenter.sasquatch.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.microsoft.appcenter.sasquatch.R;

import java.util.HashMap;
import java.util.Map;

public abstract class LogActivity extends AppCompatActivity {

    private ViewGroup mList;

    private LayoutInflater mLayoutInflater;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        mLayoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        /* Property view init. */
        mList = findViewById(R.id.list);
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
        mList.addView(mLayoutInflater.inflate(R.layout.property, mList, false));
    }

    @SuppressWarnings("unused")
    public void send(@SuppressWarnings("UnusedParameters") View view) {
        String name = ((TextView) findViewById(R.id.name)).getText().toString();
        Map<String, String> properties = null;
        for (int i = 0; i < mList.getChildCount(); i++) {
            View childAt = mList.getChildAt(i);
            CharSequence key = ((TextView) childAt.findViewById(R.id.key)).getText();
            CharSequence value = ((TextView) childAt.findViewById(R.id.value)).getText();
            if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                if (properties == null) {
                    properties = new HashMap<>();
                }
                properties.put(key.toString(), value.toString());
            }
        }
        trackLog(name, properties);
    }

    abstract void trackLog(String name, Map<String, String> properties);

    abstract int getLayoutId();
}
