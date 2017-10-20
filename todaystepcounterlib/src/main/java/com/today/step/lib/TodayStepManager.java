package com.today.step.lib;

import android.app.Application;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;

/**
 * 计步SDK初始化方法
 * Created by jiahongfei on 2017/10/9.
 */

public class TodayStepManager {

    private static final String TAG = "TodayStepManager";
    private static final int JOB_ID = 100;

    /**
     * 在程序的最开始调用，最好在自定义的application oncreate中调用
     *
     * @param application
     */
    public static void init(Application application) {

        StepAlertManagerUtils.set0SeparateAlertManager(application);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            initJobScheduler(application);
        }


        startTodayStepService(application);
    }

    public static void startTodayStepService(Application application) {
        Intent intent = new Intent(application, TodayStepService.class);
        application.startService(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void initJobScheduler(Application application) {
        Logger.e(TAG, "initJobScheduler");

        JobScheduler jobScheduler = (JobScheduler)
                application.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo.Builder builder = new JobInfo.Builder(
                JOB_ID,
                new ComponentName(application.getPackageName(), JobSchedulerService.class.getName()));
        builder.setMinimumLatency(5000)// 设置任务运行最少延迟时间
                .setOverrideDeadline(60000)// 设置deadline，若到期还没有达到规定的条件则会开始执行
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)// 设置网络条件
                .setRequiresCharging(true)// 设置是否充电的条件
                .setRequiresDeviceIdle(false);// 设置手机是否空闲的条件
        int resultCode = jobScheduler.schedule(builder.build());
        if (JobScheduler.RESULT_FAILURE == resultCode) {
            Logger.e(TAG, "jobScheduler 失败");
        }
    }
}
