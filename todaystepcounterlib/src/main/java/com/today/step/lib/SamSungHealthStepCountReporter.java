/**
 * Copyright (C) 2014 Samsung Electronics Co., Ltd. All rights reserved.
 * <p>
 * Mobile Communication Division,
 * Digital Media & Communications Business, Samsung Electronics Co., Ltd.
 * <p>
 * This software and its documentation are confidential and proprietary
 * information of Samsung Electronics Co., Ltd.  No part of the software and
 * documents may be copied, reproduced, transmitted, translated, or reduced to
 * any electronic medium or machine-readable form without the prior written
 * consent of Samsung Electronics.
 * <p>
 * Samsung Electronics makes no representations with respect to the contents,
 * and assumes no responsibility for any errors that might appear in the
 * software and documents. This publication and the contents hereof are subject
 * to change without notice.
 */

package com.today.step.lib;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;

import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthDataObserver;
import com.samsung.android.sdk.healthdata.HealthDataResolver;
import com.samsung.android.sdk.healthdata.HealthDataResolver.Filter;
import com.samsung.android.sdk.healthdata.HealthDataResolver.ReadRequest;
import com.samsung.android.sdk.healthdata.HealthDataResolver.ReadResult;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthResultHolder;

import org.joda.time.DateTime;

import java.util.Calendar;

public class SamSungHealthStepCountReporter {

    private static final String TAG = "SamSungHealthStepCountReporter";

    /**
     * 回调10次传感器监听保存一次数据
     */
    private static final int SAVE_STEP_COUNT = 10;

    private final HealthDataStore mStore;

    private Context context;

    private OnStepCounterListener mOnStepCounterListener;

    private static int mSaveStepCount = 0;

    private Log4j mLog4j = null;

    public SamSungHealthStepCountReporter(Application application, HealthDataStore store) {
        this.context = application;
        mStore = store;
        mLog4j = new Log4j(SamSungHealthStepCountReporter.class, Environment.getExternalStorageDirectory().getAbsolutePath() + "/SamSungHealthStepCountReporter.txt",false);
    }

    public void start(OnStepCounterListener onStepCounterListener) {
        this.mOnStepCounterListener = onStepCounterListener;
        // Register an observer to listen changes of step count and get today step count
        HealthDataObserver.addObserver(mStore, HealthConstants.StepCount.HEALTH_DATA_TYPE, mStepCountObserver);
        HealthDataObserver.addObserver(mStore, SamSungHealth.STEP_DAILY_TREND, mStepDailyTrendObserver);
        readTodayStepCount();
        readStepDailyTrend();
    }

    private void readStepDailyTrend() {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        // Set time range from start time of today to the current time
//        long startTime = new DateTime(System.currentTimeMillis()).plusDays(-100).getMillis();
//        long startTime = AppSharedPreferencesHelper.getSamsungHealthLastTime(context);
        //最后一次获取步数的时间
        long startTime = System.currentTimeMillis();
        long lastTime = AppSharedPreferencesHelper.getSamsungHealthLastTime(context);
        if(0 != lastTime) {
            startTime = lastTime;
        }
        startTime = getToday0Point();
        long endTime = System.currentTimeMillis();
        Filter filter = Filter.and(Filter.greaterThanEquals("day_time", startTime),
                Filter.lessThanEquals("day_time", endTime));
//      Create a filter for all source types
//        Filter filter = Filter.eq("source_type", -2);

        ReadRequest request = new ReadRequest.Builder()
                // Set the data type
                .setDataType(SamSungHealth.STEP_DAILY_TREND)
                // Set the source type with the filter
                .setFilter(filter)
                // Set the sort order
                .setSort("day_time", HealthDataResolver.SortOrder.DESC)
                // Build
                .build();

        try {
            resolver.read(request).setResultListener(mStepDailyTrendListener);
        } catch (Exception e) {
            Logger.e(TAG, e.getClass().getName() + " - " + e.getMessage());
        }
    }

