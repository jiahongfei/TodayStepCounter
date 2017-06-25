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

    private static SharedPreferences mSharedPreferences;
    private static AppSharedPreferencesHelper mInstance;
    private static Editor mEditor;

    private AppSharedPreferencesHelper(Application application){
        if(null == application){
            return;
        }
        mSharedPreferences = application.getSharedPreferences(APP_SHARD, Context.MODE_PRIVATE);
    }

    public static AppSharedPreferencesHelper getInstance(Application application) {

        if (mInstance == null) {
            synchronized (AppSharedPreferencesHelper.class) {
                if (null == mInstance) {
                    mInstance = new AppSharedPreferencesHelper(application);
                }
            }
        }
        return mInstance;
    }

    public void setVitalityLastSystemRunningTime(long lastSystemRunningTime){
        mSharedPreferences.edit().putLong(VITALITY_LAST_RUNNING_TIME,lastSystemRunningTime).commit();
    }

    public long getVitalityLastSystemRunningTime(){
        return mSharedPreferences.getLong(VITALITY_LAST_RUNNING_TIME,0L);
    }

    public void setVitalityLastSensorStep(float lastSensorStep){
        mSharedPreferences.edit().putFloat(VITALITY_LAST_SENSOR_TIME,lastSensorStep).commit();
    }

    public float getVitalityLastSensorStep(){
        return mSharedPreferences.getFloat(VITALITY_LAST_SENSOR_TIME,0.0f);
    }

    public void setVitalityStepOffset(float stepOffset){
        mSharedPreferences.edit().putFloat(VITALITY_STEP_OFFSET,stepOffset).commit();
    }

    public float getVitalityStepOffset(){
        return mSharedPreferences.getFloat(VITALITY_STEP_OFFSET,0.0f);
    }

    public void setVitalityStepToday(String stepToday){
        mSharedPreferences.edit().putString(VITALITY_STEP_TODAY,stepToday).commit();
    }

    public String getVitalityStepToday(){
        return mSharedPreferences.getString(VITALITY_STEP_TODAY,"");
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
