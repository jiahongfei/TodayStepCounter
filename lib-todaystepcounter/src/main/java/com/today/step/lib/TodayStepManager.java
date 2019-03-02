package com.today.step.lib;

import android.app.Activity;
import android.app.Application;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;

/**
 * 计步SDK初始化方法
 * Created by jiahongfei on 2017/10/9.
 */

public class TodayStepManager {

    private static final String TAG = "TodayStepManager";

    public static void startTodayStepService(Application application) {
        try {
            Intent intent = new Intent(application, TodayStepService.class);
            ContextCompat.startForegroundService(application, intent);
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

    public static boolean bindService(Activity activity, ServiceConnection conn) {
        try {
            Intent intent = new Intent(activity, TodayStepService.class);
            return activity.bindService(intent, conn, Activity.BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
//            https://stackoverflow.com/questions/38764497/security-exception-unable-to-start-service-user-0-is-restricted
            //            经过和OPPO工程师沟通，这个问题的原因是OPPO手机自动熄屏一段时间后，会启用系统自带的电量优化管理，
//            禁止一切自启动的APP（用户设置的自启动白名单除外）。所以，类似的崩溃常常集中在用户休息之后的夜里或者凌晨，
//            但是并不影响用户平时的正常使用。至于会出现user 0 is restricted，我觉得是coloros系统电量优化管理做得不好的地方。
//            对coloros官方的处理建议：既然禁止自启动，那么干脆直接force stop对应的进程，而不是抛出RuntimeException来让开发者买单。
//            对开发者处理建议：在服务启动的地方进行try catch防止崩溃即可（也是“1元夺宝”APP目前的处理方式）
        }
        return false;

    }
}
