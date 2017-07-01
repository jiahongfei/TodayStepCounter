package com.today.step.lib;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.SystemClock;
import android.text.TextUtils;

import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jiahongfei on 2017/6/30.
 */

public class StepCounter implements SensorEventListener {

    private static final String TAG = "StepCounter";
    /**
     * 回调10次传感器监听保存一次数据
     */
    private static final int SAVE_STEP_COUNT = 10;

    private Context mContext;
    private OnStepCounterListener mOnStepCounterListener;

    private boolean mSeparate = false;
    private boolean mBoot = false;
    private Log4j mLog4j;
    private static int mSaveStepCount = 0;

    public StepCounter(Context context, boolean separate, boolean boot, Log4j log4j){
        this.mContext = context;
        this.mSeparate = separate;
        this.mBoot = boot;
        this.mLog4j = log4j;
    }

    public void setSeparate(boolean separate){
        this.mSeparate = separate;
    }

    public void setBoot(boolean boot){
        this.mBoot = boot;
    }

    public void setOnStepCounterListener(OnStepCounterListener onStepCounterListener){
        this.mOnStepCounterListener = onStepCounterListener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {

            if (mBoot) {
                mBoot = false;
                Logger.e(TAG, "boot 重启手机进行归并步数");
                mLog4j.e("boot 重启手机进行归并步数");
                resetSysMergeStep(event.values[0]);
                //测试通过
            } else {
                String stepToday = AppSharedPreferencesHelper.getVitalityStepToday(mContext);

                if (!TextUtils.isEmpty(stepToday) &&
                        (event.values[0] < AppSharedPreferencesHelper.getVitalityLastSensorStep(mContext))) {
                    Logger.e(TAG, "如果当前计步器的步数小于上次计步器的步数肯定是关机了");
                    mLog4j.e("如果当前计步器的步数小于上次计步器的步数肯定是关机了");
                    resetSysMergeStep(event.values[0]);
                    //测试通过

                } else if (!TextUtils.isEmpty(stepToday) &&
                        (AppSharedPreferencesHelper.getVitalityLastSystemRunningTime(mContext) > SystemClock.elapsedRealtime())) {
                    Logger.e(TAG, "上次系统运行时间如果大于当前系统运行时间有很大几率是关机了（这个只是个猜测值来提高精度的，实际上有可能当前系统运行时间超过上次运行时间）");
                    mLog4j.e("上次系统运行时间如果大于当前系统运行时间有很大几率是关机了（这个只是个猜测值来提高精度的，实际上有可能当前系统运行时间超过上次运行时间）");
                    resetSysMergeStep(event.values[0]);
                    //测试通过

                }
            }

            if (mSeparate) {
                //分隔时间，当app不在后台进程alertmanager启动app会进入
                mSeparate = false;
                AppSharedPreferencesHelper.setVitalityStepToday(mContext,getTodayDate());
                AppSharedPreferencesHelper.setVitalityStepOffset(mContext,event.values[0]);
                Logger.e(TAG, "mSeparate  =true");
                mLog4j.e("mSeparate  =true");
                //测试
            }

            String stepToday = AppSharedPreferencesHelper.getVitalityStepToday(mContext);
            if (TextUtils.isEmpty(stepToday)) {
                //第一次用手机app的时候记录一个offset
                AppSharedPreferencesHelper.setVitalityStepToday(mContext,getTodayDate());
                AppSharedPreferencesHelper.setVitalityStepOffset(mContext,event.values[0]);
                //测试
            } else {
                try {
                    if (1 == compareCurrAndToday(stepToday)) {
                        //当前时间大于上次记录时间，跨越0点，0点到开启App这段时间的数据会算为前一天的数据。如果跨越多天，都会算前一天的
                        mLog4j.e("跨越0点 event.value[0]：" + event.values[0]);
                        mLog4j.e("跨越0点 offset ：" + AppSharedPreferencesHelper.getVitalityStepOffset(mContext));

                        long preDay = DateUtils.getDateMillis(DateUtils.dateFormat(new DateTime(System.currentTimeMillis()).plusDays(-1).getMillis(), "yyyy-MM-dd") + " 23:59:59", "yyyy-MM-dd HH:mm:ss");
                        int step = (int) (event.values[0] - AppSharedPreferencesHelper.getVitalityStepOffset(mContext));
                        Logger.e(TAG, "跨越0点计算前一天时间 ：" + DateUtils.dateFormat(preDay, "yyyy-MM-dd HH:mm:ss"));
                        Logger.e(TAG, "跨越0点计算前一天步数 ：" + step);

                        mLog4j.e("跨越0点计算前一天时间 ：" + DateUtils.dateFormat(preDay, "yyyy-MM-dd HH:mm:ss"));
                        mLog4j.e("跨越0点计算前一天步数 ：" + step);
                        if(null != mOnStepCounterListener){
                            mOnStepCounterListener.onSaveStepCounter(step,preDay);
                        }

                        AppSharedPreferencesHelper.setVitalityStepToday(mContext,getTodayDate());
                        AppSharedPreferencesHelper.setVitalityStepOffset(mContext,event.values[0]);
                        //测试
                    }
                } catch (Exception e) {
                    Logger.e(TAG, "Exception e");
                }

            }

            float offset = AppSharedPreferencesHelper.getVitalityStepOffset(mContext);
            Logger.e(TAG, "offset : " + offset + "步");
            mLog4j.e("offset : " + offset + "步");

            float step = event.values[0];
            if (event.values[0] >= offset) {
                //在不关机的情况下一直减去这个offset得出下载软件到当前时间的步数
                step = event.values[0] - offset;
            }
            //做个容错如果步数计算小于0直接设置成0不能有负值
            if (step < 0) {
                Logger.e(TAG, "做个容错如果步数计算小于0直接设置成0不能有负值");
                mLog4j.e("做个容错如果步数计算小于0直接设置成0不能有负值");
                step = 0;
                AppSharedPreferencesHelper.setVitalityStepToday(mContext,getTodayDate());
                AppSharedPreferencesHelper.setVitalityStepOffset(mContext,event.values[0]);
            }

            Logger.e(TAG, "当前步数 : " + step + "步");
            Logger.e(TAG, "传感器步数 : " + event.values[0] + "步");

            mLog4j.e("当前步数 : " + step + "步");
            mLog4j.e( "传感器步数 : " + event.values[0] + "步");

            if(null != mOnStepCounterListener){
                mOnStepCounterListener.onChangeStepCounter((int) step);
            }

            saveStepData((int) step);

            AppSharedPreferencesHelper.setVitalityLastSensorStep(mContext,event.values[0]);
            AppSharedPreferencesHelper.setVitalityLastSystemRunningTime(mContext, SystemClock.elapsedRealtime());
            AppSharedPreferencesHelper.setVitalityStepToday(mContext,getTodayDate());

        }
    }