    // Read the today's step count on demand
    private void readTodayStepCount() {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        // Set time range from start time of today to the current time
        long startTime = getStartTimeOfToday();
        long endTime = System.currentTimeMillis();
        Filter filter = Filter.and(Filter.greaterThanEquals(HealthConstants.StepCount.START_TIME, startTime),
                Filter.lessThanEquals(HealthConstants.StepCount.START_TIME, endTime));

        HealthDataResolver.ReadRequest stepCountRequest = new ReadRequest.Builder()
                .setDataType(HealthConstants.StepCount.HEALTH_DATA_TYPE)
                .setProperties(new String[]{
                        HealthConstants.StepCount.COUNT,
                        HealthConstants.StepCount.CREATE_TIME,
                        HealthConstants.StepCount.UPDATE_TIME,
                        HealthConstants.StepCount.START_TIME,
                        HealthConstants.StepCount.END_TIME})
                .setFilter(filter)
                .build();

        try {
            resolver.read(stepCountRequest).setResultListener(mStepCountListener);
        } catch (Exception e) {
            Logger.e(TAG, e.getClass().getName() + " - " + e.getMessage());
            Logger.e(TAG, "Getting step count fails.");
        }
    }

    private long getStartTimeOfToday() {
        Calendar today = Calendar.getInstance();
        if (0L == AppSharedPreferencesHelper.getSamsungHealthStepTime(context)) {
            //第一次安装app
            today.setTimeInMillis(System.currentTimeMillis());
            AppSharedPreferencesHelper.setSamsungHealthStepTime(context,System.currentTimeMillis());
        } else {
            //判断是否跨天，如果跨天重置开始时间
            if(0 != compareCurrAndToday(AppSharedPreferencesHelper.getSamsungHealthStepTime(context))){
                //跨天按照0点开始算步数
                AppSharedPreferencesHelper.setSamsungHealthStepTime(context, getToday0Point());
            }
            today.setTimeInMillis(AppSharedPreferencesHelper.getSamsungHealthStepTime(context));
        }
        return today.getTimeInMillis();

//        return new DateTime(System.currentTimeMillis()).plusDays(-1).getMillis();
//        return DateUtils.getDateMillis(DateUtils.dateFormat(System.currentTimeMillis(),"yyyy-MM-dd"),"yyyy-MM-dd");
    }

    private long getToday0Point(){
        Calendar tmpToday = Calendar.getInstance();
        tmpToday.set(Calendar.HOUR_OF_DAY, 0);
        tmpToday.set(Calendar.MINUTE, 0);
        tmpToday.set(Calendar.SECOND, 0);
        tmpToday.set(Calendar.MILLISECOND, 0);
        return tmpToday.getTimeInMillis();
    }

