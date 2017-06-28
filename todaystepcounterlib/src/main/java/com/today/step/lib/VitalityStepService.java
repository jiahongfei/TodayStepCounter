package com.today.step.lib;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;

import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@TargetApi(Build.VERSION_CODES.CUPCAKE)
public class VitalityStepService extends Service implements SensorEventListener {

    private static final String TAG = "VitalityStepService";

    public static final String INTENT_NAME_0_SEPARATE = "intent_name_0_separate";
    public static final String INTENT_NAME_BOOT = "intent_name_boot";

    private static final int UPLOAD_STEP_DELAYED = 1000 * 60 * 1;//1分钟上传服务器

    /**
     * 上传步数handler
     */
    private static final int UPLOAD_STEP_HANDLER = 0;
    /**
     * 计步停止保存步数到数据库handler
     */
    private static final int STEP_COUNTER_STOP_HANDLER = 1;
    /**
     * 没有计步传感器开启定时器来定时保存当天的步数
     */
    private static final int SAVE_TODAY_STEP = 2;
    /**
     * 计步停止保存步数到数据库延时时间
     */
    private static final int STEP_COUNTER_STOP_HANDLER_DURATION = 5 * 1000;

    /**
     * 计步器10步保存一次数据库
     */
    private static final int SAVE_STEP_COUNT = 10;

    //保存数据库60秒保存一次
    private static int SAVE_STEP_DB_DURATION = 5000;

    //默认为30秒进行一次存储
    private static int duration = SAVE_STEP_DB_DURATION;
    private static String CURRENTDATE = "";
    private SensorManager sensorManager;
    private StepDcretor stepDetector;
    private NotificationManager nm;
    private NotificationCompat.Builder builder;
    private Handler mHandler = new Handler(Looper.getMainLooper(), new VitalityStepHandler());
    private BroadcastReceiver mBatInfoReceiver;
    private WakeLock mWakeLock;
    private TimeCount time;
    private String DB_NAME = "stepcount";
    private boolean mSeparate = false;
    private boolean mBoot = false;
    private static int mSaveStepCount = 0;
    private static ArrayList<VitalityStepData> mVitalityStepDataList;
    private Log4j mLog4j = null;

    private UploadSportStepNetwork uploadSportStepNetwork;

    @Override
    public void onCreate() {
        Logger.e(TAG, "onCreate:" + StepDcretor.CURRENT_SETP);
        super.onCreate();

        mLog4j = new Log4j(VitalityStepService.class,
                Environment.getExternalStorageDirectory().getAbsolutePath() + "/VitalityStepServiceLibLog.txt");


        initBroadcastReceiver();
        sensorManager = (SensorManager) this
                .getSystemService(SENSOR_SERVICE);
        if (null == sensorManager) {
            Logger.e(TAG, "null == sensorManager");
        }
        new Thread(new Runnable() {
            public void run() {
                startStepDetector();
            }
        }).start();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Logger.e(TAG, "onStart:" + StepDcretor.CURRENT_SETP);
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.e(TAG, "onStartCommand:" + StepDcretor.CURRENT_SETP);
        if (null != intent) {
            mSeparate = intent.getBooleanExtra(INTENT_NAME_0_SEPARATE, false);
            mBoot = intent.getBooleanExtra(INTENT_NAME_BOOT, false);
        }

        if(mSeparate){
            mLog4j.e("0点分隔广播");
        }
        if(mBoot){
            mLog4j.e("开机自启动广播");
        }

        //初始化数据库
        CURRENTDATE = getTodayDate();
        StepDbUtils.createDb(this, DB_NAME);

        if (!isStepCounter() && StepDcretor.CURRENT_SETP < 1) {
            initTodayData();
            updateNotification(StepDcretor.CURRENT_SETP);
        }

        uploadSportStepNetwork = new UploadSportStepNetwork(getApplication());
        //开启上传服务器
        mHandler.removeMessages(UPLOAD_STEP_HANDLER);
        mHandler.sendEmptyMessageDelayed(UPLOAD_STEP_HANDLER, UPLOAD_STEP_DELAYED);

        return START_STICKY;
    }

