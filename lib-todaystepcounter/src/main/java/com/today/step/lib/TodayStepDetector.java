package com.today.step.lib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Handler;
import android.os.Message;


import com.andrjhf.lib.jlogger.JLoggerConstant;
import com.andrjhf.lib.jlogger.JLoggerWraper;

import java.util.HashMap;
import java.util.Map;

import static com.today.step.lib.ConstantDef.HANDLER_WHAT_TEST_JLOGGER;
import static com.today.step.lib.ConstantDef.WHAT_TEST_JLOGGER_DURATION;

/**
 * Sensor.TYPE_ACCELEROMETER
 * 加速度传感器计算当天步数，需要保持后台Service
 */
public class TodayStepDetector implements SensorEventListener{

    private final String TAG = "TodayStepDetector";

    //存放三轴数据
    float[] oriValues = new float[3];
    final int ValueNum = 4;
    //用于存放计算阈值的波峰波谷差值
    float[] tempValue = new float[ValueNum];
    int tempCount = 0;
    //是否上升的标志位
    boolean isDirectionUp = false;
    //持续上升次数
    int continueUpCount = 0;
    //上一点的持续上升的次数，为了记录波峰的上升次数
    int continueUpFormerCount = 0;
    //上一点的状态，上升还是下降
    boolean lastStatus = false;
    //波峰值
    float peakOfWave = 0;
    //波谷值
    float valleyOfWave = 0;
    //此次波峰的时间
    long timeOfThisPeak = 0;
    //上次波峰的时间
    long timeOfLastPeak = 0;
    //当前的时间
    long timeOfNow = 0;
    //当前传感器的值
    float gravityNew = 0;
    //上次传感器的值
    float gravityOld = 0;
    //动态阈值需要动态的数据，这个值用于这些动态数据的阈值
    final float InitialValue = (float) 1.3;
    //初始阈值
    float ThreadValue = (float) 2.0;
    //波峰波谷时间差
    int TimeInterval = 250;

    private int count = 0;
    private int mCount = 0;
    private OnStepCounterListener mOnStepCounterListener;
    private Context mContext;
    private long timeOfLastPeak1 = 0;
    private long timeOfThisPeak1 = 0;
    private String mTodayDate;

    /**
     * 传感器回调次数
     */
    private int mJLoggerSensorCount = 0;

