package com.today.step.lib;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.today.step.lib.SportStepJsonUtils.getCalorieByStep;
import static com.today.step.lib.SportStepJsonUtils.getDistanceByStep;

public class TodayStepService extends Service implements Handler.Callback {

    private static final String TAG = "TodayStepService";

    /**
     * 数据库中保存多少天的运动数据
     */
    private static final int DB_LIMIT = 2;

    //保存数据库频率
    private static final int DB_SAVE_COUNTER = 50;

    //传感器的采样周期，这里使用SensorManager.SENSOR_DELAY_FASTEST，如果使用SENSOR_DELAY_UI会导致部分手机后台清理内存之后传感器不记步
    private static final int SAMPLING_PERIOD_US = SensorManager.SENSOR_DELAY_FASTEST;

    private static final int HANDLER_WHAT_SAVE_STEP = 0;
    //如果走路如果停止，10秒钟后保存数据库
    private static final int LAST_SAVE_STEP_DURATION = 10*1000;

    private static final int BROADCAST_REQUEST_CODE = 100;

    public static final String INTENT_NAME_0_SEPARATE = "intent_name_0_separate";
    public static final String INTENT_NAME_BOOT = "intent_name_boot";
    public static final String INTENT_JOB_SCHEDULER = "intent_job_scheduler";

    public static int CURRENT_SETP = 0;

    private SensorManager sensorManager;
    //    private TodayStepDcretor stepDetector;
    private TodayStepDetector mStepDetector;
    private TodayStepCounter stepCounter;

    private NotificationManager nm;
    Notification notification;
    private NotificationCompat.Builder builder;

    private boolean mSeparate = false;
    private boolean mBoot = false;

    private int mDbSaveCount = 0;

    private ITodayStepDBHelper mTodayStepDBHelper;

    private final Handler sHandler = new Handler(this);

