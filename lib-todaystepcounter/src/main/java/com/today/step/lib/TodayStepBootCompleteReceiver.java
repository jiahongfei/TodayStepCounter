package com.today.step.lib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;

/**
 * 开机完成广播
 */
public class TodayStepBootCompleteReceiver extends BroadcastReceiver {

    private static final String TAG = "TodayStepBootCompleteReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        try {
            Intent todayStepIntent = new Intent(context, TodayStepService.class);
            todayStepIntent.putExtra(TodayStepService.INTENT_NAME_BOOT, true);
            ContextCompat.startForegroundService(context,todayStepIntent);

        } catch (Exception e) {
            e.printStackTrace();
//            https://stackoverflow.com/questions/38764497/security-exception-unable-to-start-service-user-0-is-restricted
            //            经过和OPPO工程师沟通，这个问题的原因是OPPO手机自动熄屏一段时间后，会启用系统自带的电量优化管理，
//            禁止一切自启动的APP（用户设置的自启动白名单除外）。所以，类似的崩溃常常集中在用户休息之后的夜里或者凌晨，
//            但是并不影响用户平时的正常使用。至于会出现user 0 is restricted，我觉得是coloros系统电量优化管理做得不好的地方。
//            对coloros官方的处理建议：既然禁止自启动，那么干脆直接force stop对应的进程，而不是抛出RuntimeException来让开发者买单。
//            对开发者处理建议：在服务启动的地方进行try catch防止崩溃即可（也是“1元夺宝”APP目前的处理方式）
        }

    }
}
