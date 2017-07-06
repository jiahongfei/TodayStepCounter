package com.today.step.lib;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;


/**
 * @ClassName: AppSharedPreferencesHelper
 * @Description: (公用类，用于缓存一些key——value类型的数据)
 */

public class AppSharedPreferencesHelper {

    public static final String APP_SHARD = "pah_share_prefs";

    //vitality 上一次计步器步数
    public static final String VITALITY_LAST_SENSOR_TIME = "vitality_last_sensor_time";
    //vitality 上一次系统运行时间
    public static final String VITALITY_LAST_RUNNING_TIME = "vitality_last_running_time";
    //vitality 第一次计步
    public static final String VITALITY_STEP_OFFSET = "vitality_step_first";
    //vitality 当前天数
    public static final String VITALITY_STEP_TODAY = "vitality_step_today";
    //SamSung Health三星S健康获取步数是否成功
    public static final String SAMSUNG_HEALTH_STEP_COUNTER = "samsung_health_step_counter";
    //SamSung Health三星S健康最后获取步数的时间
    public static final String SAMSUNG_HEALTH_LAST_TIME = "samsung_health_last_time";
    //SamSung Health三星S健康获取步数时间
    public static final String SAMSUNG_HEALTH_STEP_TIME = "samsung_health_step_time";

    private static SharedPreferences mSharedPreferences;
    private static AppSharedPreferencesHelper mInstance;
    private static Editor mEditor;
//
//    private AppSharedPreferencesHelper(Application application){
//        if(null == application){
//            return;
//        }
//        mSharedPreferences = application.getSharedPreferences(APP_SHARD, Context.MODE_PRIVATE);
//    }
//
//    public static AppSharedPreferencesHelper getInstance(Application application) {
//
//        if (mInstance == null) {
//            synchronized (AppSharedPreferencesHelper.class) {
//                if (null == mInstance) {
//                    mInstance = new AppSharedPreferencesHelper(application);
//                }
//            }
//        }
//        return mInstance;
//    }

    /**
     * Get SharedPreferences
     */
    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(APP_SHARD, Context.MODE_PRIVATE);
    }

    public static void setVitalityLastSystemRunningTime(Context context, long lastSystemRunningTime){
        getSharedPreferences(context).edit().putLong(VITALITY_LAST_RUNNING_TIME,lastSystemRunningTime).commit();
    }

    public static long getVitalityLastSystemRunningTime(Context context){
        return getSharedPreferences(context).getLong(VITALITY_LAST_RUNNING_TIME,0L);
    }

    public static void setVitalityLastSensorStep(Context context, float lastSensorStep){
        getSharedPreferences(context).edit().putFloat(VITALITY_LAST_SENSOR_TIME,lastSensorStep).commit();
    }

    public static float getVitalityLastSensorStep(Context context){
        return getSharedPreferences(context).getFloat(VITALITY_LAST_SENSOR_TIME,0.0f);
    }

    public static void setVitalityStepOffset(Context context, float stepOffset){
        getSharedPreferences(context).edit().putFloat(VITALITY_STEP_OFFSET,stepOffset).commit();
    }

    public static float getVitalityStepOffset(Context context){
        return getSharedPreferences(context).getFloat(VITALITY_STEP_OFFSET,0.0f);
    }

    public static void setVitalityStepToday(Context context, String stepToday){
        getSharedPreferences(context).edit().putString(VITALITY_STEP_TODAY,stepToday).commit();
    }

    public static String getVitalityStepToday(Context context){
        return getSharedPreferences(context).getString(VITALITY_STEP_TODAY,"");
    }

    public static void setSamsungHealthStepCounter(Context context, boolean shealth){
        getSharedPreferences(context).edit().putBoolean(SAMSUNG_HEALTH_STEP_COUNTER,shealth).commit();
    }

    public static boolean getSamsungHealthStepCounter(Context context){
        return getSharedPreferences(context).getBoolean(SAMSUNG_HEALTH_STEP_COUNTER,false);
    }

    public static void setSamsungHealthLastTime(Context context, long lastTime){
        getSharedPreferences(context).edit().putLong(SAMSUNG_HEALTH_LAST_TIME,lastTime).commit();
    }

    public static long getSamsungHealthLastTime(Context context){
        return getSharedPreferences(context).getLong(SAMSUNG_HEALTH_LAST_TIME,0L);
    }

    public static void setSamsungHealthStepTime(Context context, long lastTime){
        getSharedPreferences(context).edit().putLong(SAMSUNG_HEALTH_STEP_TIME,lastTime).commit();
    }

    public static long getSamsungHealthStepTime(Context context){
        return getSharedPreferences(context).getLong(SAMSUNG_HEALTH_STEP_TIME,0L);
    }

    public Editor getEditor() {

        if (mEditor == null) {
            mEditor = mSharedPreferences.edit();
        }
        return mEditor;
    }

    public void clear() {
        mEditor = getEditor();
        mEditor.clear();
        mEditor.commit();
    }


}
