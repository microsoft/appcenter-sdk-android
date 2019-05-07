/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.fragments;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import java.util.Calendar;
import java.util.Date;

public class DatePickerFragment extends DialogFragment {

    public static final String INITIAL_DATE = "initial_date";

    private DatePickerDialog.OnDateSetListener mListener;

    private Date getInitialDate() {
        Bundle arguments = getArguments();
        if (arguments != null) {
            return (Date) arguments.getSerializable(INITIAL_DATE);
        }
        return null;
    }

    public void setListener(DatePickerDialog.OnDateSetListener listener) {
        mListener = listener;
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(getInitialDate());
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return new DatePickerDialog(getActivity(), mListener, year, month, day);
    }
}
