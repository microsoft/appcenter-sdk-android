package com.microsoft.appcenter.sasquatch.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.fragments.TypedPropertyFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract public class PropertyActivity extends AppCompatActivity {

    protected final List<TypedPropertyFragment> mProperties = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property);
        findViewById(R.id.send_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send(v);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            addProperty();
        }
        return true;
    }

    /**
     * Indicate whether properties support strong types or not.
     *
     * @return <code>true</code> if it only supports string type, otherwise <code>false</code>.
     */
    protected boolean isStringTypeOnly() {
        return false;
    }

    protected Map<String, String> readStringProperties() {
        Map<String, String> properties = new HashMap<>();
        for (TypedPropertyFragment fragment : mProperties) {
            fragment.set(properties);
        }
        return properties;
    }

    private void addProperty() {
        TypedPropertyFragment fragment = new TypedPropertyFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean(TypedPropertyFragment.STRING_TYPE_ONLY_KEY, isStringTypeOnly());
        fragment.setArguments(bundle);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.list, fragment).commit();
        mProperties.add(fragment);
    }

    /**
     * Event handler of Send button on the activity.
     *
     * @param view The view of send button.
     */
    abstract protected void send(View view);
}
