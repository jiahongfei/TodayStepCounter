package com.today.step.lib;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import static android.content.Context.ALARM_SERVICE;

/**
 * Created by jiahongfei on 2017/6/18.
 */
public class StepAlertManagerUtils {

    /**
     * 设置0点分隔Alert，当前天+1天的0点启动
     *
     * @param application
     */
    public static void set0SeparateAlertManager(Context application) {

        DateTime dateTime = new DateTime(System.currentTimeMillis()).plusDays(1);
        String tomorrow = dateTime.toString("yyyy-MM-dd");
        DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
        long timeInMillis = DateTime.parse(tomorrow+ " 00:00:00", format).getMillis();
//
//        long timeInMillis = new DateTime(System.currentTimeMillis()).plusMinutes(1).getMillis();

        AlarmManager alarmManager = (AlarmManager) application.getSystemService(ALARM_SERVICE);
        Intent i1 = new Intent(application, StepAlertReceive.class);
        i1.putExtra(VitalityStepService.INTENT_NAME_0_SEPARATE, true);
        i1.setAction(StepAlertReceive.ACTION_STEP_ALERT);
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
