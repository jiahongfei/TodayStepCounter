package com.today.step.lib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 开机完成广播
 *
 */
public class TodayStepBootCompleteReceiver extends BroadcastReceiver {

    private static final String TAG = "TodayStepBootCompleteReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        Intent todayStepIntent = new Intent(context, TodayStepService.class);
        todayStepIntent.putExtra(TodayStepService.INTENT_NAME_BOOT,true);
        context.startService(todayStepIntent);

        Logger.e(TAG,"TodayStepBootCompleteReceiver");

    }
}