    private final HealthResultHolder.ResultListener<ReadResult> mStepCountListener = new HealthResultHolder.ResultListener<ReadResult>() {
        @Override
        public void onResult(ReadResult result) {
            int count = 0;
            long millis = 0L;
            Cursor c = null;

            try {
                c = result.getResultCursor();
                if (c != null) {
                    long stepTime = AppSharedPreferencesHelper.getSamsungHealthStepTime(context);
                    while (c.moveToNext()) {
                        int tmpCount = c.getInt(c.getColumnIndex(HealthConstants.StepCount.COUNT));
                        long tmpUpdateTime = c.getLong(c.getColumnIndex(HealthConstants.StepCount.UPDATE_TIME));
                        millis = tmpUpdateTime;
                        Logger.e(TAG, "tmpUpdateTime : " + DateUtils.dateFormat(tmpUpdateTime, "yyyy-MM-dd HH:mm:ss"));
                        Logger.e(TAG, "tmpCount : " + tmpCount);
                        mLog4j.e("tmpUpdateTime : " + DateUtils.dateFormat(tmpUpdateTime, "yyyy-MM-dd HH:mm:ss"));
                        mLog4j.e( "tmpCount : " + tmpCount);
                        if(0 != compareCurrAndToday(stepTime)){
                            count += tmpCount;
                            //清除计步器之前保存步数防止丢失
                            if (null != mOnStepCounterListener) {
                                mOnStepCounterListener.onSaveStepCounter(count, millis);
                            }
                            Logger.e(TAG,"计步器清零之前步数Count ： " + count);
                            mLog4j.e("计步器清零之前步数Count ： " + count);
                            AppSharedPreferencesHelper.setSamsungHealthStepTime(context,getToday0Point());
                            count = 0;
                            tmpCount = 0;
                            Logger.e(TAG,"跨天，计步器清零");
                            mLog4j.e("跨天，计步器清零");
                        }
                        count += tmpCount;
                        Logger.e(TAG, "count : " + count);
                        mLog4j.e("count : " + count);
                        Logger.e(TAG, "-------------------------------------");
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
            if (null != mOnStepCounterListener) {
                mOnStepCounterListener.onChangeStepCounter(count);
            }
            saveStepData(count, millis);

            //每次返回步数记录一下最后时间，用于下次启动返回时间差的步数
            AppSharedPreferencesHelper.setSamsungHealthLastTime(context, System.currentTimeMillis());
        }
    };

    private final HealthResultHolder.ResultListener<ReadResult> mStepDailyTrendListener =
            new HealthResultHolder.ResultListener<ReadResult>() {

                @Override
                public void onResult(ReadResult result) {

                    Cursor c = null;

                    try {
                        c = result.getResultCursor();
                        if (c != null) {
                            while (c.moveToNext()) {
//                                long dayTime = c.getLong(c.getColumnIndex("day_time"));
                                int stepCount = c.getInt(c.getColumnIndex("count"));
//                                long createTime = c.getLong(c.getColumnIndex("create_time"));
                                long updateTime = c.getLong(c.getColumnIndex("update_time"));

//                                Logger.d(TAG, "dayTime : " + DateUtils.dateFormat(dayTime, "yyyy-MM-dd HH:mm:ss"));
//                                Logger.d(TAG, "createTime : " + DateUtils.dateFormat(createTime, "yyyy-MM-dd HH:mm:ss"));
                                Logger.d(TAG, "updateTime : " + DateUtils.dateFormat(updateTime, "yyyy-MM-dd HH:mm:ss"));
                                Logger.d(TAG, "Step Count: " + stepCount);
                                mLog4j.d("updateTime : " + DateUtils.dateFormat(updateTime, "yyyy-MM-dd HH:mm:ss"));
                                mLog4j.d("Step Count: " + stepCount);
                                if (null != mOnStepCounterListener) {
                                    mOnStepCounterListener.onSaveStepCounter(stepCount, updateTime);
                                }
                                Logger.d(TAG, "-------------------------------------");
                                mLog4j.d("-------------------------------------");
                            }
                        } else {
                            Logger.d(TAG, "The curor is null.");
                        }
                    } catch (Exception e) {
                        Logger.d(TAG, e.getClass().getName() + " - " + e.getMessage());
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }
                }
            };

    private final HealthDataObserver mStepCountObserver = new HealthDataObserver(null) {

        // Update the step count when a change event is received
        @Override
        public void onChange(String dataTypeName) {
            Logger.e(TAG, "mStepCountObserver Observer receives a data changed event");
            mLog4j.e("mStepCountObserver Observer receives a data changed event");
            readTodayStepCount();
        }
    };

    private final HealthDataObserver mStepDailyTrendObserver = new HealthDataObserver(null) {

        // Update the step count when a change event is received
        @Override
        public void onChange(String dataTypeName) {
            Logger.e(TAG, "mStepDailyTrendObserver Observer receives a data changed event");
            mLog4j.e("mStepDailyTrendObserver Observer receives a data changed event");
            readStepDailyTrend();
        }
    };

    private void saveStepData(int step, long millis) {
        if (mSaveStepCount >= SAVE_STEP_COUNT) {
            mSaveStepCount = 0;
            //走10步保存一次数据库
            Logger.e(TAG, "保存数据库 : " + step + "步");
            if (null != mOnStepCounterListener) {
                mOnStepCounterListener.onSaveStepCounter(step, millis);
            }
        }
        mSaveStepCount++;
    }

    /**
     * 当前时间大于stepToday返回1，小于-1，等于0
     *
     * @param millis
     * @return
     */
    private int compareCurrAndToday(long millis) {
        DateTime todayTime = new DateTime(DateUtils.getDateMillis(DateUtils.dateFormat(millis, "yyyy-MM-dd"), "yyyy-MM-dd"));
        DateTime currTime = new DateTime(DateUtils.getDateMillis(DateUtils.dateFormat(System.currentTimeMillis(), "yyyy-MM-dd"), "yyyy-MM-dd"));
//        Logger.e(TAG, "todayTime ：" + todayTime.toString("yyyy-MM-dd HH:mm:ss"));
//        Logger.e(TAG, "currTime ：" + currTime.toString("yyyy-MM-dd HH:mm:ss"));

        if (currTime.isAfter(todayTime)) {
            return 1;
        } else if (currTime.isBefore(todayTime)) {
            return -1;
        } else {
            return 0;
        }
    }
}
