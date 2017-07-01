package com.today.step.lib;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
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
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@TargetApi(Build.VERSION_CODES.CUPCAKE)
public class VitalityStepService extends Service {

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

    //保存数据库60秒保存一次
    private static int SAVE_STEP_DB_DURATION = 5000;

    //默认为30秒进行一次存储
    private static int duration = SAVE_STEP_DB_DURATION;
    private static String CURRENTDATE = "";
    private SensorManager sensorManager;
    private StepDcretor stepDetector;
    private StepCounter stepCounter;
    private NotificationManager nm;
    private NotificationCompat.Builder builder;
    private Handler mHandler = new Handler(Looper.getMainLooper(), new VitalityStepHandler());
    private BroadcastReceiver mBatInfoReceiver;
    private WakeLock mWakeLock;
    private TimeCount time;
    private String DB_NAME = "stepcount";
    private boolean mSeparate = false;
    private boolean mBoot = false;
    private static ArrayList<VitalityStepData> mVitalityStepDataList;
    private Log4j mLog4j = null;

    @Override
    public void onCreate() {
        Logger.e(TAG, "onCreate:" + StepDcretor.CURRENT_SETP);
        super.onCreate();

        mLog4j = new Log4j(VitalityStepService.class, Environment.getExternalStorageDirectory().getAbsolutePath() + "/VitalityStepServiceLog.txt");

        //初始化数据库
        StepDbUtils.createDb(this, DB_NAME);

        initBroadcastReceiver();

        sensorManager = (SensorManager) this
                .getSystemService(SENSOR_SERVICE);

        if (!isStepCounter() && StepDcretor.CURRENT_SETP < 1) {
            initTodayData();
            updateNotification(StepDcretor.CURRENT_SETP);
        }

        //注册三星S键康
        addSamSungHealthStepCounterListener();
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

        CURRENTDATE = getTodayDate();

        //注册传感器
        startStepDetector();


        //开启上传服务器
        mHandler.sendEmptyMessageDelayed(UPLOAD_STEP_HANDLER, UPLOAD_STEP_DELAYED);


        //TODO:测试数据Start
        if (mSeparate) {
            mLog4j.e("0点分隔广播");
        }
        if (mBoot) {
            mLog4j.e("开机自启动广播");
        }
//        if (!isStepCounter()) {
//            ToastUtil.getInstance(getApplicationContext()).makeText("当前手机没有计步传感器");
//        } else {
//            ToastUtil.getInstance(getApplicationContext()).makeText("当前手机使用计步传感器");
//        }
        //TODO:测试数据End

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

    private void stopStepDetector(){
        //android4.4以后如果有stepcounter可以使用计步传感器
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && isStepCounter()) {
            unregisterStepCounterListener();
        } else {
            unregisterAccelerometerListener();
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

    private void addSamSungHealthStepCounterListener(){
//        Logger.e(TAG, "addSamSungHealthStepCounterListener");
//        //TODO：测试三星S健康
//        SamSungHealth.getInstance().initSamSungHealth(getApplicationContext());
//        SamSungHealth.getInstance().connectService(new SamSungHealth.OnSamSungHealthConnectionListener() {
//            @Override
//            public void onConnectionFailed(HealthConnectionErrorResult error) {
//
//            }
//
//            @Override
//            public void onChangeStepCounter(int step) {
//                //如果连接上三星S健康，就在S健康上获取数据，不使用传感器
//                stopStepDetector();
//
//                StepDcretor.CURRENT_SETP = step;
//                updateNotification(StepDcretor.CURRENT_SETP);
//
//                mHandler.removeMessages(STEP_COUNTER_STOP_HANDLER);
//                mHandler.sendEmptyMessageDelayed(STEP_COUNTER_STOP_HANDLER, STEP_COUNTER_STOP_HANDLER_DURATION);
//            }
//
//            @Override
//            public void onSaveStepCounter(int step, long millisecond) {
//                saveVitalityStepData(millisecond / 1000, step);
//
//            }
//        });
    }

    private void addStepCounterListener() {
        Logger.e(TAG, "addStepCounterListener");
        if (null != stepCounter) {
            stepCounter.setSeparate(mSeparate);
            stepCounter.setBoot(mBoot);
            Logger.e(TAG, "已经注册TYPE_STEP_COUNTER");
            return;
        }
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (null == countSensor) {
            return;
        }
        stepCounter = new StepCounter(getApplicationContext(), mSeparate,mBoot,mLog4j);
        Logger.e(TAG, "countSensor");
        sensorManager.registerListener(stepCounter, countSensor, SensorManager.SENSOR_DELAY_UI);
        stepCounter.setOnStepCounterListener(new OnStepCounterListener() {
            @Override
            public void onChangeStepCounter(int step) {

                StepDcretor.CURRENT_SETP = step;
                updateNotification(StepDcretor.CURRENT_SETP);

                mHandler.removeMessages(STEP_COUNTER_STOP_HANDLER);
                mHandler.sendEmptyMessageDelayed(STEP_COUNTER_STOP_HANDLER, STEP_COUNTER_STOP_HANDLER_DURATION);
            }

            @Override
            public void onSaveStepCounter(int step, long millisecond) {
                saveVitalityStepData(millisecond / 1000, step);
            }
        });
    }

    private void unregisterStepCounterListener(){
        if(null != sensorManager && null != stepCounter){
            sensorManager.unregisterListener(stepCounter);
            stepCounter = null;
        }
    }

    private void addBasePedoListener() {
        Logger.e(TAG, "addBasePedoListener");
        if (null != stepDetector) {
            Logger.e(TAG, "已经注册TYPE_ACCELEROMETER");
            return;
        }
        //开启定时器用来保存当天步数
        mHandler.sendEmptyMessage(SAVE_TODAY_STEP);
        //没有计步器的时候开启定时器保存数据
        Sensor sensor = sensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (null == sensor) {
            return;
        }
        stepDetector = new StepDcretor(this);
        // 获得传感器的类型，这里获得的类型是加速度传感器
        // 此方法用来注册，只有注册过才会生效，参数：SensorEventListener的实例，Sensor的实例，更新速率
        sensorManager.registerListener(stepDetector, sensor,
                SensorManager.SENSOR_DELAY_UI);
        stepDetector
                .setOnStepCounterListener(new OnStepCounterListener() {
                    @Override
                    public void onChangeStepCounter(int step) {

                        updateNotification(StepDcretor.CURRENT_SETP);

                        mHandler.removeMessages(STEP_COUNTER_STOP_HANDLER);
                        mHandler.sendEmptyMessageDelayed(STEP_COUNTER_STOP_HANDLER, STEP_COUNTER_STOP_HANDLER_DURATION);
                    }

                    @Override
                    public void onSaveStepCounter(int step, long millisecond) {
                        saveVitalityStepData(millisecond / 1000, step);

                    }
                });
    }

    private void unregisterAccelerometerListener(){
        if(null != sensorManager && null != stepDetector){
            sensorManager.unregisterListener(stepDetector);
            stepDetector = null;
        }
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
                    mLog4j.e("上传数据服务器");

                    mVitalityStepDataList = (ArrayList) StepDbUtils.getQueryAll(VitalityStepData.class);
                    if (null == mVitalityStepDataList || 0 == mVitalityStepDataList.size()) {
                        return false;
                    }
                    String uploadStep = getUploadStep(mVitalityStepDataList);
                    if (TextUtils.isEmpty(uploadStep)) {
                        return false;
                    }
//                    String currentUid = getCurrentUid();
//                    String currentUserToken = getCurrentUserToken();
//
//                    if (TextUtils.isEmpty(currentUid) || TextUtils.isEmpty(currentUserToken)) {
//                        return false;
//                    }

                    Logger.e(TAG, "开始上传");
                    mLog4j.e("开始上传");

//                    serviceDetailInteractor.postSportStepNum(currentUid, currentUserToken, uploadStep,
//                            new BaseEntityResponse<HealthBasic>(HealthBasic.class) {
//                                @Override
//                                public void onSuccess(HealthBasic healthBasic) throws Exception {
//
//                                    //上传步数成功删除对应的数据库
//                                    if (null != mVitalityStepDataList && mVitalityStepDataList.size() > 0) {
//                                        Logger.e(TAG, "删除数据库 ： " + mVitalityStepDataList.toString());
//                                        mLog4j.e("删除数据库 ： " + mVitalityStepDataList.toString());
//                                        StepDbUtils.delete(mVitalityStepDataList);
//                                    }
//                                }
//
//                                @Override
//                                public boolean onFailure(int type, String arg1) {
//
//                                    return false;
//                                }
//                            });

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
//        if (!PAHApplication.getInstance().isLogin()) {
//            return "";
//        }
//        if (null != vitalityStepDataList && vitalityStepDataList.size() > 0) {
//            List<SportRecord> sportRecordList = new ArrayList<>();
//            for (VitalityStepData vitalityStepData : vitalityStepDataList) {
//                SportRecord sportRecord = new SportRecord();
//                sportRecord.setSportDate(vitalityStepData.getDate() + "");
//                sportRecord.setStepNum(String.valueOf(vitalityStepData.getStep()));
//                sportRecord.setKm(Utils.getDistanceByStep((int) vitalityStepData.getStep()));
//                sportRecord.setKaluli(Utils.getCalorieByStep((int) vitalityStepData.getStep()));
//                sportRecordList.add(sportRecord);
//            }
//            Logger.e(TAG, "size : " + sportRecordList.size() + "  " + sportRecordList.toString());
//            return JSON.toJSONString(sportRecordList);
//        }
        return "";
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
        Logger.e(TAG, "保存数据库 ： " + step);
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
//        if (sensorManager != null && null != stepDetector) {
//            sensorManager.unregisterListener(stepDetector);
//            sensorManager = null;
//        }
//        if (stepDetector != null) {
//            stepDetector = null;
//        }
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
