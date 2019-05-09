/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.fragments;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import java.util.Calendar;
import java.util.Date;

public class TimePickerFragment extends DialogFragment {

    public static final String INITIAL_TIME = "initial_time";

    private TimePickerDialog.OnTimeSetListener mListener;

    private Date getInitialTime() {
        Bundle arguments = getArguments();
        if (arguments != null) {
            return (Date) arguments.getSerializable(INITIAL_TIME);
        }
        return null;
    }

    public void setListener(TimePickerDialog.OnTimeSetListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(getInitialTime());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        return new TimePickerDialog(getActivity(), mListener, hour, minute, true);
    }
}