    private String getTodayDate() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }

    private void initTodayData() {

        //没有计步器的用这个来记录当天的步数
        List<StepData> list = StepDbUtils.getQueryByWhere(StepData.class, "today", new String[]{CURRENTDATE});
        if (list.size() == 0 || list.isEmpty()) {
            StepDcretor.CURRENT_SETP = 0;
        } else if (list.size() == 1) {
            StepDcretor.CURRENT_SETP = Integer.parseInt(list.get(0).getStep());
        } else {
            Logger.v(TAG, "出错了！");
        }
    }

    private void initBroadcastReceiver() {
        final IntentFilter filter = new IntentFilter();
        // 屏幕灭屏广播
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        //日期修改
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        //关机广播
        filter.addAction(Intent.ACTION_SHUTDOWN);
        // 屏幕亮屏广播
        filter.addAction(Intent.ACTION_SCREEN_ON);
        // 屏幕解锁广播
        filter.addAction(Intent.ACTION_USER_PRESENT);
        // 当长按电源键弹出“关机”对话或者锁屏时系统会发出这个广播
        // example：有时候会用到系统对话框，权限可能很高，会覆盖在锁屏界面或者“关机”对话框之上，
        // 所以监听这个广播，当收到时就隐藏自己的对话，如点击pad右下角部分弹出的对话框
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

        mBatInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                String action = intent.getAction();

                if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    Logger.v(TAG, "screen on");
                } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    Logger.v(TAG, "screen off");
                    //改为60秒一存储
                    duration = 60000;
                    duration = SAVE_STEP_DB_DURATION;
                } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    Logger.v(TAG, "screen unlock");