    private void resetSysMergeStep(float currSensorStep){
        String stepToday = AppSharedPreferencesHelper.getVitalityStepToday(mContext);
        float lastSensorStep = AppSharedPreferencesHelper.getVitalityLastSensorStep(mContext);
        float stepOffset = AppSharedPreferencesHelper.getVitalityStepOffset(mContext);
        if (0 == compareCurrAndToday(stepToday) ) {
            //当天
            float curStep = lastSensorStep - stepOffset;
            AppSharedPreferencesHelper.setVitalityStepOffset(mContext,-curStep);
            Logger.e(TAG,"当天重启手机合并步数");
            mLog4j.e("当天重启手机合并步数");
        } else {
            //系统重启隔天清零
            mLog4j.e("隔天清零");
            Logger.e(TAG,"隔天清零");
            AppSharedPreferencesHelper.setVitalityStepOffset(mContext,currSensorStep);
        }
        AppSharedPreferencesHelper.setVitalityStepToday(mContext,getTodayDate());
    }

    /**
     * 当前时间大于stepToday返回1，小于-1，等于0
     *
     * @param stepToday
     * @return
     */
    private int compareCurrAndToday(String stepToday) {
        DateTime todayTime = new DateTime(DateUtils.getDateMillis(stepToday, "yyyy-MM-dd"));
        DateTime currTime = new DateTime(DateUtils.getDateMillis(DateUtils.dateFormat(System.currentTimeMillis(), "yyyy-MM-dd"), "yyyy-MM-dd"));
        Logger.e(TAG, "todayTime ：" + todayTime.toString("yyyy-MM-dd HH:mm:ss"));
        Logger.e(TAG, "currTime ：" + currTime.toString("yyyy-MM-dd HH:mm:ss"));

        if (currTime.isAfter(todayTime)) {
            return 1;
        } else if (currTime.isBefore(todayTime)) {
            return -1;
        } else {
            return 0;
        }
    }

    private void saveStepData(int step) {
        if (mSaveStepCount >= SAVE_STEP_COUNT) {
            mSaveStepCount = 0;
            //走10步保存一次数据库
            Logger.e(TAG, "保存数据库 : " + step + "步");
            if(null != mOnStepCounterListener){
                mOnStepCounterListener.onSaveStepCounter(step, System.currentTimeMillis());
            }
        }
        mSaveStepCount++;
    }


    private String getTodayDate() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
