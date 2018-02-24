package com.today.step.lib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.PowerManager;
import android.os.SystemClock;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * android4.4  Sensor.TYPE_STEP_COUNTER
 * 计步传感器计算当天步数，不需要后台Service
 * Created by jiahongfei on 2017/6/30.
 */

class TodayStepCounter implements SensorEventListener {

    private static final String TAG = "TodayStepCounter";

    private int sOffsetStep = 0;
    private int sCurrStep = 0;
    private String mTodayDate;
    private boolean mCleanStep = true;
    private boolean mShutdown = false;
    /**
     * 用来标识对象第一次创建，
     */
    private boolean mCounterStepReset = true;

    private Context mContext;
    private OnStepCounterListener mOnStepCounterListener;

    private boolean mSeparate = false;
    private boolean mBoot = false;

    public TodayStepCounter(Context context, OnStepCounterListener onStepCounterListener, boolean separate, boolean boot) {
        this.mContext = context;
        this.mSeparate = separate;
        this.mBoot = boot;
        this.mOnStepCounterListener = onStepCounterListener;

        WakeLockUtils.getLock(mContext);

        sCurrStep = (int) PreferencesHelper.getCurrentStep(mContext);
        mCleanStep = PreferencesHelper.getCleanStep(mContext);
        mTodayDate = PreferencesHelper.getStepToday(mContext);
        sOffsetStep = (int) PreferencesHelper.getStepOffset(mContext);
        mShutdown = PreferencesHelper.getShutdown(mContext);
        Logger.e(TAG, "mShutdown : " + mShutdown);
        //开机启动监听到，一定是关机开机了
        if (mBoot || shutdownBySystemRunningTime()) {
            mShutdown = true;
            PreferencesHelper.setShutdown(mContext, mShutdown);
            Logger.e(TAG, "开机启动监听到");
        }

        dateChangeCleanStep();

        initBroadcastReceiver();

        updateStepCounter();

    }

    private void initBroadcastReceiver() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                if (Intent.ACTION_TIME_TICK.equals(intent.getAction())
                        || Intent.ACTION_TIME_CHANGED.equals(intent.getAction())) {

                    Logger.e(TAG, "ACTION_TIME_TICK");
                    //service存活做0点分隔
                    dateChangeCleanStep();

                }
            }
        };
        mContext.registerReceiver(mBatInfoReceiver, filter);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {

            int counterStep = (int) event.values[0];

            if (mCleanStep) {
                //TODO:只有传感器回调才会记录当前传感器步数，然后对当天步数进行清零，所以步数会少，少的步数等于传感器启动需要的步数，假如传感器需要10步进行启动，那么就少10步
                cleanStep(counterStep);
            } else {
                //处理关机启动
                if (mShutdown || shutdownByCounterStep(counterStep)) {
                    Logger.e(TAG, "onSensorChanged shutdown");
                    shutdown(counterStep);
                }
            }
            sCurrStep = counterStep - sOffsetStep;

            if (sCurrStep < 0) {
                //容错处理，无论任何原因步数不能小于0，如果小于0，直接清零
                Logger.e(TAG, "容错处理，无论任何原因步数不能小于0，如果小于0，直接清零");
                cleanStep(counterStep);
            }

            PreferencesHelper.setCurrentStep(mContext, sCurrStep);
            PreferencesHelper.setElapsedRealtime(mContext, SystemClock.elapsedRealtime());
            PreferencesHelper.setLastSensorStep(mContext, counterStep);

            Logger.e(TAG, "counterStep : " + counterStep + " --- " + "sOffsetStep : " + sOffsetStep + " --- " + "sCurrStep : " + sCurrStep);

            updateStepCounter();
        }
    }

    private void cleanStep(int counterStep) {
        //清除步数，步数归零，优先级最高
        sCurrStep = 0;
        sOffsetStep = counterStep;
        PreferencesHelper.setStepOffset(mContext, sOffsetStep);

        mCleanStep = false;
        PreferencesHelper.setCleanStep(mContext, mCleanStep);

        Logger.e(TAG, "mCleanStep : " + "清除步数，步数归零");
    }

    private void shutdown(int counterStep) {
        int tmpCurrStep = (int) PreferencesHelper.getCurrentStep(mContext);
        //重新设置offset
        sOffsetStep = counterStep - tmpCurrStep;
        PreferencesHelper.setStepOffset(mContext, sOffsetStep);

        mShutdown = false;
        PreferencesHelper.setShutdown(mContext, mShutdown);
    }

    private boolean shutdownByCounterStep(int counterStep) {
        if (mCounterStepReset) {
            //只判断一次
            if (counterStep < PreferencesHelper.getLastSensorStep(mContext)) {
                //当前传感器步数小于上次传感器步数肯定是重新启动了，只是用来增加精度不是绝对的
                Logger.e(TAG, "当前传感器步数小于上次传感器步数肯定是重新启动了，只是用来增加精度不是绝对的");
                return true;
            }
            mCounterStepReset = false;
        }
        return false;
    }

    private boolean shutdownBySystemRunningTime() {
        if (PreferencesHelper.getElapsedRealtime(mContext) > SystemClock.elapsedRealtime()) {
            //上次运行的时间大于当前运行时间判断为重启，只是增加精度，极端情况下连续重启，会判断不出来
            Logger.e(TAG, "上次运行的时间大于当前运行时间判断为重启，只是增加精度，极端情况下连续重启，会判断不出来");
            return true;
        }
        return false;
    }

    private synchronized void dateChangeCleanStep() {
        //时间改变了清零，或者0点分隔回调
        if (!getTodayDate().equals(mTodayDate) || mSeparate) {

            WakeLockUtils.getLock(mContext);

            mCleanStep = true;
            PreferencesHelper.setCleanStep(mContext, mCleanStep);

            mTodayDate = getTodayDate();
            PreferencesHelper.setStepToday(mContext, mTodayDate);

            mShutdown = false;
            PreferencesHelper.setShutdown(mContext, mShutdown);

            mBoot = false;

            mSeparate = false;

            sCurrStep = 0;
            PreferencesHelper.setCurrentStep(mContext, sCurrStep);

            if (null != mOnStepCounterListener) {
                mOnStepCounterListener.onStepCounterClean();
            }
        }
    }

    private String getTodayDate() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }

    private void updateStepCounter() {

        //每次回调都判断一下是否跨天
        dateChangeCleanStep();

        if (null != mOnStepCounterListener) {
            mOnStepCounterListener.onChangeStepCounter(sCurrStep);
        }
    }

    public int getCurrentStep() {
        sCurrStep = (int) PreferencesHelper.getCurrentStep(mContext);
        return sCurrStep;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