//                    save();
                    saveVitalityStepData(System.currentTimeMillis() / 1000, StepDcretor.CURRENT_SETP);

                    //改为30秒一存储
                    duration = 30000;
                    duration = SAVE_STEP_DB_DURATION;
                } else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(intent.getAction())) {
                    Logger.v(TAG, " receive Intent.ACTION_CLOSE_SYSTEM_DIALOGS");
                    //保存一次
//                    save();
                    saveVitalityStepData(System.currentTimeMillis() / 1000, StepDcretor.CURRENT_SETP);

                } else if (Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
                    Logger.v(TAG, " receive ACTION_SHUTDOWN");
//                    save();
                    saveVitalityStepData(System.currentTimeMillis() / 1000, StepDcretor.CURRENT_SETP);

                } else if (Intent.ACTION_DATE_CHANGED.equals(intent.getAction())) {
                    Logger.v(TAG, " receive ACTION_TIME_CHANGED");
                    initTodayData();
                    clearStepData();
                }
            }
        };
        registerReceiver(mBatInfoReceiver, filter, "permission.ALLOW_BROADCAST", null);
    }

    private void clearStepData() {
        CURRENTDATE = "0";
    }

    private void startTimeCount() {
        time = new TimeCount(duration, 1000);
        time.start();
    }

    /**
     * 更新通知
     */
    private void updateNotification(int stepCount) {
//        builder = new NotificationCompat.Builder(this);
//        builder.setPriority(Notification.PRIORITY_MIN);
//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
//                new Intent(this, MainActivity.class), 0);
//        builder.setContentIntent(contentIntent);
//        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), com.pah.lib.R.drawable.ic_share));
//        builder.setSmallIcon(com.pah.lib.R.drawable.ic_notice_small);// 设置通知小ICON
//        builder.setTicker(getString(R.string.app_name));
//        builder.setContentTitle(getString(R.string.title_notification_bar, String.valueOf(stepCount)));
//        //设置不可清除
//        builder.setOngoing(true);
//        String km = Utils.getDistanceByStep(stepCount);
//        String calorie = Utils.getCalorieByStep(stepCount);
//        builder.setContentText(calorie + " 千卡  " + km + " 公里");
//        Notification notification = builder.build();
//
//        startForeground(0, notification);
//        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//        nm.notify(R.string.app_name, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Logger.e(TAG, "onBind:" + StepDcretor.CURRENT_SETP);
//        return messenger.getBinder();
        return mIBinder.asBinder();
    }

    private void startStepDetector() {

        getLock(this);
        //android4.4以后如果有stepcounter可以使用计步传感器
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && isStepCounter()) {
            addStepCounterListener();
        } else {
            addBasePedoListener();
        }
    }

    private boolean isStepCounter() {
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        return null == countSensor ? false : true;
    }

    private boolean isStepDetector() {
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        return null == countSensor ? false : true;
    }

    private void addStepCounterListener() {

        Logger.e(TAG, "addStepCounterListener");

        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (countSensor != null) {
            Logger.e(TAG, "countSensor");
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
        }
        if (null == countSensor) {
            Logger.v(TAG, "Count sensor not available, start self_define module.");
            addBasePedoListener();
        }
    }

    private void addBasePedoListener() {

        Logger.e(TAG, "addBasePedoListener");

        //开启定时器用来保存当天步数
        mHandler.sendEmptyMessage(SAVE_TODAY_STEP);
        //没有计步器的时候开启定时器保存数据
        stepDetector = new StepDcretor(this);
        // 获得传感器的类型，这里获得的类型是加速度传感器
        // 此方法用来注册，只有注册过才会生效，参数：SensorEventListener的实例，Sensor的实例，更新速率
        Sensor sensor = sensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(stepDetector, sensor,
                SensorManager.SENSOR_DELAY_UI);
        stepDetector
                .setOnSensorChangeListener(new StepDcretor.OnSensorChangeListener() {

                    @Override
                    public void onChange() {
                        updateNotification(StepDcretor.CURRENT_SETP);

                        vitalityStepData();

                        mHandler.removeMessages(STEP_COUNTER_STOP_HANDLER);
                        mHandler.sendEmptyMessageDelayed(STEP_COUNTER_STOP_HANDLER, STEP_COUNTER_STOP_HANDLER_DURATION);

                    }
                });
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

    private void resetSysMergeStep(float currSensorStep) {
        String stepToday = AppSharedPreferencesHelper.getInstance(getApplication()).getVitalityStepToday();
        float lastSensorStep = AppSharedPreferencesHelper.getInstance(getApplication()).getVitalityLastSensorStep();
        float stepOffset = AppSharedPreferencesHelper.getInstance(getApplication()).getVitalityStepOffset();
        if (0 == compareCurrAndToday(stepToday)) {
            //当天
            float curStep = lastSensorStep - stepOffset;
            AppSharedPreferencesHelper.getInstance(getApplication()).setVitalityStepOffset(-curStep);
            Logger.e(TAG, "当天重启手机合并步数");
            mLog4j.e("当天重启手机合并步数");

        } else {
            //系统重启隔天清零
            Logger.e(TAG, "隔天清零");
            mLog4j.e("隔天清零");
            AppSharedPreferencesHelper.getInstance(getApplication()).setVitalityStepOffset(currSensorStep);
        }
        AppSharedPreferencesHelper.getInstance(getApplication()).setVitalityStepToday(getTodayDate());
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
                String stepToday = AppSharedPreferencesHelper.getInstance(getApplication()).getVitalityStepToday();

                if (!TextUtils.isEmpty(stepToday) &&
                        (event.values[0] < AppSharedPreferencesHelper.getInstance(getApplication()).getVitalityLastSensorStep())) {
                    Logger.e(TAG, "如果当前计步器的步数小于上次计步器的步数肯定是关机了");
                    mLog4j.e("如果当前计步器的步数小于上次计步器的步数肯定是关机了");

                    resetSysMergeStep(event.values[0]);
                    //测试通过

                } else if (!TextUtils.isEmpty(stepToday) &&
                        (AppSharedPreferencesHelper.getInstance(getApplication()).getVitalityLastSystemRunningTime() > SystemClock.elapsedRealtime())) {
                    Logger.e(TAG, "上次系统运行时间如果大于当前系统运行时间有很大几率是关机了（这个只是个猜测值来提高精度的，实际上有可能当前系统运行时间超过上次运行时间）");
                    mLog4j.e("上次系统运行时间如果大于当前系统运行时间有很大几率是关机了（这个只是个猜测值来提高精度的，实际上有可能当前系统运行时间超过上次运行时间）");

                    resetSysMergeStep(event.values[0]);
                    //测试通过

                }
            }

            if (mSeparate) {
                //分隔时间，当app不在后台进程alertmanager启动app会进入
                mSeparate = false;
                AppSharedPreferencesHelper.getInstance(getApplication()).setVitalityStepToday(getTodayDate());
                AppSharedPreferencesHelper.getInstance(getApplication()).setVitalityStepOffset(event.values[0]);
                Logger.e(TAG, "mSeparate  =true");
                mLog4j.e("mSeparate  =true");

                //测试
            }

            String stepToday = AppSharedPreferencesHelper.getInstance(getApplication()).getVitalityStepToday();
            if (TextUtils.isEmpty(stepToday)) {
                //第一次用手机app的时候记录一个offset
                AppSharedPreferencesHelper.getInstance(getApplication()).setVitalityStepToday(getTodayDate());
                AppSharedPreferencesHelper.getInstance(getApplication()).setVitalityStepOffset(event.values[0]);
                //测试
            } else {
                try {
                    if (1 == compareCurrAndToday(stepToday)) {
                        //当前时间大于上次记录时间，跨越0点，0点到开启App这段时间的数据会算为前一天的数据。如果跨越多天，都会算前一天的

                        long preDay = DateUtils.getDateMillis(DateUtils.dateFormat(new DateTime(System.currentTimeMillis()).plusDays(-1).getMillis(), "yyyy-MM-dd") + " 23:59:59", "yyyy-MM-dd HH:mm:ss");
                        int step = (int) (event.values[0] - AppSharedPreferencesHelper.getInstance(getApplication()).getVitalityStepOffset());
                        Logger.e(TAG, "跨越0点计算前一天时间 ：" + DateUtils.dateFormat(preDay, "yyyy-MM-dd HH:mm:ss"));
                        Logger.e(TAG, "跨越0点计算前一天步数 ：" + step);
                        mLog4j.e("跨越0点计算前一天时间 ：" + DateUtils.dateFormat(preDay, "yyyy-MM-dd HH:mm:ss"));
                        mLog4j.e("跨越0点计算前一天步数 ：" + step);
                        saveVitalityStepData(preDay / 1000, step);

                        AppSharedPreferencesHelper.getInstance(getApplication()).setVitalityStepToday(getTodayDate());
                        AppSharedPreferencesHelper.getInstance(getApplication()).setVitalityStepOffset(event.values[0]);
                        //测试
                    }
                } catch (Exception e) {
                    Logger.e(TAG, "Exception e");
                }

            }

            float offset = AppSharedPreferencesHelper.getInstance(getApplication()).getVitalityStepOffset();
            Logger.e(TAG, "offset : " + offset + "步");

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
                AppSharedPreferencesHelper.getInstance(getApplication()).setVitalityStepToday(getTodayDate());
                AppSharedPreferencesHelper.getInstance(getApplication()).setVitalityStepOffset(event.values[0]);
            }
            StepDcretor.CURRENT_SETP = (int) step;
            updateNotification(StepDcretor.CURRENT_SETP);

            Logger.e(TAG, "当前步数 : " + StepDcretor.CURRENT_SETP + "步");
            Logger.e(TAG, "传感器步数 : " + event.values[0] + "步");
            mLog4j.e("当前步数 : " + StepDcretor.CURRENT_SETP + "步");
            mLog4j.e( "传感器步数 : " + event.values[0] + "步");

            vitalityStepData();

            AppSharedPreferencesHelper.getInstance(getApplication()).setVitalityLastSensorStep(event.values[0]);
            AppSharedPreferencesHelper.getInstance(getApplication()).setVitalityLastSystemRunningTime(SystemClock.elapsedRealtime());
            AppSharedPreferencesHelper.getInstance(getApplication()).setVitalityStepToday(getTodayDate());

            mHandler.removeMessages(STEP_COUNTER_STOP_HANDLER);
            mHandler.sendEmptyMessageDelayed(STEP_COUNTER_STOP_HANDLER, STEP_COUNTER_STOP_HANDLER_DURATION);

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void vitalityStepData() {
        if (mSaveStepCount >= SAVE_STEP_COUNT) {
            mSaveStepCount = 0;
            //走10步保存一次数据库
            Logger.e(TAG, "保存数据库 : " + StepDcretor.CURRENT_SETP + "步");
            saveVitalityStepData(System.currentTimeMillis() / 1000, StepDcretor.CURRENT_SETP);
        }
        mSaveStepCount++;
    }

    class VitalityStepHandler implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case UPLOAD_STEP_HANDLER: {
                    //开启上传服务器
                    mHandler.removeMessages(UPLOAD_STEP_HANDLER);
                    mHandler.sendEmptyMessageDelayed(UPLOAD_STEP_HANDLER, UPLOAD_STEP_DELAYED);

                    Logger.e(TAG, "上传数据服务器");

                    mVitalityStepDataList = (ArrayList) StepDbUtils.getQueryAll(VitalityStepData.class);
                    if (null == mVitalityStepDataList || 0 == mVitalityStepDataList.size()) {
                        return false;
                    }
                    String uploadStep = getUploadStep(mVitalityStepDataList);
                    if (TextUtils.isEmpty(uploadStep)) {
                        return false;
                    }
                    Logger.e(TAG, "删除数据库 ： " + mVitalityStepDataList.toString());
                    StepDbUtils.delete(mVitalityStepDataList);

//                    Logger.e(TAG, "开始上传");
//                    uploadSportStepNetwork.postSportStepNum(uploadStep, new UploadSportStepResponse() {
//                        @Override
//                        public void onSuccess(String response) {
//                            //上传步数成功删除对应的数据库
//                            if (null != mVitalityStepDataList && mVitalityStepDataList.size() > 0) {
//                                Logger.e(TAG, "删除数据库 ： " + mVitalityStepDataList.toString());
//                                StepDbUtils.delete(mVitalityStepDataList);
//                            }
//                        }
//
//                        @Override
//                        public void onFails(String error) {
//
//                        }
//                    });

                    break;
                }
                case STEP_COUNTER_STOP_HANDLER: {
                    //计步暂停保存数据库
                    Logger.e(TAG, "计步暂停保存数据库");
                    saveVitalityStepData(System.currentTimeMillis() / 1000, StepDcretor.CURRENT_SETP);

                    break;
                }
                case SAVE_TODAY_STEP: {
                    startTimeCount();
                    break;
                }
                default:
                    break;
            }
            return false;
        }
    }

    private String getUploadStep(ArrayList<VitalityStepData> vitalityStepDataList) {
        String jsonString = JSON.toJSONString(vitalityStepDataList);
        return jsonString;
    }

    class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            // 如果计时器正常结束，则开始计步
            time.cancel();
            save();
            mHandler.sendEmptyMessage(SAVE_TODAY_STEP);
        }

        @Override
        public void onTick(long millisUntilFinished) {
        }
    }

    private void saveVitalityStepData(long date, long step) {
        VitalityStepData vitalityStepData = new VitalityStepData();
        vitalityStepData.setDate(date);
        vitalityStepData.setStep(step);
        StepDbUtils.insert(vitalityStepData);
        Logger.e(TAG, "saveVitalityStepData");
    }

    private void save() {
        int tempStep = StepDcretor.CURRENT_SETP;
        List<StepData> list = StepDbUtils.getQueryByWhere(StepData.class, "today", new String[]{CURRENTDATE});
        if (list.size() == 0 || list.isEmpty()) {
            StepData data = new StepData();
            data.setToday(CURRENTDATE);
            data.setStep(tempStep + "");
            StepDbUtils.insert(data);
        } else if (list.size() == 1) {
            StepData data = list.get(0);
            data.setStep(tempStep + "");
            StepDbUtils.update(data);
        } else {
        }
    }


    @Override
    public void onDestroy() {
        Logger.e(TAG, "onDestroy:" + StepDcretor.CURRENT_SETP);
        //取消前台进程
        stopForeground(true);
        StepDbUtils.closeDb();
        if (null != mBatInfoReceiver) {
            unregisterReceiver(mBatInfoReceiver);
        }
        if (sensorManager != null) {
            sensorManager.unregisterListener(stepDetector);
            sensorManager = null;
        }
        if (stepDetector != null) {
            stepDetector = null;
        }
        Intent intent = new Intent(this, VitalityStepService.class);
        startService(intent);
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Logger.e(TAG, "onUnbind:" + StepDcretor.CURRENT_SETP);
        return super.onUnbind(intent);
    }

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
                    VitalityStepService.class.getName());
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


    private final ISportStepInterface.Stub mIBinder = new ISportStepInterface.Stub() {
        @Override
        public int getCurrTimeSportStep() throws RemoteException {
            return StepDcretor.CURRENT_SETP;
        }
    };

}
