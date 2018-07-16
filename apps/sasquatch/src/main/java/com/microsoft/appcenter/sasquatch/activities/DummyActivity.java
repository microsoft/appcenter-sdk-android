package com.microsoft.appcenter.sasquatch.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.microsoft.appcenter.sasquatch.R;

import java.util.Locale;

public class DummyActivity extends AppCompatActivity {

    private static int counter = 0;

    private int currentCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dummy);

        counter++;
        ((TextView) findViewById(R.id.counter)).setText(String.format(Locale.ENGLISH, "%d", counter));

        final Handler handler = new Handler();
        if (counter < 5) {
            currentCounter = counter;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivityForResult(new Intent(DummyActivity.this, DummyActivity.class), currentCounter);
                }
            }, 500);
        } else {
            counter = 0;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setResult(Activity.RESULT_OK);
                    finish();
                }
            }, 500);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final Handler handler = new Handler();
        if (this.currentCounter == requestCode) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setResult(Activity.RESULT_OK);
                    finish();
                }
            }, 500);
        }
    }
}
