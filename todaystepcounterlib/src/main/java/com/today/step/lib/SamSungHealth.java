package com.today.step.lib;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthDataService;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;
import com.samsung.android.sdk.healthdata.HealthResultHolder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by jiahongfei on 2017/6/30.
 */

public class SamSungHealth {

    private static final String TAG = "SamSungHealthStepCountReporter";

    public static final String STEP_DAILY_TREND = "com.samsung.shealth.step_daily_trend";

    public interface OnSamSungHealthConnectionListener extends OnStepCounterListener {

        void onConnectionFailed(HealthConnectionErrorResult error);
    }

    private static SamSungHealth sInstance;

    private Activity activity;
    private Application context;
    private HealthDataStore mStore;
    private HealthConnectionErrorResult mConnError;
    private Set<HealthPermissionManager.PermissionKey> mKeySet;
    private SamSungHealthStepCountReporter mReporter;

    private OnSamSungHealthConnectionListener mOnSamSungHealthConnectionListener;

    private SamSungHealth() {
    }

    public static SamSungHealth getInstance() {
        if (null == sInstance) {
            synchronized (SamSungHealth.class) {
                if (null == sInstance) {
                    sInstance = new SamSungHealth();
                }
            }
        }
        return sInstance;
    }

    public void initSamSungHealth(Application application) {
        this.context = application;
        mKeySet = new HashSet<>();
        mKeySet.add(new HealthPermissionManager.PermissionKey(HealthConstants.StepCount.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ));
        mKeySet.add(new HealthPermissionManager.PermissionKey( STEP_DAILY_TREND, HealthPermissionManager.PermissionType.READ));
        HealthDataService healthDataService = new HealthDataService();
        try {
            healthDataService.initialize(context);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create a HealthDataStore instance and set its listener
        mStore = new HealthDataStore(context, mConnectionListener);
        // Request the connection to the health data store
    }

    public void connectService(OnSamSungHealthConnectionListener onSamSungHealthConnectionListener) {
        this.mOnSamSungHealthConnectionListener = onSamSungHealthConnectionListener;
        if (null != mStore) {
            mStore.connectService();
        }
    }

    public void onDestroy() {
        if (null != mStore) {
            mStore.disconnectService();
        }
    }

    private final HealthDataStore.ConnectionListener mConnectionListener = new HealthDataStore.ConnectionListener() {

        @Override
        public void onConnected() {
            Logger.e(TAG, "Health data service is connected.");
            HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);
            mReporter = new SamSungHealthStepCountReporter(context, mStore);

            try {
                // Check whether the permissions that this application needs are acquired
                Map<HealthPermissionManager.PermissionKey, Boolean> resultMap = pmsManager.isPermissionAcquired(mKeySet);

                if (resultMap.containsValue(Boolean.FALSE)) {
                    // Request the permission for reading step counts if it is not acquired
//                    pmsManager.requestPermissions(mKeySet, activity).setResultListener(mPermissionListener);
                } else {
                    // Get the current step count and display it
                    mReporter.start(mInternalOnStepCounterListener);
                }
            } catch (Exception e) {
                Logger.e(TAG, e.getClass().getName() + " - " + e.getMessage());
                Logger.e(TAG, "Permission setting fails.");
            }
        }

        @Override
        public void onConnectionFailed(HealthConnectionErrorResult error) {
            AppSharedPreferencesHelper.setSamsungHealthStepCounter(context, false);
            Logger.d(TAG, "Health data service is not available.");
            showConnectionFailureDialog(error);
            if(null != mOnSamSungHealthConnectionListener){
                mOnSamSungHealthConnectionListener.onConnectionFailed(error);
            }
        }

        @Override
        public void onDisconnected() {
            Logger.d(TAG, "Health data service is disconnected.");
        }
    };

    private OnStepCounterListener mInternalOnStepCounterListener = new OnStepCounterListener() {

        @Override
        public void onChangeStepCounter(int step) {
            Logger.e(TAG, "SamSungHealth onChangeStepCounter : " + step);
            AppSharedPreferencesHelper.setSamsungHealthStepCounter(context, true);
            if(null != mOnSamSungHealthConnectionListener){
                mOnSamSungHealthConnectionListener.onChangeStepCounter(step);
            }
        }

        @Override
        public void onSaveStepCounter(int step, long millisecond) {
            Logger.e(TAG, "SamSungHealth onSaveStepCounter : " + step);
            if(null != mOnSamSungHealthConnectionListener){
                mOnSamSungHealthConnectionListener.onSaveStepCounter(step, millisecond);
            }
        }
    };

    private final HealthResultHolder.ResultListener<HealthPermissionManager.PermissionResult> mPermissionListener =
            new HealthResultHolder.ResultListener<HealthPermissionManager.PermissionResult>() {

                @Override
                public void onResult(HealthPermissionManager.PermissionResult result) {
                    Logger.d(TAG, "Permission callback is received.");
                    Map<HealthPermissionManager.PermissionKey, Boolean> resultMap = result.getResultMap();

                    if (resultMap.containsValue(Boolean.FALSE)) {
//                        showPermissionAlarmDialog();
                        Logger.e(TAG, "showPermissionAlarmDialog");
                    } else {
                        // Get the current step count and display it
                        mReporter.start(mInternalOnStepCounterListener);
                    }
                }
            };

    private void showConnectionFailureDialog(HealthConnectionErrorResult error) {

//        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        mConnError = error;
        String message = "Connection with Samsung Health is not available";

        if (mConnError.hasResolution()) {
            switch (error.getErrorCode()) {
                case HealthConnectionErrorResult.PLATFORM_NOT_INSTALLED:
                    message = "Please install Samsung Health";
                    break;
                case HealthConnectionErrorResult.OLD_VERSION_PLATFORM:
                    message = "Please upgrade Samsung Health";
                    break;
                case HealthConnectionErrorResult.PLATFORM_DISABLED:
                    message = "Please enable Samsung Health";
                    break;
                case HealthConnectionErrorResult.USER_AGREEMENT_NEEDED:
                    message = "Please agree with Samsung Health policy";
                    break;
                default:
                    message = "Please make Samsung Health available";
                    break;
            }
        }
        //Toast
        Logger.e(TAG, message);
//        alert.setMessage(message);
//
//        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int id) {
//                if (mConnError.hasResolution()) {
//                    mConnError.resolve(mInstance);
//                }
//            }
//        });
//
//        if (error.hasResolution()) {
//            alert.setNegativeButton("Cancel", null);
//        }
//
//        alert.show();
    }

}
