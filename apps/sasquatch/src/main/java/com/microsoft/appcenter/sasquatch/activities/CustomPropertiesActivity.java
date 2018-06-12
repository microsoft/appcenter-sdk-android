package com.microsoft.appcenter.sasquatch.activities;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.CustomProperties;
import com.microsoft.appcenter.sasquatch.R;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CustomPropertiesActivity extends AppCompatActivity {

    private final List<CustomPropertyFragment> mProperties = new ArrayList<>();

    private CustomPropertyFragment mCurrentProperty = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_properties);
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
        CustomPropertyFragment fragment = new CustomPropertyFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.list, fragment).commit();
        mProperties.add(fragment);
    }

    @SuppressWarnings({"unused", "unchecked"})
    public void send(@SuppressWarnings("UnusedParameters") View view) {
        CustomProperties customProperties = new CustomProperties();
        for (CustomPropertyFragment property : mProperties) {
            property.set(customProperties);
        }
        AppCenter.setCustomProperties(customProperties);
    }

    public static class CustomPropertyFragment extends Fragment
            implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

        private static final int TYPE_CLEAR = 0;
        private static final int TYPE_BOOLEAN = 1;
        private static final int TYPE_NUMBER = 2;
        private static final int TYPE_DATETIME = 3;
        private static final int TYPE_STRING = 4;

        private EditText mEditKey;
        private Spinner mEditType;
        private EditText mEditString;
        private EditText mEditNumber;
        private EditText mEditDate;
        private EditText mEditTime;
        private CheckBox mEditBool;
        private View mDateTime;

        private Date mDate;

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.custom_property, container, false);

            mEditKey = view.findViewById(R.id.key);
            mEditType = view.findViewById(R.id.type);
            mEditString = view.findViewById(R.id.string);
            mEditNumber = view.findViewById(R.id.number);
            mEditDate = view.findViewById(R.id.date);
            mEditTime = view.findViewById(R.id.time);
            mEditBool = view.findViewById(R.id.bool);
            mDateTime = view.findViewById(R.id.datetime);

            mEditType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    updateValueType();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            mEditDate.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    showDate();
                }
            });
            mEditTime.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    showTime();
                }
            });

            setDate(new Date());

            return view;
        }

        private void showDate() {
            CustomPropertiesActivity activity = (CustomPropertiesActivity) getActivity();
            activity.mCurrentProperty = this;
            new DatePickerFragment().show(activity.getSupportFragmentManager(), "datePicker");
        }

        private void showTime() {
            CustomPropertiesActivity activity = (CustomPropertiesActivity) getActivity();
            activity.mCurrentProperty = this;
            new TimePickerFragment().show(activity.getSupportFragmentManager(), "timePicker");
        }

        private void updateValueType() {
            int type = mEditType.getSelectedItemPosition();
            mEditString.setVisibility(type == TYPE_STRING ? View.VISIBLE : View.GONE);
            mEditNumber.setVisibility(type == TYPE_NUMBER ? View.VISIBLE : View.GONE);
            mEditBool.setVisibility(type == TYPE_BOOLEAN ? View.VISIBLE : View.GONE);
            mDateTime.setVisibility(type == TYPE_DATETIME ? View.VISIBLE : View.GONE);
        }

        private void setDate(Date date) {
            mDate = date;
            mEditDate.setText(DateFormat.getDateInstance().format(mDate));
            mEditTime.setText(DateFormat.getTimeInstance().format(mDate));
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(mDate);
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            setDate(calendar.getTime());
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(mDate);
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            setDate(calendar.getTime());
        }

        void set(CustomProperties customProperties) {
            int type = mEditType.getSelectedItemPosition();
            String key = mEditKey.getText().toString();
            switch (type) {
                case TYPE_CLEAR:
                    customProperties.clear(key);
                    break;
                case TYPE_BOOLEAN:
                    customProperties.set(key, mEditBool.isChecked());
                    break;
                case TYPE_NUMBER:
                    String stringValue = mEditNumber.getText().toString();
                    Number value;
                    try {
                        value = Integer.parseInt(stringValue);
                    } catch (NumberFormatException ignored) {
                        value = Double.parseDouble(stringValue);
                    }
                    customProperties.set(key, value);
                    break;
                case TYPE_DATETIME:
                    customProperties.set(key, mDate);
                    break;
                case TYPE_STRING:
                    customProperties.set(key, mEditString.getText().toString());
                    break;
            }
        }
    }

    public static class DatePickerFragment extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            CustomPropertiesActivity activity = (CustomPropertiesActivity) getActivity();
            CustomPropertyFragment property = activity.mCurrentProperty;
            Calendar calendar = Calendar.getInstance();
            if (property != null) {
                calendar.setTime(property.mDate);
            }
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            return new DatePickerDialog(activity, property, year, month, day);
        }
    }

    public static class TimePickerFragment extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            CustomPropertiesActivity activity = (CustomPropertiesActivity) getActivity();
            CustomPropertyFragment property = activity.mCurrentProperty;
            Calendar calendar = Calendar.getInstance();
            if (property != null) {
                calendar.setTime(property.mDate);
            }
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            return new TimePickerDialog(activity, property, hour, minute, true);
        }
    }
}
