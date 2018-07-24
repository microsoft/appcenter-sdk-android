package com.microsoft.appcenter.sasquatch.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.microsoft.appcenter.sasquatch.R;

public class CommonSchemaPropertiesActivity extends AppCompatActivity {

    private Spinner mCommonSchemaPropertiesSpinner;

    private EditText mCommonSchemaPropertyValue;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_schema_properties);

        /* Common schema properties init. */
        mCommonSchemaPropertiesSpinner = findViewById(R.id.common_schema_property);
        ArrayAdapter<String> csAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.common_schema_properties));
        mCommonSchemaPropertiesSpinner.setAdapter(csAdapter);
        mCommonSchemaPropertiesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String savedPropertyValue = getSharedPreferences("Sasquatch", Context.MODE_PRIVATE).getString(mCommonSchemaPropertiesSpinner.getSelectedItem().toString(), "");
                mCommonSchemaPropertyValue.setText(savedPropertyValue);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        /* Common schema properties value listener. */
        mCommonSchemaPropertyValue = findViewById(R.id.common_schema_property_entry);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Might need to put this somewhere else
        SharedPreferences.Editor editor = getSharedPreferences("Sasquatch", Context.MODE_PRIVATE).edit();
        editor.putString(mCommonSchemaPropertiesSpinner.getSelectedItem().toString(), mCommonSchemaPropertyValue.getText().toString());
        editor.commit();
        super.onSaveInstanceState(savedInstanceState);
    }
}
