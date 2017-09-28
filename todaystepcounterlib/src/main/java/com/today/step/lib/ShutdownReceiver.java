package com.today.step.lib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by jiahongfei on 2017/9/27.
 */

public class ShutdownReceiver extends BroadcastReceiver {

    private static final String TAG = "ShutdownReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SHUTDOWN)) {
            Toast.makeText(context,"shutdown",Toast.LENGTH_SHORT).show();
            Log.e(TAG,"shutdown");
        }
    }

}
