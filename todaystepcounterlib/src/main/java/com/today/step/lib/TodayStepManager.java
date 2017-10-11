package com.today.step.lib;

import android.app.Application;
import android.content.Intent;

/**
 * 计步SDK初始化方法
 * Created by jiahongfei on 2017/10/9.
 */

public class TodayStepManager {

    /**
     * 在程序的最开始调用，最好在自定义的application oncreate中调用
     *
     * @param application
     */
    public static void init(Application application) {
        StepAlertManagerUtils.set0SeparateAlertManager(application);

        startTodayStepService(application);
    }

    public static void startTodayStepService(Application application) {
        Intent intent = new Intent(application, TodayStepService.class);
        application.startService(intent);
    }
}
