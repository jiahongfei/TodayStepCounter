package com.today.step;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.today.step.lib.BaseConstantDef;
import com.today.step.lib.Logger;
import com.today.step.lib.VitalityStepService;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";

    //循环取当前时刻的步数中间的间隔时间
    private long TIME_INTERVAL_REFRESH = 500;

    private Messenger messenger = new Messenger(new Handler(new TodayStepCounterCall()));
    private Messenger mServiceMessenger = null;
    private Handler mDelayHandler = new Handler(new TodayStepCounterCall());
    private int mStepSum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this, VitalityStepService.class);
        startService(intent);
        bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                try {
                    Logger.d(TAG, "send msg to fetch step count when onServiceConnected");
                    mServiceMessenger = new Messenger(service);
                    Message msg = Message.obtain(null, BaseConstantDef.MSG_FROM_CLIENT);
                    msg.replyTo = messenger;
                    mServiceMessenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        }, Context.BIND_AUTO_CREATE);
    }

    class TodayStepCounterCall implements Handler.Callback{

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case BaseConstantDef.MSG_FROM_SERVER:
                    // 更新界面上的步数
                    int type = msg.getData().getInt(VitalityStepService.VITALITY_STEP_TYPE);
                    if(VitalityStepService.VITALITY_STEP_TYPE_REFRESH_SHOW == type) {
                        int step = msg.getData().getInt("step");
                        if (mStepSum != step) {
                            mStepSum = step;
                            updateStepCount();
                        }
                    }
                    mDelayHandler.sendEmptyMessageDelayed(BaseConstantDef.REQUEST_SERVER, TIME_INTERVAL_REFRESH);
                    break;
                case BaseConstantDef.REQUEST_SERVER:
                    try {
                        Message msg1 = Message.obtain(null, BaseConstantDef.MSG_FROM_CLIENT);
                        msg1.replyTo = messenger;
                        mServiceMessenger.send(msg1);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    break;
            }
            return false;
        }
    }

    private void updateStepCount() {
        Log.e(TAG,"updateStepCount : " + mStepSum);
        TextView stepTextView = (TextView)findViewById(R.id.stepTextView);
        stepTextView.setText(mStepSum + "步");
    }
}
