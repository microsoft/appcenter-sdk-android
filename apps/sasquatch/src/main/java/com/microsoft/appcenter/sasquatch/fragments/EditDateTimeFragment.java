/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import com.microsoft.appcenter.sasquatch.R;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

public abstract class EditDateTimeFragment extends Fragment
        implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    private EditText mEditDate;

    private EditText mEditTime;

    View mDateTime;

    /**
     * Date value, with default being current time.
     */
    Date mDate = new Date();

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        /* Find views. */
        mEditDate = view.findViewById(R.id.date);
        mEditTime = view.findViewById(R.id.time);
        mDateTime = view.findViewById(R.id.datetime);

        /* Set listeners. */
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

        /* Refresh UI with set date value. */
        setDate(mDate);
    }

    private void showDate() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putSerializable(DatePickerFragment.INITIAL_DATE, mDate);
        DatePickerFragment fragment = new DatePickerFragment();
        fragment.setArguments(bundle);
        fragment.setListener(this);
        fragment.show(activity.getSupportFragmentManager(), "datePicker");
    }

    private void showTime() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putSerializable(TimePickerFragment.INITIAL_TIME, mDate);
        TimePickerFragment fragment = new TimePickerFragment();
        fragment.setArguments(bundle);
        fragment.setListener(this);
        fragment.show(getActivity().getSupportFragmentManager(), "timePicker");
    }

    void setDate(Date date) {
        mDate = date;

        /* If UI ready update now, otherwise do it in onCreateView. */
        if (mEditDate != null) {
            mEditDate.setText(DateFormat.getDateInstance().format(mDate));
            mEditTime.setText(DateFormat.getTimeInstance().format(mDate));
        }
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
}