    private final Handler sHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case HANDLER_WHAT_TEST_JLOGGER:{
                    Map<String,String> map = new HashMap<>();
                    map.put("mCount",String.valueOf(mCount));
                    map.put("count",String.valueOf(count));
                    map.put("mJLoggerSensorCount",String.valueOf(mJLoggerSensorCount));
                    JLoggerWraper.onEventInfo(mContext,JLoggerConstant.JLOGGER_TYPE_ACCELEROMETER_TIMER,map);
//                    JLogger.i(TAG,"onSensorChanged mCount : " +  mCount +
//                            "   this.count :" + count +
//                            "   SensorCount : " + mJLoggerSensorCount);

                    sHandler.removeMessages(HANDLER_WHAT_TEST_JLOGGER);
                    sHandler.sendEmptyMessageDelayed(HANDLER_WHAT_TEST_JLOGGER,WHAT_TEST_JLOGGER_DURATION);
                    break;
                }
            }
            return false;
        }
    });

    public TodayStepDetector(Context context, OnStepCounterListener onStepCounterListener){
        super();
        mContext = context;
        this.mOnStepCounterListener = onStepCounterListener;

        WakeLockUtils.getLock(mContext);

        mCount = (int) PreferencesHelper.getCurrentStep(mContext);
        mTodayDate = PreferencesHelper.getStepToday(mContext);
        Map<String,String> map = new HashMap<>();
        map.put("mCount",String.valueOf(mCount));
        map.put("mTodayDate",String.valueOf(mTodayDate));
        JLoggerWraper.onEventInfo(mContext,JLoggerConstant.JLOGGER_TYPE_ACCELEROMETER_CONSTRUCTOR,map);
//        JLogger.i(TAG, "TodayStepDetector mCount : " + mCount +
//                "   mTodayDate:" + mTodayDate);

        dateChangeCleanStep();
        initBroadcastReceiver();

        updateStepCounter();

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

    private synchronized void dateChangeCleanStep() {
        //时间改变了清零，或者0点分隔回调
        if (!getTodayDate().equals(mTodayDate)) {

            WakeLockUtils.getLock(mContext);

            mCount = 0;
            PreferencesHelper.setCurrentStep(mContext, mCount);

            mTodayDate = getTodayDate();
            PreferencesHelper.setStepToday(mContext, mTodayDate);

            setSteps(0);
            JLoggerWraper.onEventInfo(mContext,JLoggerConstant.JLOGGER_TYPE_ACCELEROMETER_DATECHANGECLEANSTEP);
            mJLoggerSensorCount = 0;

            if(null != mOnStepCounterListener){
                mOnStepCounterListener.onStepCounterClean();
            }
        }
    }

    private String getTodayDate() {
        return DateUtils.getCurrentDate("yyyy-MM-dd");
    }

    private void updateStepCounter(){

        //每次回调都判断一下是否跨天
        dateChangeCleanStep();

        if (null != mOnStepCounterListener) {
            mOnStepCounterListener.onChangeStepCounter(mCount);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(Sensor.TYPE_ACCELEROMETER == event.sensor.getType()) {
            for (int i = 0; i < 3; i++) {
                oriValues[i] = event.values[i];
            }
            gravityNew = (float) Math.sqrt(oriValues[0] * oriValues[0]
                    + oriValues[1] * oriValues[1] + oriValues[2] * oriValues[2]);
            detectorNewStep(gravityNew);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //
    }

    /*
    * 检测步子，并开始计步
    * 1.传入sersor中的数据
    * 2.如果检测到了波峰，并且符合时间差以及阈值的条件，则判定为1步
    * 3.符合时间差条件，波峰波谷差值大于initialValue，则将该差值纳入阈值的计算中
    * */
    private void detectorNewStep(float values) {
        if (gravityOld == 0) {
            gravityOld = values;
        } else {
            if (detectorPeak(values, gravityOld)) {
                timeOfLastPeak = timeOfThisPeak;
                timeOfNow = System.currentTimeMillis();
                if (timeOfNow - timeOfLastPeak >= TimeInterval
                        && (peakOfWave - valleyOfWave >= ThreadValue)) {
                    timeOfThisPeak = timeOfNow;
                    /*
                     * 更新界面的处理，不涉及到算法
                     * 一般在通知更新界面之前，增加下面处理，为了处理无效运动：
                     * 1.连续记录10才开始计步
                     * 2.例如记录的9步用户停住超过3秒，则前面的记录失效，下次从头开始
                     * 3.连续记录了9步用户还在运动，之前的数据才有效
                     * */
                    countStep();
                }
                if (timeOfNow - timeOfLastPeak >= TimeInterval
                        && (peakOfWave - valleyOfWave >= InitialValue)) {
                    timeOfThisPeak = timeOfNow;
                    ThreadValue = peakValleyThread(peakOfWave - valleyOfWave);
                }
            }
        }
        gravityOld = values;
    }

    /*
     * 检测波峰
     * 以下四个条件判断为波峰：
     * 1.目前点为下降的趋势：isDirectionUp为false
     * 2.之前的点为上升的趋势：lastStatus为true
     * 3.到波峰为止，持续上升大于等于2次
     * 4.波峰值大于20
     * 记录波谷值
     * 1.观察波形图，可以发现在出现步子的地方，波谷的下一个就是波峰，有比较明显的特征以及差值
     * 2.所以要记录每次的波谷值，为了和下次的波峰做对比
     * */
    private boolean detectorPeak(float newValue, float oldValue) {
        lastStatus = isDirectionUp;
        if (newValue >= oldValue) {
            isDirectionUp = true;
            continueUpCount++;
        } else {
            continueUpFormerCount = continueUpCount;
            continueUpCount = 0;
            isDirectionUp = false;
        }

        if (!isDirectionUp && lastStatus
                && (continueUpFormerCount >= 2 || oldValue >= 20)) {
            peakOfWave = oldValue;
            return true;
        } else if (!lastStatus && isDirectionUp) {
            valleyOfWave = oldValue;
            return false;
        } else {
            return false;
        }
    }

    /*
     * 阈值的计算
     * 1.通过波峰波谷的差值计算阈值
     * 2.记录4个值，存入tempValue[]数组中
     * 3.在将数组传入函数averageValue中计算阈值
     * */
    private float peakValleyThread(float value) {
        float tempThread = ThreadValue;
        if (tempCount < ValueNum) {
            tempValue[tempCount] = value;
            tempCount++;
        } else {
            tempThread = averageValue(tempValue, ValueNum);
            for (int i = 1; i < ValueNum; i++) {
                tempValue[i - 1] = tempValue[i];
            }
            tempValue[ValueNum - 1] = value;
        }
        return tempThread;

    }

    /*
     * 梯度化阈值
     * 1.计算数组的均值
     * 2.通过均值将阈值梯度化在一个范围里
     * */
    private float averageValue(float value[], int n) {
        float ave = 0;
        for (int i = 0; i < n; i++) {
            ave += value[i];
        }
        ave = ave / ValueNum;
        if (ave >= 8)
            ave = (float) 4.3;
        else if (ave >= 7 && ave < 8)
            ave = (float) 3.3;
        else if (ave >= 4 && ave < 7)
            ave = (float) 2.3;
        else if (ave >= 3 && ave < 4)
            ave = (float) 2.0;
        else {
            ave = (float) 1.3;
        }
        return ave;
    }




    /*
    * 连续走十步才会开始计步
    * 连续走了9步以下,停留超过3秒,则计数清空
    * */
    private void countStep() {
        this.timeOfLastPeak1 = this.timeOfThisPeak1;
        this.timeOfThisPeak1 = System.currentTimeMillis();
        if (this.timeOfThisPeak1 - this.timeOfLastPeak1 <= 3000L){
            if(this.count<9){
                this.count++;
            }else if(this.count == 9){
                this.count++;
                this.mCount += this.count;
                PreferencesHelper.setCurrentStep(mContext, mCount);
                updateStepCounter();
            }else{
                this.mCount++;
                PreferencesHelper.setCurrentStep(mContext, mCount);
                updateStepCounter();
            }
        }else{//超时
            this.count = 1;//为1,不是0
        }
        //测试传感器是否回调
        mJLoggerSensorCount++;
    }


    private void setSteps(int initValue) {
        this.mCount = initValue;
        this.count = 0;
        timeOfLastPeak1 = 0;
        timeOfThisPeak1 = 0;
    }

    public int getCurrentStep() {
        return mCount;
    }

    public void setCurrentStep(int initStep){

        setSteps(initStep);

        mCount = initStep;
        PreferencesHelper.setCurrentStep(mContext, mCount);

        mTodayDate = getTodayDate();
        PreferencesHelper.setStepToday(mContext, mTodayDate);

        if(null != mOnStepCounterListener){
            mOnStepCounterListener.onChangeStepCounter(mCount);
        }
    }
}
