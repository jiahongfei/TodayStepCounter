package com.today.step.lib;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

import static android.content.Context.ALARM_SERVICE;

/**
 * Created by jiahongfei on 2017/6/18.
 */
class StepAlertManagerUtils {

    private static final String TAG = "StepAlertManagerUtils";

    /**
     * 设置0点分隔Alert，当前天+1天的0点启动
     *
     * @param application
     */
    public static void set0SeparateAlertManager(Context application) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.DAY_OF_YEAR,1);
        String tomorrow = DateUtils.dateFormat(calendar.getTimeInMillis(),"yyyy-MM-dd");
        long timeInMillis = DateUtils.getDateMillis(tomorrow+ " 00:00:00","yyyy-MM-dd HH:mm:ss");

        Logger.e(TAG, DateUtils.dateFormat(timeInMillis,"yyyy-MM-dd HH:mm:ss"));

        AlarmManager alarmManager = (AlarmManager) application.getSystemService(ALARM_SERVICE);
        Intent i1 = new Intent(application, TodayStepAlertReceive.class);
        i1.putExtra(TodayStepService.INTENT_NAME_0_SEPARATE, true);
        i1.setAction(TodayStepAlertReceive.ACTION_STEP_ALERT);
        PendingIntent operation = PendingIntent.getBroadcast(application, 0, i1, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, operation);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, operation);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, operation);
        }

    }
}
