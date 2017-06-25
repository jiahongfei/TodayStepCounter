package com.today.step.lib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 开机完成广播
 *
 */
public class BootCompleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        Intent vitalityIntent = new Intent(context, VitalityStepService.class);
        vitalityIntent.putExtra(VitalityStepService.INTENT_NAME_BOOT,true);
        context.startService(vitalityIntent);

        Log.e("BootCompleteReceiver","BootCompleteReceiver");

    }
}
