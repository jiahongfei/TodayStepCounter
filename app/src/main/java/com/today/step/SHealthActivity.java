package com.today.step;

import android.app.Application;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.today.step.lib.SamSungHealth;

/**
 * Created by jiahongfei on 2017/7/3.
 */

public class SHealthActivity extends AppCompatActivity {

    private static final String TAG = "SHealthActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shealth);

        SamSungHealth.getInstance().initSamSungHealth((Application) getApplicationContext());
        SamSungHealth.getInstance().connectService(new SamSungHealth.OnSamSungHealthConnectionListener() {
            @Override
            public void onConnectionFailed(HealthConnectionErrorResult error) {

            }

            @Override
            public void onChangeStepCounter(int step) {
                updateStepCount(step);
            }

            @Override
            public void onSaveStepCounter(int step, long millisecond) {

            }
        });
    }

    private void updateStepCount(int step) {
        Log.e(TAG,"updateStepCount : " + step);
        TextView stepTextView = (TextView)findViewById(R.id.stepTextView);
        stepTextView.setText(step + "步");
    }
}
