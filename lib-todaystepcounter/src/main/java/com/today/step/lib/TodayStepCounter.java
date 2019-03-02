package com.today.step.lib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import com.andrjhf.lib.jlogger.JLoggerConstant;
import com.andrjhf.lib.jlogger.JLoggerWraper;

import java.util.HashMap;
import java.util.Map;

import static com.today.step.lib.ConstantDef.HANDLER_WHAT_TEST_JLOGGER;
import static com.today.step.lib.ConstantDef.WHAT_TEST_JLOGGER_DURATION;

/**
 * Sensor.TYPE_STEP_COUNTER
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

    private float mJLoggerSensorStep = 0f;
    private int mJLoggerCounterStep = 0;
    private int mJLoggerCurrStep = 0;
    private int mJLoggerOffsetStep = 0;
    /**
     * 传感器回调次数
     */
    private long mJLoggerSensorCount = 0;

    private final Handler sHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case HANDLER_WHAT_TEST_JLOGGER:{
                    HashMap<String,String> map = new HashMap<>();
                    map.put("sCurrStep",String.valueOf(mJLoggerCurrStep));
                    map.put("counterStep",String.valueOf(mJLoggerCounterStep));
                    map.put("SensorStep",String.valueOf(mJLoggerSensorStep));
                    map.put("sOffsetStep",String.valueOf(mJLoggerOffsetStep));
                    map.put("SensorCount",String.valueOf(mJLoggerSensorCount));
                    //增加电量、息屏状态
                    int battery = getBattery();
                    if(battery!=-1){
                        map.put("battery",String.valueOf(battery));
                    }
                    map.put("isScreenOn",String.valueOf(getScreenState()));
                    Log.e("wcd_map",map.toString());
                    JLoggerWraper.onEventInfo(mContext,JLoggerConstant.JLOGGER_TYPE_STEP_COUNTER_TIMER,map);
                    sHandler.removeMessages(HANDLER_WHAT_TEST_JLOGGER);
                    sHandler.sendEmptyMessageDelayed(HANDLER_WHAT_TEST_JLOGGER,WHAT_TEST_JLOGGER_DURATION);
                    break;
                }
            }
            return false;
        }
    });

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
        //开机启动监听到，一定是关机开机了
        boolean isShutdown =  shutdownBySystemRunningTime();
        if (mBoot || isShutdown) {
            mShutdown = true;
            PreferencesHelper.setShutdown(mContext, mShutdown);
        }
        HashMap<String,String> map = new HashMap<>();
        map.put("sCurrStep",String.valueOf(sCurrStep));
        map.put("mCleanStep",String.valueOf(mCleanStep));
        map.put("mTodayDate",String.valueOf(mTodayDate));
        map.put("sOffsetStep",String.valueOf(sOffsetStep));
        map.put("mShutdown",String.valueOf(mShutdown));
        map.put("isShutdown",String.valueOf(isShutdown));
        map.put("lastSensorStep",String.valueOf(PreferencesHelper.getLastSensorStep(mContext)));
        JLoggerWraper.onEventInfo(mContext,JLoggerConstant.JLOGGER_TYPE_STEP_CONSTRUCTOR,map);

        dateChangeCleanStep();

        initBroadcastReceiver();

        updateStepCounter();

        //启动JLogger日志打印
        sHandler.removeMessages(HANDLER_WHAT_TEST_JLOGGER);
        sHandler.sendEmptyMessageDelayed(HANDLER_WHAT_TEST_JLOGGER,WHAT_TEST_JLOGGER_DURATION);
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
                Map<String,String> map = new HashMap<>();
                map.put("clean_before_sCurrStep",String.valueOf(sCurrStep));
                map.put("clean_before_sOffsetStep",String.valueOf(sOffsetStep));
                map.put("clean_before_mCleanStep",String.valueOf(mCleanStep));
                cleanStep(counterStep);
                map.put("clean_after_sCurrStep",String.valueOf(sCurrStep));
                map.put("clean_after_sOffsetStep",String.valueOf(sOffsetStep));
                map.put("clean_after_mCleanStep",String.valueOf(mCleanStep));
                JLoggerWraper.onEventInfo(mContext,JLoggerConstant.JLOGGER_TYPE_STEP_CLEANSTEP,map);
            } else {
                //处理关机启动
                if (mShutdown || shutdownByCounterStep(counterStep)) {
                    Map<String,String> map = new HashMap<>();
                    map.put("shutdown_before_mShutdown",String.valueOf(mShutdown));
                    map.put("shutdown_before_mCounterStepReset",String.valueOf(mCounterStepReset));
                    map.put("shutdown_before_sOffsetStep",String.valueOf(sOffsetStep));
                    shutdown(counterStep);
                    map.put("shutdown_after_mShutdown",String.valueOf(mShutdown));
                    map.put("shutdown_after_mCounterStepReset",String.valueOf(mCounterStepReset));
                    map.put("shutdown_after_sOffsetStep",String.valueOf(sOffsetStep));
                    JLoggerWraper.onEventInfo(mContext,JLoggerConstant.JLOGGER_TYPE_STEP_SHUTDOWN,map);
                }
            }
            sCurrStep = counterStep - sOffsetStep;

            if (sCurrStep < 0) {
                Map<String,String> map = new HashMap<>();
                map.put("tolerance_before_counterStep",String.valueOf(counterStep));
                map.put("tolerance_before_sCurrStep",String.valueOf(sCurrStep));
                map.put("tolerance_before_sOffsetStep",String.valueOf(sOffsetStep));
                //容错处理，无论任何原因步数不能小于0，如果小于0，直接清零
                cleanStep(counterStep);
                map.put("tolerance_after_counterStep",String.valueOf(counterStep));
                map.put("tolerance_after_sCurrStep",String.valueOf(sCurrStep));
                map.put("tolerance_after_sOffsetStep",String.valueOf(sOffsetStep));
                JLoggerWraper.onEventInfo(mContext,JLoggerConstant.JLOGGER_TYPE_STEP_TOLERANCE,map);
            }

            PreferencesHelper.setCurrentStep(mContext, sCurrStep);
            PreferencesHelper.setElapsedRealtime(mContext, SystemClock.elapsedRealtime());
            PreferencesHelper.setLastSensorStep(mContext, counterStep);

            mJLoggerSensorStep = event.values[0];
            mJLoggerCounterStep = counterStep;
            mJLoggerCurrStep = sCurrStep;
            mJLoggerOffsetStep = sOffsetStep;
            updateStepCounter();
            if(mJLoggerSensorCount==0){
                sHandler.removeMessages(HANDLER_WHAT_TEST_JLOGGER);
                sHandler.sendEmptyMessageDelayed(HANDLER_WHAT_TEST_JLOGGER,800);
            }
            //用来判断传感器是否回调
            mJLoggerSensorCount++;

        }
    }

    private void cleanStep(int counterStep) {
        //清除步数，步数归零，优先级最高
        sCurrStep = 0;
        sOffsetStep = counterStep;
        PreferencesHelper.setStepOffset(mContext, sOffsetStep);

        mCleanStep = false;
        PreferencesHelper.setCleanStep(mContext, mCleanStep);
        mJLoggerCurrStep = sCurrStep;
        mJLoggerOffsetStep = sOffsetStep;
    }

    private void shutdown(int counterStep) {
        int tmpCurrStep = (int) PreferencesHelper.getCurrentStep(mContext);
        //重新设置offset
        sOffsetStep = counterStep - tmpCurrStep;
        //TODO 只有在当天进行过关机，才会进入到这，直接置反??@老大
//        sOffsetStep = -tmpCurrStep;
        PreferencesHelper.setStepOffset(mContext, sOffsetStep);

        mShutdown = false;
        PreferencesHelper.setShutdown(mContext, mShutdown);
    }

    private boolean shutdownByCounterStep(int counterStep) {
        if (mCounterStepReset) {
            //只判断一次
            mCounterStepReset = false;
            if (counterStep < PreferencesHelper.getLastSensorStep(mContext)) {
                JLoggerWraper.onEventInfo(mContext,JLoggerConstant.JLOGGER_TYPE_STEP_SHUTDOWNBYCOUNTERSTEP,"当前传感器步数小于上次传感器步数");
                //当前传感器步数小于上次传感器步数肯定是重新启动了，只是用来增加精度不是绝对的
//                Logger.e(TAG, "当前传感器步数小于上次传感器步数肯定是重新启动了，只是用来增加精度不是绝对的");
                return true;
            }
        }
        return false;
    }

    private boolean shutdownBySystemRunningTime() {
        if (PreferencesHelper.getElapsedRealtime(mContext) > SystemClock.elapsedRealtime()) {
            JLoggerWraper.onEventInfo(mContext,JLoggerConstant.JLOGGER_TYPE_STEP_SHUTDOWNBYSYSTEMRUNNINGTIME,"本地记录的时间，判断进行了关机操作");
            //上次运行的时间大于当前运行时间判断为重启，只是增加精度，极端情况下连续重启，会判断不出来
//            Logger.e(TAG, "上次运行的时间大于当前运行时间判断为重启，只是增加精度，极端情况下连续重启，会判断不出来");
            return true;
        }
        return false;
    }

    private synchronized void dateChangeCleanStep(){

        //时间改变了清零，或者0点分隔回调
        if (!getTodayDate().equals(mTodayDate) || mSeparate) {
            HashMap<String,String> map = new HashMap<>();
            map.put("getTodayDate()",String.valueOf(getTodayDate()));
            map.put("mTodayDate",mTodayDate);
            map.put("mSeparate",String.valueOf(mSeparate));
            JLoggerWraper.onEventInfo(mContext,JLoggerConstant.JLOGGER_TYPE_STEP_COUNTER_DATECHANGECLEANSTEP,map);
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

            mJLoggerSensorCount = 0;
            mJLoggerCurrStep = sCurrStep;

            if(null != mOnStepCounterListener){
                mOnStepCounterListener.onStepCounterClean();
            }
        }
    }

    private String getTodayDate() {
        return DateUtils.getCurrentDate("yyyy-MM-dd");
    }

    private void updateStepCounter() {

        //每次回调都判断一下是否跨天
        dateChangeCleanStep();

        if (null != mOnStepCounterListener) {
            mOnStepCounterListener.onChangeStepCounter(sCurrStep);
        }
    }

    public int getCurrentStep(){
        sCurrStep = (int) PreferencesHelper.getCurrentStep(mContext);
        return sCurrStep;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    private int getBattery(){
        BatteryManager batteryManager = (BatteryManager)mContext.getSystemService(Context.BATTERY_SERVICE);
        int battery = -1;
        if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.LOLLIPOP){
             battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        return battery;
    }
    private boolean getScreenState(){
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        return pm.isScreenOn();
    }
}
