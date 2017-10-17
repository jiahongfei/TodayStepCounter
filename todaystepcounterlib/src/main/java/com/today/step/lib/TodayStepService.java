package com.today.step.lib;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TodayStepService extends Service {

    private static final String TAG = "TodayStepService";

    //回调30次保存一次数据库
    private static final int DB_SAVE_COUNTER = 30;

    //传感器的采样周期，这里使用SensorManager.SENSOR_DELAY_FASTEST，如果使用SENSOR_DELAY_UI会导致部分手机后台清理内存之后传感器不记步
    private static final int SAMPLING_PERIOD_US = SensorManager.SENSOR_DELAY_FASTEST;

    public static final String INTENT_NAME_0_SEPARATE = "intent_name_0_separate";
    public static final String INTENT_NAME_BOOT = "intent_name_boot";
    public static final String INTENT_JOB_SCHEDULER = "intent_job_scheduler";

    public static int CURRENT_SETP = 0;

    private WakeLock mWakeLock;
    private SensorManager sensorManager;
    private TodayStepDcretor stepDetector;
    private TodayStepCounter stepCounter;

    private NotificationManager nm;
    Notification notification;
    private NotificationCompat.Builder builder;

    private boolean mSeparate = false;
    private boolean mBoot = false;

    private int mDbSaveCount = 0;

    private TodayStepDBHelper mTodayStepDBHelper;

    @Override
    public void onCreate() {
        Logger.e(TAG, "onCreate:" + CURRENT_SETP);
        super.onCreate();

        mTodayStepDBHelper = new TodayStepDBHelper(getApplicationContext());

        sensorManager = (SensorManager) this
                .getSystemService(SENSOR_SERVICE);

        initNotification(CURRENT_SETP);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.e(TAG, "onStartCommand:" + CURRENT_SETP);

        if (null != intent) {
            mSeparate = intent.getBooleanExtra(INTENT_NAME_0_SEPARATE, false);
            mBoot = intent.getBooleanExtra(INTENT_NAME_BOOT, false);
        }

        updateNotification(CURRENT_SETP);

        //注册传感器
        startStepDetector();

        //TODO:测试数据Start
//        if(Logger.sIsDebug) {
//            if (!isStepCounter()) {
//                Toast.makeText(getApplicationContext(), "Lib 当前手机没有计步传感器", Toast.LENGTH_LONG).show();
//            } else {
//                Toast.makeText(getApplicationContext(), "Lib 当前手机使用计步传感器", Toast.LENGTH_LONG).show();
//
//            }
//        }
        //TODO:测试数据End

        return START_STICKY;
    }

    private void initNotification(int currentStep) {

        builder = new NotificationCompat.Builder(this);
        builder.setPriority(Notification.PRIORITY_MIN);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 100,
                new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);
        builder.setSmallIcon(R.mipmap.ic_launcher);// 设置通知小ICON

        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setTicker(getString(R.string.app_name));
        builder.setContentTitle(getString(R.string.title_notification_bar, String.valueOf(currentStep)));
        String km = getDistanceByStep(currentStep);
        String calorie = getCalorieByStep(currentStep);
        builder.setContentText(calorie + " 千卡  " + km + " 公里");

        //设置不可清除
        builder.setOngoing(true);
        notification = builder.build();
        //将Service设置前台，这里的id和notify的id一定要相同否则会出现后台清理内存Service被杀死通知还存在的bug
        startForeground(R.string.app_name, notification);
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(R.string.app_name, notification);

    }

    @Override
    public IBinder onBind(Intent intent) {
        Logger.e(TAG, "onBind:" + CURRENT_SETP);
        return mIBinder.asBinder();
    }

    private void startStepDetector() {

//        getLock(this);

        //android4.4以后如果有stepcounter可以使用计步传感器
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && isStepCounter()) {
            addStepCounterListener();
        } else {
            addBasePedoListener();
        }
    }

    private void addStepCounterListener() {
        Logger.e(TAG, "addStepCounterListener");
        if (null != stepCounter) {
            Logger.e(TAG, "已经注册TYPE_STEP_COUNTER");
            CURRENT_SETP = stepCounter.getCurrentStep();
            updateNotification(CURRENT_SETP);
            return;
        }
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (null == countSensor) {
            return;
        }
        stepCounter = new TodayStepCounter(getApplicationContext(), mOnStepCounterListener, mSeparate, mBoot);
        Logger.e(TAG, "countSensor");
        sensorManager.registerListener(stepCounter, countSensor, SAMPLING_PERIOD_US);
    }

    private void addBasePedoListener() {
        Logger.e(TAG, "addBasePedoListener");
        if (null != stepDetector) {
            Logger.e(TAG, "已经注册TYPE_ACCELEROMETER");
            CURRENT_SETP = stepDetector.getCurrentStep();
            updateNotification(CURRENT_SETP);
            return;
        }
        //没有计步器的时候开启定时器保存数据
        Sensor sensor = sensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (null == sensor) {
            return;
        }
        stepDetector = new TodayStepDcretor(this, mOnStepCounterListener);
        Log.e(TAG, "TodayStepDcretor");
        // 获得传感器的类型，这里获得的类型是加速度传感器
        // 此方法用来注册，只有注册过才会生效，参数：SensorEventListener的实例，Sensor的实例，更新速率
        sensorManager.registerListener(stepDetector, sensor, SAMPLING_PERIOD_US);
    }

    @Override
    public void onDestroy() {
        Logger.e(TAG, "onDestroy:" + CURRENT_SETP);

        Intent intent = new Intent(this, TodayStepService.class);
        startService(intent);
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Logger.e(TAG, "onUnbind:" + CURRENT_SETP);
        return super.onUnbind(intent);
    }

    /**
     * 步数每次回调的方法
     *
     * @param currentStep
     */
    private void updateTodayStep(int currentStep) {
        CURRENT_SETP = currentStep;
        updateNotification(CURRENT_SETP);
        saveDb(currentStep);
    }

    private void saveDb(int currentStep) {
        if (DB_SAVE_COUNTER > mDbSaveCount) {
            mDbSaveCount++;
            return;
        }
        mDbSaveCount = 0;

        Logger.e(TAG, "saveDb currentStep : " + currentStep);
        TodayStepData todayStepData = new TodayStepData();
        todayStepData.setToday(getTodayDate());
        todayStepData.setDate(System.currentTimeMillis());
        todayStepData.setStep(currentStep);
        if (null != mTodayStepDBHelper) {
            mTodayStepDBHelper.insert(todayStepData);
        }
    }

    private void cleanDb() {

        Logger.e(TAG, "cleanDb");

        mDbSaveCount = 0;
        if (null != mTodayStepDBHelper) {
            mTodayStepDBHelper.deleteTable();
            mTodayStepDBHelper.createTable();
        }
    }

    private String getTodayDate() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }

    /**
     * 更新通知
     */
    private void updateNotification(int stepCount) {
        if( null == builder || null == nm){
            return ;
        }
        builder.setContentTitle(getString(R.string.title_notification_bar, String.valueOf(stepCount)));
        String km = getDistanceByStep(stepCount);
        String calorie = getCalorieByStep(stepCount);
        builder.setContentText(calorie + " 千卡  " + km + " 公里");
        notification = builder.build();
        nm.notify(R.string.app_name, notification);
    }

    private boolean isStepCounter() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER);
    }

    private boolean isStepDetector() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_DETECTOR);
    }

    private OnStepCounterListener mOnStepCounterListener = new OnStepCounterListener() {
        @Override
        public void onChangeStepCounter(int step) {
            updateTodayStep(step);

        }

        @Override
        public void onStepCounterClean() {

            CURRENT_SETP = 0;
            updateNotification(CURRENT_SETP);

            cleanDb();
        }

    };

    private final ISportStepInterface.Stub mIBinder = new ISportStepInterface.Stub() {

        private static final String SPORT_DATE = "sportDate";
        private static final String STEP_NUM = "stepNum";
        private static final String DISTANCE = "km";
        private static final String CALORIE = "kaluli";

        @Override
        public int getCurrentTimeSportStep() throws RemoteException {
            return CURRENT_SETP;
        }

        @Override
        public String getTodaySportStepArray() throws RemoteException {
            if (null != mTodayStepDBHelper) {
                List<TodayStepData>  todayStepDataArrayList = mTodayStepDBHelper.getQueryAll();
                if(null == todayStepDataArrayList){
                    return null;
                }
                JSONArray jsonArray = new JSONArray();
                for (int i = 0; i<todayStepDataArrayList.size(); i++){
                    TodayStepData todayStepData = todayStepDataArrayList.get(i);
                    try {
                        JSONObject subObject = new JSONObject();
                        subObject.put(TodayStepDBHelper.TODAY,todayStepData.getToday());
                        subObject.put(SPORT_DATE,todayStepData.getDate());
                        subObject.put(STEP_NUM,todayStepData.getStep());
                        subObject.put(DISTANCE,getDistanceByStep(todayStepData.getStep()));
                        subObject.put(CALORIE,getCalorieByStep(todayStepData.getStep()));
                        jsonArray.put(subObject);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                Logger.e(TAG,jsonArray.toString());
                return jsonArray.toString();
            }
            return null;
        }
    };

    synchronized private WakeLock getLock(Context context) {
        if (mWakeLock != null) {
            if (mWakeLock.isHeld())
                mWakeLock.release();
            mWakeLock = null;
        }

        if (mWakeLock == null) {
            PowerManager mgr = (PowerManager) context
                    .getSystemService(Context.POWER_SERVICE);
            mWakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    TodayStepService.class.getName());
            mWakeLock.setReferenceCounted(true);
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(System.currentTimeMillis());
            int hour = c.get(Calendar.HOUR_OF_DAY);
            if (hour >= 23 || hour <= 6) {
                mWakeLock.acquire(5000);
            } else {
                mWakeLock.acquire(300000);
            }
        }
        return (mWakeLock);
    }

    // 公里计算公式
    public String getDistanceByStep(long steps) {
        return String.format("%.2f", steps * 0.6f / 1000);
    }

    // 千卡路里计算公式
    public String getCalorieByStep(long steps) {
        return String.format("%.1f", steps * 0.6f * 60 * 1.036f / 1000);
    }
}