    private Microlog4Android mMicrolog4Android = new Microlog4Android();

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case HANDLER_WHAT_SAVE_STEP: {
                Logger.e(TAG, "HANDLER_WHAT_SAVE_STEP");

                microlog4AndroidError("HANDLER_WHAT_SAVE_STEP");

                mDbSaveCount = 0;

                saveDb(true, CURRENT_SETP);
                break;
            }
            default:
                break;
        }
        return false;
    }

    @Override
    public void onCreate() {
        Logger.e(TAG, "onCreate:" + CURRENT_SETP);
        super.onCreate();

        mTodayStepDBHelper = TodayStepDBHelper.factory(getApplicationContext());

        sensorManager = (SensorManager) this
                .getSystemService(SENSOR_SERVICE);

        initNotification(CURRENT_SETP);

        if(null != mMicrolog4Android) {
            mMicrolog4Android.configure(this);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.e(TAG, "onStartCommand:" + CURRENT_SETP);

        if (null != intent) {
            mSeparate = intent.getBooleanExtra(INTENT_NAME_0_SEPARATE, false);
            mBoot = intent.getBooleanExtra(INTENT_NAME_BOOT, false);
        }

        mDbSaveCount = 0;

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

        microlog4AndroidError("onStartCommand");

        return START_STICKY;
    }

    private void initNotification(int currentStep) {

        builder = new NotificationCompat.Builder(this);
        builder.setPriority(Notification.PRIORITY_MIN);

        String receiverName = getReceiver(getApplicationContext());
        PendingIntent contentIntent = PendingIntent.getBroadcast(this, BROADCAST_REQUEST_CODE, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        if (!TextUtils.isEmpty(receiverName)) {
            try {
                contentIntent = PendingIntent.getBroadcast(this, BROADCAST_REQUEST_CODE, new Intent(this, Class.forName(receiverName)), PendingIntent.FLAG_UPDATE_CURRENT);
            } catch (Exception e) {
                e.printStackTrace();
                contentIntent = PendingIntent.getBroadcast(this, BROADCAST_REQUEST_CODE, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
            }
        }
        builder.setContentIntent(contentIntent);
        int smallIcon = getResources().getIdentifier("icon_step_small", "mipmap", getPackageName());
        if (0 != smallIcon) {
            Logger.e(TAG, "smallIcon");
            builder.setSmallIcon(smallIcon);
        } else {
            builder.setSmallIcon(R.mipmap.ic_notification_default);// 设置通知小ICON
        }
        int largeIcon = getResources().getIdentifier("icon_step_large", "mipmap", getPackageName());
        if (0 != largeIcon) {
            Logger.e(TAG, "largeIcon");
            builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), largeIcon));
        } else {
            builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_notification_default));

        }
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
            WakeLockUtils.getLock(this);
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
        if (null != mStepDetector) {
            WakeLockUtils.getLock(this);
            Logger.e(TAG, "已经注册TYPE_ACCELEROMETER");
            CURRENT_SETP = mStepDetector.getCurrentStep();
            updateNotification(CURRENT_SETP);
            return;
        }
        //没有计步器的时候开启定时器保存数据
        Sensor sensor = sensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (null == sensor) {
            return;
        }
        mStepDetector = new TodayStepDetector(this, mOnStepCounterListener);
        Log.e(TAG, "TodayStepDcretor");
        // 获得传感器的类型，这里获得的类型是加速度传感器
        // 此方法用来注册，只有注册过才会生效，参数：SensorEventListener的实例，Sensor的实例，更新速率
        sensorManager.registerListener(mStepDetector, sensor, SAMPLING_PERIOD_US);
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

        microlog4AndroidError("   currentStep : " + currentStep);

        CURRENT_SETP = currentStep;
        updateNotification(CURRENT_SETP);
        saveStep(currentStep);
    }

    private void saveStep(int currentStep) {
        sHandler.removeMessages(HANDLER_WHAT_SAVE_STEP);
        sHandler.sendEmptyMessageDelayed(HANDLER_WHAT_SAVE_STEP, LAST_SAVE_STEP_DURATION);

        microlog4AndroidError("   mDbSaveCount : " + mDbSaveCount);

        if (DB_SAVE_COUNTER > mDbSaveCount) {
            mDbSaveCount++;
            return;
        }
        mDbSaveCount = 0;

        saveDb(false, currentStep);
    }

    /**
     * @param handler     true handler回调保存步数，否false
     * @param currentStep
     */
    private void saveDb(boolean handler, int currentStep) {

        TodayStepData todayStepData = new TodayStepData();
        todayStepData.setToday(getTodayDate());
        todayStepData.setDate(System.currentTimeMillis());
        todayStepData.setStep(currentStep);
        if (null != mTodayStepDBHelper) {
            Logger.e(TAG, "saveDb handler : " + handler);
            if (!handler || !mTodayStepDBHelper.isExist(todayStepData)) {
                Logger.e(TAG, "saveDb currentStep : " + currentStep);

                microlog4AndroidError("saveDb currentStep : " + currentStep);

                mTodayStepDBHelper.insert(todayStepData);
            }
        }
    }

    private void cleanDb() {

        Logger.e(TAG, "cleanDb");

        mDbSaveCount = 0;

        if (null != mTodayStepDBHelper) {
            mTodayStepDBHelper.clearCapacity(DateUtils.dateFormat(System.currentTimeMillis(), "yyyy-MM-dd"), DB_LIMIT);
        }

//        if (null != mTodayStepDBHelper) {
        //保存多天的步数
//            mTodayStepDBHelper.deleteTable();
//            mTodayStepDBHelper.createTable();
//        }
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
        if (null == builder || null == nm) {
            return;
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

        @Override
        public int getCurrentTimeSportStep() throws RemoteException {
            return CURRENT_SETP;
        }

        private JSONArray getSportStepJsonArray(List<TodayStepData> todayStepDataArrayList) {
            return SportStepJsonUtils.getSportStepJsonArray(todayStepDataArrayList);
        }

        @Override
        public String getTodaySportStepArray() throws RemoteException {
            if (null != mTodayStepDBHelper) {
                List<TodayStepData> todayStepDataArrayList = mTodayStepDBHelper.getQueryAll();
                JSONArray jsonArray = getSportStepJsonArray(todayStepDataArrayList);
                Logger.e(TAG, jsonArray.toString());
                return jsonArray.toString();
            }
            return null;
        }

        @Override
        public String getTodaySportStepArrayByDate(String date) throws RemoteException {
            if (null != mTodayStepDBHelper) {
                List<TodayStepData> todayStepDataArrayList = mTodayStepDBHelper.getStepListByDate(date);
                JSONArray jsonArray = getSportStepJsonArray(todayStepDataArrayList);
                Logger.e(TAG, jsonArray.toString());
                return jsonArray.toString();
            }
            return null;
        }

        @Override
        public String getTodaySportStepArrayByStartDateAndDays(String date, int days) throws RemoteException {
            if (null != mTodayStepDBHelper) {
                List<TodayStepData> todayStepDataArrayList = mTodayStepDBHelper.getStepListByStartDateAndDays(date, days);
                JSONArray jsonArray = getSportStepJsonArray(todayStepDataArrayList);
                Logger.e(TAG, jsonArray.toString());
                return jsonArray.toString();
            }
            return null;
        }
    };

    public static String getReceiver(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_RECEIVERS);
            ActivityInfo[] activityInfos = packageInfo.receivers;
            if (null != activityInfos && activityInfos.length > 0) {
                for (int i = 0; i < activityInfos.length; i++) {
                    String receiverName = activityInfos[i].name;
                    Class superClazz = Class.forName(receiverName).getSuperclass();
                    int count = 1;
                    while (null != superClazz) {
                        if (superClazz.getName().equals("java.lang.Object")) {
                            break;
                        }
                        if (superClazz.getName().equals(BaseClickBroadcast.class.getName())) {
                            Log.e(TAG, "receiverName : " + receiverName);
                            return receiverName;
                        }
                        if (count > 20) {
                            //用来做容错，如果20个基类还不到Object直接跳出防止while死循环
                            break;
                        }
                        count++;
                        superClazz = superClazz.getSuperclass();
                        Log.e(TAG, "superClazz : " + superClazz);

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void microlog4AndroidError(String msg){
        if (null != mMicrolog4Android) {
            mMicrolog4Android.error(DateUtils.getCurrentDate("yyyy-MM-dd HH:mm:ss") + "   " + msg);
        }
    }

}
